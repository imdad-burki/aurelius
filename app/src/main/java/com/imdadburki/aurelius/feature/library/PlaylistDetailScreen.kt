package com.imdadburki.aurelius.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.data.PlaylistTrackEntity
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.OutlineGoldButton
import com.imdadburki.aurelius.core.design.TopBar
import com.imdadburki.aurelius.core.design.TrackRow

private fun PlaylistTrackEntity.toTrack() = Track(
    id = mediaId, title = title, artist = artist, album = "",
    durationMs = durationMs, uri = uri, artworkUri = artworkUri
)

/**
 * Playlist detail: name, song list with Play / Shuffle, remove per song, and
 * "Add songs" — a picker over the device library (songs already in the
 * playlist are hidden). Tapping a playlist anywhere in the app opens this.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    vm: LibraryViewModel,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onBack: () -> Unit
) {
    val playlists by vm.playlists.collectAsState()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val entities by vm.playlistTracks(playlistId).collectAsState(emptyList())
    val allTracks by vm.tracks.collectAsState()
    var showAddSongs by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }

    val tracks = entities.map { it.toTrack() }
    val totalMs = tracks.sumOf { it.durationMs }
    val minutes = totalMs / 60000

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        TopBar(title = "", onBack = onBack)
        if (playlist == null) {
            Text("Playlist not found.", style = AureliusType.Body, modifier = Modifier.padding(24.dp))
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(playlist.name, style = AureliusType.Heading, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${tracks.size} songs · $minutes min",
                style = AureliusType.Body, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldButton(
                    "▶  PLAY",
                    { if (tracks.isNotEmpty()) onPlayTracks(tracks, 0) },
                    Modifier.weight(1f),
                    enabled = tracks.isNotEmpty()
                )
                OutlineGoldButton("SHUFFLE", {
                    if (tracks.isNotEmpty()) onPlayTracks(tracks.shuffled(), 0)
                }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlineGoldButton("＋  ADD SONGS", {
                selected = emptySet()
                showAddSongs = true
            }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(16.dp))
        if (tracks.isEmpty()) {
            Text(
                "No songs yet. Tap Add songs to fill this playlist.",
                style = AureliusType.Body,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                items(tracks.size) { i ->
                    val t = tracks[i]
                    TrackRow(
                        t,
                        onClick = { onPlayTracks(tracks, i) },
                        leadingNumber = "%02d".format(i + 1),
                        trailing = {
                            IconButton(onClick = { vm.removeTrackFromPlaylist(playlistId, t.id) }) {
                                Icon(Icons.Default.Close, "Remove", tint = AureliusColors.Error)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSongs) {
        val inPlaylist = entities.map { it.mediaId }.toSet()
        val addable = allTracks.filter { it.id !in inPlaylist }
        AlertDialog(
            onDismissRequest = { showAddSongs = false },
            title = { Text("Add songs", style = AureliusType.Title, maxLines = 1) },
            text = {
                if (addable.isEmpty()) {
                    Text("Every song in your library is already in this playlist.", style = AureliusType.Body)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
                        items(addable) { t ->
                            val checked = selected.contains(t.id)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selected = if (checked) selected - t.id else selected + t.id
                                }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selected = if (it) selected + t.id else selected - t.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = AureliusColors.GoldPrimary)
                                )
                                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(t.title, style = AureliusType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(t.artist, style = AureliusType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = addable.filter { selected.contains(it.id) }
                        if (picked.isNotEmpty()) vm.addTracksToPlaylist(playlistId, picked)
                        showAddSongs = false
                    },
                    enabled = selected.isNotEmpty()
                ) {
                    Text(
                        "Add (${selected.size})",
                        style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongs = false }) { Text("Cancel", style = AureliusType.Body) }
            },
            containerColor = AureliusColors.Surface
        )
    }
}
