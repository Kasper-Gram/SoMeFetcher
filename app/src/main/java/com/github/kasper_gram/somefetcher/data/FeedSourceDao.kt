package com.github.kasper_gram.somefetcher.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FeedSourceDao {

    @Query("SELECT * FROM feed_sources ORDER BY name ASC")
    fun getAllSources(): LiveData<List<FeedSource>>

    @Query("SELECT * FROM feed_sources WHERE isEnabled = 1")
    suspend fun getEnabledSources(): List<FeedSource>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: FeedSource): Long

    @Update
    suspend fun update(source: FeedSource)

    @Delete
    suspend fun delete(source: FeedSource)

    @Query("UPDATE feed_sources SET lastFetched = :timestamp WHERE id = :id")
    suspend fun updateLastFetched(id: Long, timestamp: Long)
}
