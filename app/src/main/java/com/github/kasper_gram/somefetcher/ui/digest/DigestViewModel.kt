package com.github.kasper_gram.somefetcher.ui.digest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.data.FeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DigestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SoMeFetcherApplication).repository

    val pagingItems: Flow<PagingData<FeedItem>> = repository.unreadItemsPaged
        .cachedIn(viewModelScope)

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    /** Non-zero when the last refresh had feed failures; Fragment should show a message and reset. */
    private val _failedSourceCount = MutableLiveData(0)
    val failedSourceCount: LiveData<Int> = _failedSourceCount

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val failed = repository.refreshFeeds()
                if (failed > 0) {
                    _failedSourceCount.value = failed
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun acknowledgeRefreshError() {
        _failedSourceCount.value = 0
    }
}
