package com.github.kasper_gram.somefetcher.feed

import com.github.kasper_gram.somefetcher.data.FeedItem
import com.github.kasper_gram.somefetcher.data.FeedSource
import com.github.kasper_gram.somefetcher.data.ItemType
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

class FeedParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchFeed(source: FeedSource): List<FeedItem> {
        val request = Request.Builder()
            .url(source.url)
            .header("User-Agent", "SoMeFetcher/1.0")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val bytes = response.body?.bytes() ?: return emptyList()
        return parseFeed(source, bytes)
    }

    private fun parseFeed(source: FeedSource, data: ByteArray): List<FeedItem> {
        val input = SyndFeedInput()
        val feed = input.build(XmlReader(ByteArrayInputStream(data)))
        return feed.entries.mapNotNull { entry -> entryToFeedItem(entry, source.id) }
    }

    private fun entryToFeedItem(entry: SyndEntry, sourceId: Long): FeedItem? {
        val title = entry.title?.trim().orEmpty()
        val link = entry.link?.trim().orEmpty()
        if (title.isEmpty() && link.isEmpty()) return null
        val description = entry.description?.value?.trim().orEmpty()
        val publishedAt = entry.publishedDate?.time
            ?: entry.updatedDate?.time
            ?: System.currentTimeMillis()
        return FeedItem(
            sourceId = sourceId,
            title = title,
            description = description,
            link = link,
            publishedAt = publishedAt,
            type = ItemType.FEED
        )
    }
}
