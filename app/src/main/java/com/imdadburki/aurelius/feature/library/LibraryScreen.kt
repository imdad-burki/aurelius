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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.data.PlaylistEntity
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.EmptyState
import com.imdadburki.aurelius.core.design.ErrorView
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.LoadingView
import com.imdadburki.aurelius.core.design.TopBar
import com.imdadburki.aurelius.core.design.TrackRow

/**
 * Library: Songs / Albums / Artists / Folders / Playlists tabs, sorting, search,
 * playlist creation, favorites, empty state (Scan Device / Search YouTube).
 */
@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onSearchYouTube: () -> Unit,
    onScan: () -> Unit
) {
    val visible by vm.visibleTracks.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val sort by vm.sort.collectAsState()
    val query by vm.query.collectAsState()
    val favorites by vm.favoriteIds.collectAsState()
    val playlists by vm.playlists.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        TopBar(title = "Library")
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            placeholder = { Text("Search your library...", style = AureliusType.Body) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = AureliusColors.GoldPrimary) },
            singleLine = true,
            textStyle = AureliusType.Body.copy(color = AureliusColors.TextPrimary),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AureliusColors.GoldPrimary,
                unfocusedBorderColor = AureliusColors.Border,
                cursorColor = AureliusColors.GoldPrimary
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        // Scrollable tabs with compact single-line labels: a fixed 5-tab row at
        // 18sp wrapped "Playlists" into "Playlis ts" on a 360dp phone.
        ScrollableTabRow(
            selectedTabIndex = tab,
            containerColor = AureliusColors.Background,
            contentColor = AureliusColors.GoldPrimary,
            edgePadding = 16.dp,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[tab]),
                    color = AureliusColors.GoldPrimary
                )
            }
        ) {
            listOf("Songs", "Albums", "Artists", "Folders", "Playlists").forEachIndexed { i, label ->
                Tab(selected = tab == i, onClick = { tab = i }) {
                    Text(label, style = AureliusType.TabLabel.copy(
                        color = if (tab == i) AureliusColors.GoldPrimary else AureliusColors.TextSecondary
                    ), maxLines = 1, softWrap = false, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
                }
            }
        }

        if (loading) {
            LoadingView(Modifier.fillMaxSize())
            return@Column
        }
        if (error != null) {
            ErrorView(error!!, onRetry = onScan, Modifier.fillMaxSize())
            return@Column
        }
        if (tracks.isEmpty()) {
            EmptyState(
                drawableRes = R.drawable.illus_lyre,
                title = "Your library is empty.",
                subtitle = "Scan your device or search YouTube to add music.",
                primaryAction = "Scan Device",
                onPrimary = onScan,
                secondaryAction = "Search YouTube",
                onSecondary = onSearchYouTube,
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        when (tab) {
            0 -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sort by: ${sort.name.lowercase().replaceFirstChar { it.uppercase() }}", style = AureliusType.Body)
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Sort", tint = AureliusColors.GoldPrimary)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        LibrarySort.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { vm.setSort(s); sortMenu = false }
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    items(visible) { t ->
                        val idx = tracks.indexOf(t)
                        TrackRow(
                            t,
                            onClick = { onPlayTracks(tracks, idx.coerceAtLeast(0)) },
                            trailing = {
                                IconButton(onClick = { vm.toggleFavorite(t) }) {
                                    Icon(
                                        if (favorites.contains(t.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        "Favorite",
                                        tint = AureliusColors.GoldPrimary
                                    )
                                }
                            }
                        )
                    }
                }
            }
            1 -> {
                val albums = visible.groupBy { it.album }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    items(albums.entries.toList()) { (name, list) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenAlbum(name) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.imdadburki.aurelius.core.design.TrackArtwork(
                                list.first(), modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, style = AureliusType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${list.size} songs · ${list.first().artist}", style = AureliusType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            2 -> {
                val artists = visible.groupBy { it.artist }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    items(artists.entries.toList()) { (name, list) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenArtist(name) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, style = AureliusType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.size} songs", style = AureliusType.Body, maxLines = 1)
                        }
                    }
                }
            }
            3 -> {
                // Real folders: songs grouped by their actual storage folder
                // (derived from the file path at scan time).
                val folders = visible.groupBy { it.folderName.ifBlank { "Unknown folder" } }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    items(folders.entries.toList()) { (name, list) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenFolder(name) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = AureliusColors.GoldPrimary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Text(name, style = AureliusType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${list.size} songs", style = AureliusType.Body, maxLines = 1)
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    item {
                        GoldButton("Create Playlist", { showCreate = true }, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                    }
                    items(playlists) { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenPlaylist(p) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, style = AureliusType.Title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { vm.deletePlaylist(p) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = AureliusColors.Error)
                            }
                        }
                    }
                    if (playlists.isEmpty()) {
                        item { Text("No playlists yet. Create one to get started.", style = AureliusType.Body) }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Playlist", style = AureliusType.Title) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name", style = AureliusType.Body) },
                    singleLine = true,
                    textStyle = AureliusType.Body.copy(color = AureliusColors.TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AureliusColors.GoldPrimary,
                        unfocusedBorderColor = AureliusColors.Border,
                        cursorColor = AureliusColors.GoldPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        vm.createPlaylist(newName.trim(), emptyList())
                        newName = ""
                        showCreate = false
                    }
                }) { Text("Create", style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Cancel", style = AureliusType.Body) }
            },
            containerColor = AureliusColors.Surface
        )
    }
}
