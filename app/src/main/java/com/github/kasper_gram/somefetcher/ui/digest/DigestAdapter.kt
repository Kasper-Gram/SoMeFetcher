package com.github.kasper_gram.somefetcher.ui.digest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.data.FeedItem
import com.github.kasper_gram.somefetcher.data.ItemType
import com.github.kasper_gram.somefetcher.databinding.ItemFeedBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DigestAdapter(
    private val onItemClick: (FeedItem) -> Unit,
    private val onStarClick: (FeedItem) -> Unit
) : PagingDataAdapter<FeedItem, DigestAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
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
                    R.drawable.ic_notification
                } else {
                    R.drawable.ic_rss_feed
                }
            )
            binding.iconStar.setImageResource(
                if (item.isStarred) R.drawable.ic_star else R.drawable.ic_star_border
            )
            binding.root.setOnClickListener {
                onItemClick(item)
            }
            binding.iconStar.setOnClickListener {
                onStarClick(item)
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
