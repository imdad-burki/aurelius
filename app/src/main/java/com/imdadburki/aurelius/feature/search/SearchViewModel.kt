package com.imdadburki.aurelius.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.di.AppContainer
import com.imdadburki.aurelius.core.network.YouTubeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val container: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _youTubeResults = MutableStateFlow<List<YouTubeResult>>(emptyList())
    val youTubeResults: StateFlow<List<YouTubeResult>> = _youTubeResults.asStateFlow()
    private val _localResults = MutableStateFlow<List<Track>>(emptyList())
    val localResults: StateFlow<List<Track>> = _localResults.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()
    private var nextPageToken: String? = null
    private var lastQuery = ""

    fun setQuery(q: String) { _query.value = q }

    fun search(query: String, localTracks: List<Track>) {
        val q = query.trim()
        if (q.isEmpty()) return
        lastQuery = q
        nextPageToken = null
        _localResults.value = localTracks.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
        if (!_history.value.contains(q)) _history.value = listOf(q) + _history.value.take(9)
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val (results, token) = container.youTubeApi.search(q)
                _youTubeResults.value = results
                nextPageToken = token
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
                _youTubeResults.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadMore() {
        val token = nextPageToken ?: return
        viewModelScope.launch {
            try {
                val (results, newToken) = container.youTubeApi.search(lastQuery, token)
                _youTubeResults.value = _youTubeResults.value + results
                nextPageToken = newToken
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load more results"
            }
        }
    }

    fun clearHistory() { _history.value = emptyList() }
    fun clearError() { _error.value = null }
}
