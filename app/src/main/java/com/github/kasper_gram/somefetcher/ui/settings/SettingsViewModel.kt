package com.github.kasper_gram.somefetcher.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.github.kasper_gram.somefetcher.SoMeFetcherApplication
import com.github.kasper_gram.somefetcher.data.FeedSource
import com.github.kasper_gram.somefetcher.feed.OPMLManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class AddSourceError { INVALID_URL, UNREACHABLE, NOT_A_FEED }

sealed class AddSourceState {
    data object Idle : AddSourceState()
    data object Validating : AddSourceState()
    data object Success : AddSourceState()
    data class Invalid(val reason: AddSourceError) : AddSourceState()
}

sealed class OPMLImportState {
    data object Idle : OPMLImportState()
    data object Importing : OPMLImportState()
    data class Success(val imported: Int, val skipped: Int) : OPMLImportState()
    data class Error(val message: String) : OPMLImportState()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SoMeFetcherApplication).repository

    val sources: LiveData<List<FeedSource>> = repository.allSources

    private val _addSourceState = MutableStateFlow<AddSourceState>(AddSourceState.Idle)
    val addSourceState: StateFlow<AddSourceState> = _addSourceState.asStateFlow()

    private val _opmlImportState = MutableStateFlow<OPMLImportState>(OPMLImportState.Idle)
    val opmlImportState: StateFlow<OPMLImportState> = _opmlImportState.asStateFlow()

    fun addSource(name: String, url: String) {
        viewModelScope.launch {
            _addSourceState.value = AddSourceState.Validating
            try {
                repository.validateFeedUrl(url)
                repository.addFeedSource(FeedSource(name = name, url = url))
                _addSourceState.value = AddSourceState.Success
            } catch (e: CancellationException) {
                _addSourceState.value = AddSourceState.Idle
                throw e
            } catch (e: IllegalArgumentException) {
                _addSourceState.value = AddSourceState.Invalid(AddSourceError.INVALID_URL)
            } catch (e: java.net.UnknownHostException) {
                _addSourceState.value = AddSourceState.Invalid(AddSourceError.UNREACHABLE)
            } catch (e: java.net.SocketTimeoutException) {
                _addSourceState.value = AddSourceState.Invalid(AddSourceError.UNREACHABLE)
            } catch (e: com.rometools.rome.io.FeedException) {
                _addSourceState.value = AddSourceState.Invalid(AddSourceError.NOT_A_FEED)
            } catch (_: Exception) {
                _addSourceState.value = AddSourceState.Invalid(AddSourceError.NOT_A_FEED)
            }
        }
    }

    fun resetAddSourceState() {
        _addSourceState.value = AddSourceState.Idle
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

    /**
     * Parses the OPML file from [inputStream] and bulk-inserts new sources, skipping duplicates.
     * Updates [opmlImportState] with the result.
     */
    fun importOpml(inputStream: InputStream) {
        viewModelScope.launch {
            _opmlImportState.value = OPMLImportState.Importing
            try {
                val entries = withContext(Dispatchers.IO) {
                    OPMLManager.parseOpml(inputStream)
                }
                val (imported, skipped) = repository.importOpmlSources(entries)
                _opmlImportState.value = OPMLImportState.Success(imported, skipped)
            } catch (e: CancellationException) {
                _opmlImportState.value = OPMLImportState.Idle
                throw e
            } catch (e: Exception) {
                _opmlImportState.value = OPMLImportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetOpmlImportState() {
        _opmlImportState.value = OPMLImportState.Idle
    }

    /**
     * Generates an OPML 2.0 XML string from all feed sources.
     * Returns `null` if there are no sources to export.
     */
    suspend fun buildExportOpml(): String? = withContext(Dispatchers.IO) {
        val sources = repository.getAllSourcesList()
        if (sources.isEmpty()) null
        else OPMLManager.exportOpml(sources)
    }
}

