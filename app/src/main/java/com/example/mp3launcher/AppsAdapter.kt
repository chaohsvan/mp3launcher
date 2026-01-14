package com.example.mp3launcher

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppsAdapter(
    private var apps: List<AppInfo>,
    private val onItemClicked: (AppInfo?) -> Unit,
    private val onItemLongClicked: (AppInfo) -> Unit // Added for pinning
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    private val VIEW_TYPE_SEARCH = 0
    private val VIEW_TYPE_APP = 1

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView? = view.findViewById(R.id.app_icon)
        val appLabel: TextView? = view.findViewById(R.id.app_label)
        val pinnedIndicator: View? = view.findViewById(R.id.pinned_indicator)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_SEARCH else VIEW_TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_SEARCH) R.layout.item_search else R.layout.item_app
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_APP) {
            val app = apps[position - 1] // Adjust position for search icon
            holder.appIcon?.setImageDrawable(app.icon)
            holder.appLabel?.text = app.label
            holder.itemView.setOnClickListener { onItemClicked(app) }

            holder.itemView.setOnLongClickListener {
                onItemLongClicked(app)
                true // Consume the long click
            }

            holder.pinnedIndicator?.visibility = if (app.isPinned) View.VISIBLE else View.GONE

            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            holder.appIcon?.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else {
            // Search Icon
            holder.itemView.setOnClickListener { onItemClicked(null) }
            holder.itemView.setOnLongClickListener(null) // No long click for search icon
            holder.pinnedIndicator?.visibility = View.GONE
        }
    }

    override fun getItemCount() = apps.size + 1 // +1 for search

    fun getAppAt(position: Int): AppInfo? {
        if (position == 0) return null
        return apps.getOrNull(position - 1)
    }

    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
