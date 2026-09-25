package com.focuslock.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Adding apps to the block list is free. Removing them needs all PINs. */
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

        val alreadyBlocked = Store.blocked(this)
        Thread {
            val apps = Apps.launchable(this).filter { it.pkg !in alreadyBlocked }
            runOnUiThread {
                progress.visibility = View.GONE
                adapter.items = apps
            }
        }.start()

        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            if (adapter.selected.isNotEmpty()) {
                Store.setBlocked(this, Store.blocked(this) + adapter.selected)
                Toast.makeText(this, "Blocked ${adapter.selected.size} app(s)", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
