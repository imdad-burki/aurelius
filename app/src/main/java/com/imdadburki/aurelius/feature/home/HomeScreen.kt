package com.imdadburki.aurelius.feature.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.data.PlayHistoryEntity
import com.imdadburki.aurelius.core.data.PlaylistEntity
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.SectionTitle
import com.imdadburki.aurelius.core.design.TrackArtwork
import com.imdadburki.aurelius.core.design.TrackRow
import com.imdadburki.aurelius.core.network.YouTubeResult
import com.imdadburki.aurelius.feature.library.LibraryViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

fun greeting(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
fun HomeScreen(
    libraryVm: LibraryViewModel,
    trending: List<YouTubeResult>,
    trendingError: String?,
    onTrendingRetry: () -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayYouTube: (YouTubeResult) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlaylist: (PlaylistEntity) -> Unit,
    onOpenAlbum: (String) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val tracks by libraryVm.tracks.collectAsState()
    val history by libraryVm.recentHistory.collectAsState()
    val playlists by libraryVm.playlists.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = AureliusColors.Surface) {
                Text(
                    "AURELIUS",
                    style = AureliusType.WordmarkSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(24.dp)
                )
                DrawerEntry("Home", true) { scope.launch { drawerState.close() } }
                DrawerEntry("Search", false) { scope.launch { drawerState.close() }; onOpenSearch() }
                DrawerEntry("Library", false) { scope.launch { drawerState.close() }; onOpenLibrary() }
                DrawerEntry("Settings", false) { scope.launch { drawerState.close() }; onOpenSettings() }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(AureliusColors.Background),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menu", tint = AureliusColors.GoldPrimary)
                    }
                    Text(
                        "Aurelius",
                        style = AureliusType.WordmarkSmall,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, "Search", tint = AureliusColors.GoldPrimary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = AureliusColors.GoldPrimary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(greeting(), style = AureliusType.Heading, maxLines = 1)
                Text("Ready for your soundtrack?", style = AureliusType.Body, maxLines = 1)
                Spacer(Modifier.height(24.dp))
            }

            if (history.isNotEmpty()) {
                item {
                    SectionTitle("Continue Listening")
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(history) { h ->
                            ContinueCard(h) {
                                val t = tracks.firstOrNull { it.id == h.mediaId }
                                    ?: Track(h.mediaId, h.title, h.artist, "", 0, h.uri, h.artworkUri)
                                onPlayTracks(listOf(t), 0)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                SectionTitle("Trending on YouTube")
                Spacer(Modifier.height(12.dp))
            }
            if (trendingError != null) {
                item {
                    Text(
                        trendingError,
                        style = AureliusType.Body.copy(color = AureliusColors.Error)
                    )
                    Text(
                        "Retry",
                        style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
                        modifier = Modifier.clickable { onTrendingRetry() }.padding(vertical = 8.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(trending) { r -> TrendingCard(r) { onPlayYouTube(r) } }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (tracks.isNotEmpty()) {
                item {
                    SectionTitle("Recently Added", onSeeAll = onOpenLibrary)
                    Spacer(Modifier.height(12.dp))
                }
                val recent = tracks.sortedByDescending { it.addedAtSec }.take(5)
                items(recent) { t ->
                    TrackRow(t, onClick = {
                        val idx = tracks.indexOf(t)
                        onPlayTracks(tracks, idx.coerceAtLeast(0))
                    })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }

            if (playlists.isNotEmpty()) {
                item {
                    SectionTitle("Your Playlists", onSeeAll = onOpenLibrary)
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(playlists) { p -> PlaylistCard(p) { onOpenPlaylist(p) } }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (tracks.isNotEmpty()) {
                item {
                    SectionTitle("Albums", onSeeAll = onOpenLibrary)
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    val albums = tracks.groupBy { it.album }.entries.take(6)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albums.toList()) { (name, albumTracks) ->
                            AlbumCard(name, albumTracks.first()) { onOpenAlbum(name) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawerEntry(label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, style = AureliusType.Title) },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = AureliusColors.Elevated,
            unselectedContainerColor = AureliusColors.Surface,
            selectedTextColor = AureliusColors.GoldPrimary,
            unselectedTextColor = AureliusColors.TextPrimary
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ContinueCard(h: PlayHistoryEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AureliusColors.Surface)
    ) {
        Column {
            if (h.artworkUri != null) {
                AsyncImage(
                    model = h.artworkUri,
                    contentDescription = h.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.aurelius_logo),
                    error = painterResource(R.drawable.aurelius_logo),
                    modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                TrackArtwork(
                    Track(h.mediaId, h.title, h.artist, "", 0, h.uri, h.artworkUri),
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )
            }
            Text(
                h.title, style = AureliusType.Title,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TrendingCard(r: YouTubeResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(170.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AureliusColors.Surface)
    ) {
        Column {
            AsyncImage(
                model = r.thumbnailUrlLarge.ifEmpty { r.thumbnailUrl },
                contentDescription = r.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.aurelius_logo),
                error = painterResource(R.drawable.aurelius_logo),
                modifier = Modifier.fillMaxWidth().height(95.dp).clip(RoundedCornerShape(16.dp))
            )
            Text(
                r.title, style = AureliusType.Title,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                r.channel, style = AureliusType.Caption,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun PlaylistCard(p: PlaylistEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(170.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AureliusColors.Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("♪", style = AureliusType.Heading.copy(color = AureliusColors.GoldPrimary))
            Spacer(Modifier.height(8.dp))
            Text(p.name, style = AureliusType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Playlist", style = AureliusType.Caption)
        }
    }
}

@Composable
private fun AlbumCard(name: String, track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(130.dp).clickable { onClick() }
    ) {
        TrackArtwork(track, modifier = Modifier.size(130.dp), cornerDp = 16)
        Spacer(Modifier.height(8.dp))
        Text(name, style = AureliusType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, style = AureliusType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
