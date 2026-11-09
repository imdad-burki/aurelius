package com.imdadburki.aurelius.feature.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imdadburki.aurelius.core.data.LibraryScanner
import com.imdadburki.aurelius.core.data.PlaylistEntity
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.data.appendPositions
import com.imdadburki.aurelius.core.data.shouldAddFavorite
import com.imdadburki.aurelius.core.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibrarySort { TITLE, ARTIST, DURATION, RECENT }

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _sort = MutableStateFlow(LibrarySort.TITLE)
    val sort: StateFlow<LibrarySort> = _sort.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val favoriteIds = container.database.favoriteDao().favoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = container.database.playlistDao().playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentHistory = container.database.historyDao().recent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Visible tracks after sort + search filter. */
    val visibleTracks: StateFlow<List<Track>> = combine(_tracks, _sort, _query) { list, sort, q ->
        val filtered = if (q.isBlank()) list else list.filter {
            it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true)
        }
        when (sort) {
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            LibrarySort.DURATION -> filtered.sortedByDescending { it.durationMs }
            LibrarySort.RECENT -> filtered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSort(s: LibrarySort) { _sort.value = s }
    fun setQuery(q: String) { _query.value = q }

    fun scan(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val found = withContext(Dispatchers.IO) { LibraryScanner.scan(context) }
                _tracks.value = found
            } catch (e: SecurityException) {
                _error.value = "Music permission is required to scan your device."
            } catch (e: Exception) {
                _error.value = "Could not scan your library: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val dao = container.database.favoriteDao()
            if (shouldAddFavorite(favoriteIds.value, track.id)) dao.add(com.imdadburki.aurelius.core.data.FavoriteEntity(track.id))
            else dao.remove(track.id)
        }
    }

    fun createPlaylist(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            val dao = container.database.playlistDao()
            val id = dao.insertPlaylist(PlaylistEntity(name = name))
            tracks.forEachIndexed { i, t ->
                dao.insertTrack(
                    com.imdadburki.aurelius.core.data.PlaylistTrackEntity(
                        playlistId = id, mediaId = t.id, position = i,
                        title = t.title, artist = t.artist, uri = t.uri,
                        durationMs = t.durationMs, artworkUri = t.artworkUri
                    )
                )
            }
        }
    }

    fun deletePlaylist(p: PlaylistEntity) {
        viewModelScope.launch { container.database.playlistDao().deletePlaylist(p) }
    }

    /** Live track list of one playlist (for its detail screen). */
    fun playlistTracks(pid: Long) = container.database.playlistDao().tracks(pid)

    /** Appends songs to a playlist, after the existing last position. */
    fun addTracksToPlaylist(playlistId: Long, tracks: List<Track>) {
        viewModelScope.launch {
            val dao = container.database.playlistDao()
            val positions = appendPositions(dao.maxPosition(playlistId), tracks.size)
            tracks.forEachIndexed { i, t ->
                dao.insertTrack(
                    com.imdadburki.aurelius.core.data.PlaylistTrackEntity(
                        playlistId = playlistId, mediaId = t.id, position = positions[i],
                        title = t.title, artist = t.artist, uri = t.uri,
                        durationMs = t.durationMs, artworkUri = t.artworkUri
                    )
                )
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, mediaId: String) {
        viewModelScope.launch { container.database.playlistDao().removeTrack(playlistId, mediaId) }
    }

    fun recordHistory(track: Track, positionMs: Long = 0) {
        viewModelScope.launch {
            container.database.historyDao().record(
                com.imdadburki.aurelius.core.data.PlayHistoryEntity(
                    mediaId = track.id, title = track.title, artist = track.artist,
                    uri = track.uri, artworkUri = track.artworkUri, lastPositionMs = positionMs
                )
            )
        }
    }
}
