package com.focuslock.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var enableBtn: Button
    private lateinit var empty: TextView
    private val adapter = AppAdapter(pickMode = false) { app -> confirmUnblock(app) }
    private var setupInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        enableBtn = findViewById(R.id.enableBtn)
        empty = findViewById(R.id.empty)
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.addBtn).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.changePinsBtn).setOnClickListener {
            verifyAllPins { startPinSetup(cancelable = true) }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (!Store.hasPins(this) && !setupInProgress) startPinSetup(cancelable = false)
    }

    // ---------- UI state ----------

    private fun refresh() {
        val on = isBlockerEnabled()
        status.text = if (on) "✅ Blocker is ON" else "⚠️ Blocker is OFF — turn it on in Accessibility"
        enableBtn.visibility = if (on) View.GONE else View.VISIBLE

        val blocked = Store.blocked(this).map { Apps.info(this, it) }.sortedBy { it.label.lowercase() }
        adapter.items = blocked
        empty.visibility = if (blocked.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun isBlockerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, BlockerService::class.java)
        return enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == me }
    }

    // ---------- Unblocking (needs every PIN, in order) ----------

    private fun confirmUnblock(app: AppInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Unblock ${app.label}?")
            .setMessage("You'll need to enter all ${Store.pinCount(this)} PINs in order.")
            .setPositiveButton("Continue") { _, _ ->
                verifyAllPins {
                    Store.setBlocked(this, Store.blocked(this) - app.pkg)
                    refresh()
                    toast("${app.label} unblocked")
                }
            }
            .setNegativeButton("Keep blocked", null)
            .show()
    }

    private fun verifyAllPins(onSuccess: () -> Unit) = verifyPin(0, Store.pinCount(this), onSuccess)

    private fun verifyPin(index: Int, total: Int, onSuccess: () -> Unit) {
        if (index >= total) { onSuccess(); return }
        pinDialog("Enter PIN ${index + 1} of $total", null, cancelable = true, onOk = { pin ->
            if (Store.checkPin(this, index, pin)) verifyPin(index + 1, total, onSuccess)
            else toast("Wrong PIN. Start over.")
        })
    }

    // ---------- PIN setup ----------

    private fun startPinSetup(cancelable: Boolean) {
        setupInProgress = true
        val choices = arrayOf("2 PINs", "3 PINs", "4 PINs", "5 PINs")
        val b = MaterialAlertDialogBuilder(this)
            .setTitle("How many PINs to unblock an app?")
            .setCancelable(cancelable)
            .setItems(choices) { _, which -> collectPin(which + 2, mutableListOf(), cancelable) }
        if (cancelable) {
            b.setNegativeButton("Cancel") { _, _ -> setupInProgress = false }
            b.setOnCancelListener { setupInProgress = false }
        }
        b.show()
    }

    private fun collectPin(total: Int, pins: MutableList<String>, cancelable: Boolean) {
        val n = pins.size + 1
        val stop = { setupInProgress = false }
        pinDialog("Set PIN $n of $total", "At least 4 digits. Each PIN must be different.", cancelable,
            onOk = { pin ->
                when {
                    pin.length < 4 -> { toast("PIN must be at least 4 digits"); collectPin(total, pins, cancelable) }
                    pin in pins -> { toast("Use a different PIN from the earlier ones"); collectPin(total, pins, cancelable) }
                    else -> pinDialog("Confirm PIN $n of $total", null, cancelable, onOk = { again ->
                        if (again != pin) {
                            toast("PINs didn't match, try again")
                            collectPin(total, pins, cancelable)
                        } else {
                            pins += pin
                            if (pins.size == total) {
                                Store.savePins(this, pins)
                                setupInProgress = false
                                toast("$total PINs saved. Don't forget them!")
                            } else collectPin(total, pins, cancelable)
                        }
                    }, onCancel = stop)
                }
            }, onCancel = stop)
    }

    // ---------- helpers ----------

    private fun pinDialog(
        title: String,
        message: String?,
        cancelable: Boolean,
        onOk: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
        }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val box = FrameLayout(this).apply { setPadding(pad, pad / 2, pad, 0); addView(input) }

        val b = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(box)
            .setCancelable(cancelable)
            .setPositiveButton("OK") { _, _ -> onOk(input.text.toString()) }
        if (message != null) b.setMessage(message)
        if (cancelable) {
            b.setNegativeButton("Cancel") { _, _ -> onCancel() }
            b.setOnCancelListener { onCancel() }
        }
        val dialog = b.show()
        input.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
