package com.github.kasper_gram.somefetcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feed_sources")
data class FeedSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val isEnabled: Boolean = true,
    val lastFetched: Long = 0L
)
