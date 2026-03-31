package com.github.kasper_gram.somefetcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_items")
data class FeedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val title: String,
    val description: String,
    val link: String,
    val publishedAt: Long,
    val isRead: Boolean = false,
    val type: ItemType = ItemType.FEED
)

enum class ItemType { FEED, NOTIFICATION }
