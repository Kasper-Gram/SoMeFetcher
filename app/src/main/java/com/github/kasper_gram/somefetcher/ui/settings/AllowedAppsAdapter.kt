package com.github.kasper_gram.somefetcher.ui.settings

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.databinding.ItemAllowedAppBinding

class AllowedAppsAdapter(
    private val onToggle: (packageName: String, allowed: Boolean) -> Unit
) : ListAdapter<AppInfo, AllowedAppsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAllowedAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAllowedAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.textAppName.text = app.appName
            binding.imageAppIcon.contentDescription =
                itemView.context.getString(R.string.app_icon_description, app.appName)
            try {
                binding.imageAppIcon.setImageDrawable(
                    itemView.context.packageManager.getApplicationIcon(app.packageName)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                binding.imageAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            binding.switchAllowed.contentDescription =
                itemView.context.getString(R.string.switch_allow_app_description, app.appName)
            binding.switchAllowed.setOnCheckedChangeListener(null)
            binding.switchAllowed.isChecked = app.isAllowed
            binding.switchAllowed.setOnCheckedChangeListener { _, isChecked ->
                onToggle(app.packageName, isChecked)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem == newItem
        }
    }
}
