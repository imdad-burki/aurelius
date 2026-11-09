package com.imdadburki.aurelius.feature.search

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.ErrorView
import com.imdadburki.aurelius.core.design.LoadingView
import com.imdadburki.aurelius.core.design.TrackRow
import com.imdadburki.aurelius.core.design.formatDuration
import com.imdadburki.aurelius.core.network.YouTubeResult

/**
 * Search: YouTube tab (real Data API v3) + Local tab (device library).
 * Recent searches are kept in memory for the session only.
 */
@Composable
fun SearchScreen(
    vm: SearchViewModel,
    localTracks: List<Track>,
    onPlayLocal: (Track) -> Unit,
    onPlayYouTube: (YouTubeResult) -> Unit
) {
    val query by vm.query.collectAsState()
    val ytResults by vm.youTubeResults.collectAsState()
    val localResults by vm.localResults.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val history by vm.history.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var searched by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { vm.setQuery(it) },
            placeholder = { Text("Search songs, artists, albums...", style = AureliusType.Body) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = AureliusColors.GoldPrimary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Default.Clear, "Clear", tint = AureliusColors.TextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                vm.search(query, localTracks); searched++
            }),
            textStyle = AureliusType.Body.copy(color = AureliusColors.TextPrimary),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AureliusColors.GoldPrimary,
                unfocusedBorderColor = AureliusColors.Border,
                cursorColor = AureliusColors.GoldPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = AureliusColors.Background,
            contentColor = AureliusColors.GoldPrimary,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[tab]),
                    color = AureliusColors.GoldPrimary
                )
            }
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }) {
                Text("YouTube", style = AureliusType.Title.copy(
                    color = if (tab == 0) AureliusColors.GoldPrimary else AureliusColors.TextSecondary
                ), maxLines = 1, softWrap = false, modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = tab == 1, onClick = { tab = 1 }) {
                Text("Local", style = AureliusType.Title.copy(
                    color = if (tab == 1) AureliusColors.GoldPrimary else AureliusColors.TextSecondary
                ), maxLines = 1, softWrap = false, modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        if (loading) {
            LoadingView(Modifier.fillMaxSize())
            return@Column
        }
        if (error != null) {
            ErrorView(error!!, onRetry = { vm.search(query, localTracks) }, Modifier.fillMaxSize())
            return@Column
        }

        if (searched == 0) {
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recent searches", style = AureliusType.Title)
                    Text(
                        "Clear",
                        style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
                        modifier = Modifier.clickable { vm.clearHistory() }
                    )
                }
                Spacer(Modifier.height(8.dp))
                history.forEach { h ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            vm.setQuery(h); vm.search(h, localTracks); searched++
                        }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, null, tint = AureliusColors.TextTertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(h, style = AureliusType.Body, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Close, null, tint = AureliusColors.TextTertiary)
                    }
                }
            } else {
                Spacer(Modifier.height(32.dp))
                Text("Search anything.", style = AureliusType.Heading)
                Text("Songs, artists, albums — from YouTube or your library.", style = AureliusType.Body)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (tab == 0) {
                if (ytResults.isEmpty()) {
                    item { Text("No YouTube results for \"$query\".", style = AureliusType.Body) }
                } else {
                    items(ytResults) { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onPlayYouTube(r) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = r.thumbnailUrl,
                                contentDescription = r.title,
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.aurelius_logo),
                                error = painterResource(R.drawable.aurelius_logo),
                                modifier = Modifier.size(64.dp, 48.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = AureliusType.Title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(r.channel, style = AureliusType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(formatDuration(r.durationMs), style = AureliusType.Body)
                        }
                    }
                    item {
                        Text(
                            "Load more",
                            style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
                            modifier = Modifier.clickable { vm.loadMore() }.padding(vertical = 12.dp)
                        )
                    }
                }
            } else {
                if (localResults.isEmpty()) {
                    item { Text("No local results for \"$query\".", style = AureliusType.Body) }
                } else {
                    items(localResults) { t ->
                        TrackRow(t, onClick = { onPlayLocal(t) })
                    }
                }
            }
        }
    }
}
