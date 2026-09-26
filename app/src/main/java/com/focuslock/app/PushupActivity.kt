package com.focuslock.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Counts real pushups with on-device pose detection.
 * A rep = body horizontal + elbows go below ~95° and back above ~150°.
 */
class PushupActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var countText: TextView
    private lateinit var hintText: TextView
    private lateinit var executor: ExecutorService
    private val detector by lazy {
        PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
        )
    }

    private lateinit var pkg: String
    private var target = 0
    private var count = 0
    private var armedUp = false
    private var isDown = false
    private var finished = false
    private var useFront = true

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startCamera() else {
            Toast.makeText(this, "Camera permission is needed to count pushups", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pushup)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pkg = intent.getStringExtra(MainActivity.EXTRA_PKG) ?: run { finish(); return }
        target = Store.rule(this, pkg)?.amount ?: run { finish(); return }

        previewView = findViewById(R.id.preview)
        countText = findViewById(R.id.count)
        hintText = findViewById(R.id.hint)
        executor = Executors.newSingleThreadExecutor()
        findViewById<Button>(R.id.switchBtn).setOnClickListener { useFront = !useFront; startCamera() }

        updateCount()
        hint("Place the phone on the floor, sideways, 1.5–2 m away. Your whole body should be visible from the side.")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            startCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { analyze(it) }
            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                hint("Camera error: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null || finished) { proxy.close(); return }
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { onPose(it) }
            .addOnCompleteListener { proxy.close() }
    }

    private class Side(
        val shoulder: PoseLandmark, val elbow: PoseLandmark,
        val wrist: PoseLandmark, val hip: PoseLandmark, val score: Float
    )

    private fun side(pose: Pose, s: Int, e: Int, w: Int, h: Int): Side? {
        val sh = pose.getPoseLandmark(s) ?: return null
        val el = pose.getPoseLandmark(e) ?: return null
        val wr = pose.getPoseLandmark(w) ?: return null
        val hp = pose.getPoseLandmark(h) ?: return null
        val score = minOf(sh.inFrameLikelihood, el.inFrameLikelihood, wr.inFrameLikelihood, hp.inFrameLikelihood)
        return Side(sh, el, wr, hp, score)
    }

    private fun onPose(pose: Pose) {
        if (finished) return
        val left = side(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_HIP)
        val right = side(pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_HIP)
        val s = listOfNotNull(left, right).maxByOrNull { it.score }
        if (s == null || s.score < 0.5f) {
            hint("I can't see you clearly. Show your whole body from the side.")
            return
        }

        // Body must be roughly horizontal (real pushup position, not standing)
        val dx = abs(s.hip.position.x - s.shoulder.position.x)
        val dy = abs(s.hip.position.y - s.shoulder.position.y)
        if (dy > dx) {
            hint("Get into pushup position — body horizontal, side view.")
            return
        }

        val a = angle(s.shoulder, s.elbow, s.wrist)
        when {
            !armedUp -> {
                if (a > 150) { armedUp = true; hint("Go down!") } else hint("Start with arms straight.")
            }
            !isDown && a < 95 -> { isDown = true; hint("Push up!") }
            isDown && a > 150 -> {
                isDown = false
                count++
                updateCount()
                if (count >= target) success() else hint("Good! Down again.")
            }
        }
    }

    private fun angle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val r = atan2(c.position.y - b.position.y, c.position.x - b.position.x) -
            atan2(a.position.y - b.position.y, a.position.x - b.position.x)
        var deg = abs(Math.toDegrees(r.toDouble()))
        if (deg > 180) deg = 360 - deg
        return deg
    }

    private fun updateCount() { countText.text = "$count / $target" }
    private fun hint(t: String) { hintText.text = t }

    private fun success() {
        finished = true
        Store.removeRule(this, pkg)
        Toast.makeText(this, "💪 Done! App unblocked.", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::executor.isInitialized) executor.shutdown()
        detector.close()
    }
}
