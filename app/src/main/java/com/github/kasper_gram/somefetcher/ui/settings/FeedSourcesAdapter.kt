package com.github.kasper_gram.somefetcher.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedSource>() {
            override fun areItemsTheSame(oldItem: FeedSource, newItem: FeedSource) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FeedSource, newItem: FeedSource) =
                oldItem == newItem
        }
    }
}
