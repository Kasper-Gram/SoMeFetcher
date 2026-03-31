package com.github.kasper_gram.somefetcher.ui.settings

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean
)

class AllowedAppsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PREF_BLOCKED_PACKAGES = "blocked_notification_packages"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(application)

    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> get() = _apps

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val appList = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val ownPackage = getApplication<Application>().packageName
                val blocked = prefs.getStringSet(PREF_BLOCKED_PACKAGES, emptySet()) ?: emptySet()
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { it.packageName != ownPackage }
                    .mapNotNull { info ->
                        try {
                            AppInfo(
                                packageName = info.packageName,
                                appName = pm.getApplicationLabel(info).toString(),
                                isAllowed = info.packageName !in blocked
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _apps.value = appList
            _isLoading.value = false
        }
    }

    fun setAppAllowed(packageName: String, allowed: Boolean) {
        val blocked = prefs.getStringSet(PREF_BLOCKED_PACKAGES, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        if (allowed) {
            blocked.remove(packageName)
        } else {
            blocked.add(packageName)
        }
        prefs.edit().putStringSet(PREF_BLOCKED_PACKAGES, blocked).apply()
        _apps.value = _apps.value?.map { app ->
            if (app.packageName == packageName) app.copy(isAllowed = allowed) else app
        }
    }
}
