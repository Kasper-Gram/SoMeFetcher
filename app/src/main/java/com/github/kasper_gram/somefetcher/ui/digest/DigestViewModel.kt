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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

enum class DigestFilter { UNREAD, ALL, STARRED }

@OptIn(ExperimentalCoroutinesApi::class)
class DigestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SoMeFetcherApplication).repository

    private val _filter = MutableStateFlow(DigestFilter.UNREAD)
    val filter: StateFlow<DigestFilter> = _filter

    val pagingItems: Flow<PagingData<FeedItem>> = _filter.flatMapLatest { filter ->
        when (filter) {
            DigestFilter.UNREAD -> repository.unreadItemsPaged
            DigestFilter.ALL -> repository.allItemsPaged
            DigestFilter.STARRED -> repository.starredItemsPaged
        }
    }.cachedIn(viewModelScope)

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    /** Non-zero when the last refresh had feed failures; Fragment should show a message and reset. */
    private val _failedSourceCount = MutableLiveData(0)
    val failedSourceCount: LiveData<Int> = _failedSourceCount

    fun setFilter(filter: DigestFilter) {
        _filter.value = filter
    }

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

    fun toggleStar(item: FeedItem) {
        viewModelScope.launch {
            repository.setStarred(item.id, !item.isStarred)
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
