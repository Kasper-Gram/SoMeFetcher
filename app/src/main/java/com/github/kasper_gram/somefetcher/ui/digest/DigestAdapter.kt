package com.github.kasper_gram.somefetcher.ui.digest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.kasper_gram.somefetcher.data.FeedItem
import com.github.kasper_gram.somefetcher.data.ItemType
import com.github.kasper_gram.somefetcher.databinding.ItemFeedBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DigestAdapter(
    private val onItemRead: (FeedItem) -> Unit
) : ListAdapter<FeedItem, DigestAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        fun bind(item: FeedItem) {
            binding.textTitle.text = item.title
            binding.textDescription.text = item.description.ifEmpty { item.link }
            binding.textDate.text = dateFormat.format(Date(item.publishedAt))
            binding.iconType.setImageResource(
                if (item.type == ItemType.NOTIFICATION) {
                    android.R.drawable.ic_dialog_info
                } else {
                    android.R.drawable.ic_menu_rss_feed
                }
            )
            binding.root.setOnClickListener {
                onItemRead(item)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedItem>() {
            override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem) =
                oldItem == newItem
        }
    }
}
