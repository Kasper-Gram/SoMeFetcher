package com.github.kasper_gram.somefetcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.data.FeedItem
import com.github.kasper_gram.somefetcher.data.ItemType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoMeNotificationService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val packageName = sbn.packageName
        if (shouldIgnorePackage(packageName)) return

        val item = FeedItem(
            sourceId = -1L,
            title = title,
            description = "[$packageName] $text",
            link = "",
            publishedAt = sbn.postTime,
            type = ItemType.NOTIFICATION
        )

        val app = application as? SoMeFetcherApplication ?: return
        scope.launch {
            app.repository.insertItem(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun shouldIgnorePackage(packageName: String): Boolean {
        return packageName == this.packageName
    }
}
