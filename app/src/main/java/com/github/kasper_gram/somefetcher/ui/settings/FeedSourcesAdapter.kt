package com.github.kasper_gram.somefetcher.ui.settings

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.data.FeedSource
import com.github.kasper_gram.somefetcher.databinding.ItemFeedSourceBinding

class FeedSourcesAdapter(
    private val onToggle: (FeedSource) -> Unit,
    private val onDelete: (FeedSource) -> Unit
) : ListAdapter<FeedSource, FeedSourcesAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedSourceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFeedSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(source: FeedSource) {
            binding.textSourceName.text = source.name
            binding.textSourceUrl.text = source.url
            binding.switchEnabled.isChecked = source.isEnabled
            binding.switchEnabled.setOnCheckedChangeListener(null)
            binding.switchEnabled.setOnCheckedChangeListener { _, _ -> onToggle(source) }
            binding.buttonDelete.setOnClickListener { onDelete(source) }

            val context = binding.root.context
            val lastFetchedMs = source.lastFetched
            val isStale = lastFetchedMs == 0L ||
                System.currentTimeMillis() - lastFetchedMs > STALE_THRESHOLD_MS

            val syncLabel = if (lastFetchedMs == 0L) {
                context.getString(R.string.last_synced_never)
            } else {
                DateUtils.getRelativeTimeSpanString(
                    lastFetchedMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
            }
            binding.textLastSynced.text =
                context.getString(R.string.last_synced_format, syncLabel)

            val warningIcon = if (isStale) {
                ContextCompat.getDrawable(context, R.drawable.ic_sync_warning)?.also {
                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                }
            } else {
                null
            }
            binding.textLastSynced.setCompoundDrawablesRelative(warningIcon, null, null, null)
            binding.textLastSynced.contentDescription = if (isStale) {
                context.getString(R.string.last_synced_format, syncLabel) +
                    ". " + context.getString(R.string.sync_warning_description)
            } else {
                null
            }
        }
    }

    companion object {
        private const val STALE_THRESHOLD_MS = 24 * 60 * 60 * 1000L

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedSource>() {
            override fun areItemsTheSame(oldItem: FeedSource, newItem: FeedSource) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FeedSource, newItem: FeedSource) =
                oldItem == newItem
        }
    }
}
