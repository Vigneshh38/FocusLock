package com.focuslock.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
    private val adapter = AppAdapter(
        pickMode = false,
        subtitle = { describe(it.pkg) },
        onRemove = { startUnblock(it) }
    )
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

        enableBtn.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        findViewById<Button>(R.id.addBtn).setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.changePwBtn).setOnClickListener {
            askPassword("Enter current password") { setupPassword(cancelable = true) }
        }
        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            askPassword("Password to open FocusLock's app settings") {
                Store.pauseProtection(this, PAUSE_MS)
                toast("Protection paused for 2 minutes")
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }
        findViewById<Button>(R.id.uninstallBtn).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Uninstall FocusLock?")
                .setMessage("All blocks will be removed.")
                .setPositiveButton("Continue") { _, _ ->
                    askPassword("Password to uninstall") {
                        Store.pauseProtection(this, PAUSE_MS)
                        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")))
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (!Store.hasPassword(this) && !setupInProgress) setupPassword(cancelable = false)
    }

    private fun refresh() {
        val on = isBlockerEnabled()
        status.text = if (on) "✅ Blocker is ON" else "⚠️ Blocker is OFF — turn it on in Accessibility"
        enableBtn.visibility = if (on) View.GONE else View.VISIBLE

        val blocked = Store.blockedPkgs(this).map { Apps.info(this, it) }.sortedBy { it.label.lowercase() }
        adapter.items = blocked
        empty.visibility = if (blocked.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun describe(pkg: String): String {
        val r = Store.rule(this, pkg) ?: return pkg
        return if (r.method == Store.PUSHUPS) "Unlock: ${r.amount} pushups" else "Unlock: type ${r.amount} words"
    }

    private fun isBlockerEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = ComponentName(this, BlockerService::class.java)
        return enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == me }
    }

    // ---------- unblock ----------

    private fun startUnblock(app: AppInfo) {
        val r = Store.rule(this, app.pkg) ?: return
        val cls = if (r.method == Store.PUSHUPS) PushupActivity::class.java else ParagraphActivity::class.java
        val what = if (r.method == Store.PUSHUPS) "do ${r.amount} pushups" else "type ${r.amount} words with no mistakes"
        MaterialAlertDialogBuilder(this)
            .setTitle("Unblock ${app.label}?")
            .setMessage("You'll need to $what.")
            .setPositiveButton("Start") { _, _ ->
                startActivity(Intent(this, cls).putExtra(EXTRA_PKG, app.pkg))
            }
            .setNegativeButton("Keep blocked", null)
            .show()
    }

    // ---------- password ----------

    private fun askPassword(title: String, onOk: () -> Unit) {
        passwordDialog(title, null, cancelable = true) { pw ->
            if (Store.checkPassword(this, pw)) onOk() else toast("Wrong password")
        }
    }

    private fun setupPassword(cancelable: Boolean) {
        setupInProgress = true
        val done = { setupInProgress = false }
        passwordDialog(
            "Set FocusLock password",
            "Needed to uninstall FocusLock or open its settings. At least 4 characters.",
            cancelable, onCancel = done
        ) { pw ->
            if (pw.length < 4) { toast("At least 4 characters"); setupPassword(cancelable); return@passwordDialog }
            passwordDialog("Confirm password", null, cancelable, onCancel = done) { again ->
                if (again != pw) { toast("Didn't match, try again"); setupPassword(cancelable) }
                else { Store.setPassword(this, pw); done(); toast("Password saved. Don't forget it!") }
            }
        }
    }

    private fun passwordDialog(
        title: String, message: String?, cancelable: Boolean,
        onCancel: () -> Unit = {}, onOk: (String) -> Unit
    ) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Password"
        }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val box = FrameLayout(this).apply { setPadding(pad, pad / 2, pad, 0); addView(input) }
        val b = MaterialAlertDialogBuilder(this)
            .setTitle(title).setView(box).setCancelable(cancelable)
            .setPositiveButton("OK") { _, _ -> onOk(input.text.toString()) }
        if (message != null) b.setMessage(message)
        if (cancelable) {
            b.setNegativeButton("Cancel") { _, _ -> onCancel() }
            b.setOnCancelListener { onCancel() }
        }
        val d = b.show()
        input.requestFocus()
        d.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    companion object {
        const val EXTRA_PKG = "pkg"
        private const val PAUSE_MS = 2 * 60 * 1000L
    }
}
