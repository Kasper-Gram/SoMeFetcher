package com.github.kasper_gram.somefetcher.ui.digest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.data.FeedItem
import kotlinx.coroutines.launch

class DigestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SoMeFetcherApplication).repository

    val items: LiveData<List<FeedItem>> = repository.unreadItems

    fun markRead(item: FeedItem) {
        viewModelScope.launch {
            repository.markItemRead(item.id)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            repository.markAllRead()
        }
    }
}
