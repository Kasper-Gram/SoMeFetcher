package com.github.kasper_gram.somefetcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.github.kasper_gram.somefetcher.worker.DigestScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val times = DigestScheduler.loadTimes(prefs)
        DigestScheduler.scheduleAll(context, times)
    }
}
