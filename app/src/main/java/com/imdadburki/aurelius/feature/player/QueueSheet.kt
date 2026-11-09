package com.imdadburki.aurelius.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.TrackRow

/**
 * Queue sheet ("Up Next"): current track highlighted, upcoming tracks with
 * move up/down reordering, remove, clear. Every action updates the real session.
 */
@Composable
fun QueueSheetContent(vm: PlayerViewModel, onClose: () -> Unit) {
    val player = vm.player
    val queue by player.queue.collectAsState()
    val index by player.currentIndex.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Up Next", style = AureliusType.Heading, modifier = Modifier.weight(1f))
            TextButton(onClick = { player.clearQueue(); onClose() }) {
                Text("Clear", style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = AureliusColors.GoldPrimary)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (queue.isEmpty()) {
            Text("Queue is empty.", style = AureliusType.Body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp))
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            itemsIndexed(queue) { i, track ->
                val isCurrent = i == index
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Text(
                            "NOW PLAYING",
                            style = AureliusType.Caption.copy(color = AureliusColors.GoldPrimary),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    TrackRow(
                        track,
                        onClick = {
                            if (!isCurrent) {
                                player.playTracks(queue, i)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        isCurrent = isCurrent,
                        trailing = {
                            Row {
                                IconButton(onClick = { player.moveQueueItem(i, i - 1) }) {
                                    Icon(Icons.Default.ArrowUpward, "Move up", tint = AureliusColors.TextSecondary)
                                }
                                IconButton(onClick = { player.moveQueueItem(i, i + 1) }) {
                                    Icon(Icons.Default.ArrowDownward, "Move down", tint = AureliusColors.TextSecondary)
                                }
                                IconButton(onClick = { player.removeFromQueue(i) }) {
                                    Icon(Icons.Default.Clear, "Remove", tint = AureliusColors.TextSecondary)
                                }
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
