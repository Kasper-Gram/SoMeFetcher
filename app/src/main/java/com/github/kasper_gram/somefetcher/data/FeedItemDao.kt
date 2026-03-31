package com.github.kasper_gram.somefetcher.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FeedItemDao {

    @Query("SELECT * FROM feed_items ORDER BY publishedAt DESC")
    fun getAllItems(): PagingSource<Int, FeedItem>

    @Query("SELECT * FROM feed_items WHERE isRead = 0 ORDER BY publishedAt DESC")
    fun getUnreadItems(): PagingSource<Int, FeedItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<FeedItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: FeedItem)

    @Query("UPDATE feed_items SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE feed_items SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM feed_items WHERE publishedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
