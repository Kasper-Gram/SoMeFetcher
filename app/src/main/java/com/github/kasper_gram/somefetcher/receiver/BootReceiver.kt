package com.github.kasper_gram.somefetcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.github.kasper_gram.somefetcher.worker.DigestScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val hour = prefs.getInt("digest_hour", 8)
        val minute = prefs.getInt("digest_minute", 0)
        DigestScheduler.schedule(context, hour, minute)
    }
}
