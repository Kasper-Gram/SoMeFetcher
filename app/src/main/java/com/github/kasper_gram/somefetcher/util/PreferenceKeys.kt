package com.github.kasper_gram.somefetcher.util

/** Shared SharedPreferences key constants used across multiple layers of the app. */
object PreferenceKeys {
    /** Set<String> of package names whose notifications are suppressed by SoMeNotificationService. */
    const val PREF_BLOCKED_PACKAGES = "blocked_notification_packages"
}
