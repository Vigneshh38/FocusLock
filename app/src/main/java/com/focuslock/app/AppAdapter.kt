package com.focuslock.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** pickMode = true: checkboxes (add apps). false: "Unblock" buttons (blocked list). */
class AppAdapter(
    private val pickMode: Boolean,
    private val onRemove: (AppInfo) -> Unit = {}
) : RecyclerView.Adapter<AppAdapter.VH>() {

    var items: List<AppInfo> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }

    val selected = mutableSetOf<String>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
        val pkg: TextView = v.findViewById(R.id.pkg)
        val check: CheckBox = v.findViewById(R.id.check)
        val remove: Button = v.findViewById(R.id.remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val app = items[position]
        h.icon.setImageDrawable(app.icon)
        h.label.text = app.label
        h.pkg.text = app.pkg

        if (pickMode) {
            h.remove.visibility = View.GONE
            h.check.visibility = View.VISIBLE
            h.check.isChecked = app.pkg in selected
            h.itemView.setOnClickListener {
                if (!selected.add(app.pkg)) selected.remove(app.pkg)
                h.check.isChecked = app.pkg in selected
            }
        } else {
            h.check.visibility = View.GONE
            h.remove.visibility = View.VISIBLE
            h.remove.setOnClickListener { onRemove(app) }
            h.itemView.setOnClickListener(null)
        }
    }
}
