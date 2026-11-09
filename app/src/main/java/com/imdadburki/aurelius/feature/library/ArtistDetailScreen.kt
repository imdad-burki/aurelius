package com.imdadburki.aurelius.feature.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.OutlineGoldButton
import com.imdadburki.aurelius.core.design.TopBar
import com.imdadburki.aurelius.core.design.TrackArtwork
import com.imdadburki.aurelius.core.design.TrackRow

/** Artist detail: artwork, song count, Play / Shuffle, tracklist. */
@Composable
fun ArtistDetailScreen(
    artistName: String,
    vm: LibraryViewModel,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onBack: () -> Unit
) {
    val tracks by vm.tracks.collectAsState()
    val artistTracks = tracks.filter { it.artist == artistName }
    val totalMs = artistTracks.sumOf { it.durationMs }
    val minutes = totalMs / 60000

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        TopBar(title = "", onBack = onBack)
        if (artistTracks.isEmpty()) {
            Text("Artist not found.", style = AureliusType.Body, modifier = Modifier.padding(24.dp))
            return@Column
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrackArtwork(artistTracks.first(), modifier = Modifier.size(220.dp), cornerDp = 16)
            Spacer(Modifier.height(16.dp))
            Text(artistName, style = AureliusType.Heading, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${artistTracks.size} songs · $minutes min",
                style = AureliusType.Body, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldButton("▶  PLAY", { onPlayTracks(artistTracks, 0) }, Modifier.weight(1f))
                OutlineGoldButton("SHUFFLE", {
                    val shuffled = artistTracks.shuffled()
                    onPlayTracks(shuffled, 0)
                }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            itemsIndexed(artistTracks) { i, t ->
                TrackRow(
                    t,
                    onClick = { onPlayTracks(artistTracks, i) },
                    leadingNumber = "%02d".format(i + 1)
                )
            }
        }
    }
}
