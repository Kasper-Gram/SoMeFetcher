package com.github.kasper_gram.somefetcher.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feed_items",
    indices = [
        Index("isRead"),
        Index("publishedAt"),
        Index("sourceId")
    ]
)
data class FeedItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val title: String,
    val description: String,
    val link: String,
    val publishedAt: Long,
    val isRead: Boolean = false,
    val type: ItemType = ItemType.FEED,
    val isStarred: Boolean = false
)

enum class ItemType { FEED, NOTIFICATION }
