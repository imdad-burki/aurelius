package com.imdadburki.aurelius.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.data.displayArtist
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.TrackArtwork

/**
 * Mini player pill above the bottom nav: artwork, title/artist, gold progress
 * line on the top edge, gold circular play/pause, next. Tap expands to the
 * full player.
 */
@Composable
fun MiniPlayer(vm: PlayerViewModel, onExpand: () -> Unit) {
    val player = vm.player
    val queue by player.queue.collectAsState()
    val index by player.currentIndex.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val track = queue.getOrNull(index) ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AureliusColors.Elevated)
            .border(1.dp, AureliusColors.GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .clickable { onExpand() }
    ) {
        LinearProgressIndicator(
            progress = { if (duration > 0) position.toFloat() / duration else 0f },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = AureliusColors.GoldPrimary,
            trackColor = Color.Transparent
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtwork(track, modifier = Modifier.size(44.dp), cornerDp = 8)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = AureliusType.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(displayArtist(track.artist), style = AureliusType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(AureliusColors.GoldPrimary)
                    .clickable { player.toggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = { player.next() }) {
                Icon(Icons.Default.SkipNext, "Next", tint = AureliusColors.GoldPrimary)
            }
        }
    }
}
