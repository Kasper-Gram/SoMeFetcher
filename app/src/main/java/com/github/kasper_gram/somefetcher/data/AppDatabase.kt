package com.github.kasper_gram.somefetcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FeedItem::class, FeedSource::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun feedItemDao(): FeedItemDao
    abstract fun feedSourceDao(): FeedSourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from v1 to v2: adds the isStarred column and creates indexes on
         * feed_items(isRead), feed_items(publishedAt), and feed_items(sourceId).
         *
         * Convention: always add a new Migration object here and pass it to addMigrations().
         * Never use fallbackToDestructiveMigration() — doing so would silently wipe user data.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feed_items ADD COLUMN isStarred INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_items_isRead ON feed_items(isRead)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_items_publishedAt ON feed_items(publishedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_items_sourceId ON feed_items(sourceId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "somefetcher_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
