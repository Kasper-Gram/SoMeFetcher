package com.github.kasper_gram.somefetcher.repository

import androidx.lifecycle.LiveData
import com.github.kasper_gram.somefetcher.data.FeedItem
import com.github.kasper_gram.somefetcher.data.FeedItemDao
import com.github.kasper_gram.somefetcher.data.FeedSource
import com.github.kasper_gram.somefetcher.data.FeedSourceDao
import com.github.kasper_gram.somefetcher.feed.FeedParser
import java.util.concurrent.TimeUnit

class DigestRepository(
    private val feedItemDao: FeedItemDao,
    private val feedSourceDao: FeedSourceDao,
    private val feedParser: FeedParser = FeedParser()
) {

    val allItems: LiveData<List<FeedItem>> = feedItemDao.getAllItems()
    val unreadItems: LiveData<List<FeedItem>> = feedItemDao.getUnreadItems()
    val allSources: LiveData<List<FeedSource>> = feedSourceDao.getAllSources()

    suspend fun addFeedSource(source: FeedSource): Long = feedSourceDao.insert(source)

    suspend fun updateFeedSource(source: FeedSource) = feedSourceDao.update(source)

    suspend fun deleteFeedSource(source: FeedSource) = feedSourceDao.delete(source)

    suspend fun insertItems(items: List<FeedItem>) = feedItemDao.insertAll(items)

    suspend fun insertItem(item: FeedItem) = feedItemDao.insert(item)

    suspend fun markItemRead(id: Long) = feedItemDao.markAsRead(id)

    suspend fun markAllRead() = feedItemDao.markAllAsRead()

    suspend fun getEnabledSources(): List<FeedSource> = feedSourceDao.getEnabledSources()

    suspend fun updateSourceLastFetched(id: Long, timestamp: Long) =
        feedSourceDao.updateLastFetched(id, timestamp)

    suspend fun pruneOldItems() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        feedItemDao.deleteOlderThan(cutoff)
    }

    suspend fun refreshFeeds(): Int {
        val sources = feedSourceDao.getEnabledSources()
        var failedCount = 0
        for (source in sources) {
            try {
                val items = feedParser.fetchFeed(source)
                feedItemDao.insertAll(items)
                feedSourceDao.updateLastFetched(source.id, System.currentTimeMillis())
            } catch (_: Exception) {
                failedCount++
            }
        }
        return failedCount
    }
}
