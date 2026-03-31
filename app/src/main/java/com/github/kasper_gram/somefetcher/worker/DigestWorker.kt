package com.github.kasper_gram.somefetcher.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.kasper_gram.somefetcher.R
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.feed.FeedParser
import com.github.kasper_gram.somefetcher.ui.MainActivity

class DigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SoMeFetcherApplication ?: return Result.failure()
        val repository = app.repository
        val parser = FeedParser()

        // Fetch all enabled feeds
        val sources = repository.getEnabledSources()
        var newItemCount = 0
        for (source in sources) {
            try {
                val items = parser.fetchFeed(source)
                repository.insertItems(items)
                repository.updateSourceLastFetched(source.id, System.currentTimeMillis())
                newItemCount += items.size
            } catch (_: Exception) {
                // Continue with other sources on failure
            }
        }

        // Prune old items
        repository.pruneOldItems()

        // Post digest notification
        if (newItemCount > 0) {
            postDigestNotification(newItemCount)
        }

        return Result.success()
    }

    private fun postDigestNotification(count: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.digest_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.digest_channel_description)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.digest_notification_title))
            .setContentText(
                applicationContext.resources.getQuantityString(
                    R.plurals.digest_notification_text, count, count
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "somefetcher_digest"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "SoMeDigestWork"
    }
}
