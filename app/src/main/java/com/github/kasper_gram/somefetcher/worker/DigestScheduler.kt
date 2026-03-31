package com.github.kasper_gram.somefetcher.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object DigestScheduler {

    const val MAX_DIGEST_TIMES = 3
    const val PREF_KEY_DIGEST_TIMES = "digest_times"

    private const val WORK_NAME_PREFIX = "SoMeDigestWork"
    // Legacy single-slot work name kept so it can be cancelled on upgrade
    private const val WORK_NAME_LEGACY = "SoMeDigestWork"

    /** Schedule one PeriodicWorkRequest per entry in [times], cancelling any previous slots. */
    fun scheduleAll(context: Context, times: List<Pair<Int, Int>>) {
        val wm = WorkManager.getInstance(context)
        // Cancel legacy single work and all current slots
        wm.cancelUniqueWork(WORK_NAME_LEGACY)
        for (i in 0 until MAX_DIGEST_TIMES) {
            wm.cancelUniqueWork(slotName(i))
        }
        times.take(MAX_DIGEST_TIMES).forEachIndexed { index, (hour, minute) ->
            val delay = calculateInitialDelay(hour, minute)
            val request = PeriodicWorkRequestBuilder<DigestWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork(
                slotName(index),
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    /** Convenience overload for a single delivery time. */
    fun schedule(context: Context, hourOfDay: Int, minute: Int) {
        scheduleAll(context, listOf(Pair(hourOfDay, minute)))
    }

    /** Cancel all scheduled digest work. */
    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_NAME_LEGACY)
        for (i in 0 until MAX_DIGEST_TIMES) {
            wm.cancelUniqueWork(slotName(i))
        }
    }

    fun cancel(context: Context) = cancelAll(context)

    /**
     * Load the list of delivery times from [prefs].
     * Automatically migrates legacy `digest_hour` / `digest_minute` keys on first run.
     */
    fun loadTimes(prefs: SharedPreferences): List<Pair<Int, Int>> {
        val timesSet = prefs.getStringSet(PREF_KEY_DIGEST_TIMES, null)
        if (timesSet != null) {
            return timesSet.mapNotNull { parseTime(it) }
                .sortedWith(compareBy({ it.first }, { it.second }))
        }
        // One-time migration from legacy single-time prefs
        val hour = prefs.getInt("digest_hour", 8)
        val minute = prefs.getInt("digest_minute", 0)
        val times = listOf(Pair(hour, minute))
        saveTimes(prefs, times)
        return times
    }

    /** Persist [times] to [prefs]. */
    fun saveTimes(prefs: SharedPreferences, times: List<Pair<Int, Int>>) {
        val set = times.map { "${it.first},${it.second}" }.toSet()
        prefs.edit().putStringSet(PREF_KEY_DIGEST_TIMES, set).apply()
    }

    fun calculateInitialDelay(hourOfDay: Int, minute: Int): Long {
        require(hourOfDay in 0..23) { "hourOfDay must be 0–23, was $hourOfDay" }
        require(minute in 0..59) { "minute must be 0–59, was $minute" }
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun slotName(index: Int) = "${WORK_NAME_PREFIX}_$index"

    private fun parseTime(s: String): Pair<Int, Int>? {
        return try {
            val parts = s.split(",")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            null
        }
    }
}
