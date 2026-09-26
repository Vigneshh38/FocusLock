package com.focuslock.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Pick apps -> choose unlock method (pushups / paragraph) -> choose amount. */
class AppPickerActivity : AppCompatActivity() {

    private val adapter = AppAdapter(pickMode = true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        title = "Choose apps to block"

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        val progress = findViewById<ProgressBar>(R.id.progress)

        val alreadyBlocked = Store.blockedPkgs(this)
        Thread {
            val apps = Apps.launchable(this).filter { it.pkg !in alreadyBlocked }
            runOnUiThread {
                progress.visibility = View.GONE
                adapter.items = apps
            }
        }.start()

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            if (adapter.selected.isEmpty()) finish() else chooseMethod(adapter.selected.toSet())
        }
    }

    private fun chooseMethod(pkgs: Set<String>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("To unblock later, you must…")
            .setItems(arrayOf("💪  Do pushups (camera counts them)", "⌨️  Type a paragraph with zero mistakes")) { _, which ->
                if (which == 0) chooseAmount(pkgs, Store.PUSHUPS, intArrayOf(10, 20, 30, 50), "pushups", "How many pushups?")
                else chooseAmount(pkgs, Store.PARAGRAPH, intArrayOf(50, 100, 150, 200), "words", "How long a paragraph?")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseAmount(pkgs: Set<String>, method: String, options: IntArray, unit: String, title: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setItems(options.map { "$it $unit" }.toTypedArray()) { _, which ->
                Store.addRules(this, pkgs, method, options[which])
                Toast.makeText(this, "Blocked ${pkgs.size} app(s)", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Back") { _, _ -> chooseMethod(pkgs) }
            .show()
    }
}
