package com.github.kasper_gram.somefetcher.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.data.FeedSource
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SoMeFetcherApplication).repository

    val sources: LiveData<List<FeedSource>> = repository.allSources

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            repository.addFeedSource(FeedSource(name = name, url = url))
        }
    }

    fun toggleSource(source: FeedSource) {
        viewModelScope.launch {
            repository.updateFeedSource(source.copy(isEnabled = !source.isEnabled))
        }
    }

    fun deleteSource(source: FeedSource) {
        viewModelScope.launch {
            repository.deleteFeedSource(source)
        }
    }
}
