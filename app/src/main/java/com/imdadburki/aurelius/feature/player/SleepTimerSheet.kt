package com.imdadburki.aurelius.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.OutlineGoldButton
import com.imdadburki.aurelius.core.design.formatDuration
import com.imdadburki.aurelius.core.playback.PlayerManager

/**
 * Sleep timer sheet: 15/30/45/60 min, End of Track, Custom; live countdown and cancel.
 * The timer lives in PlayerManager (app-scoped), so rotation/recreation never restarts it.
 */
@Composable
fun SleepTimerSheetContent(player: PlayerManager, onClose: () -> Unit) {
    val remaining by player.sleepRemainingMs.collectAsState()
    var selected by remember { mutableStateOf(30) } // minutes; -1 = end of track
    var customMinutes by remember { mutableStateOf(45) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sleep Timer", style = AureliusType.Heading, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        Spacer(Modifier.height(8.dp))
        if (remaining != null) {
            if (remaining == -1L) {
                Text("Ends at the end of this track", style = AureliusType.Body)
            } else {
                Text(formatDuration(remaining!!), style = AureliusType.Timer)
                Text("remaining", style = AureliusType.Body)
            }
            Spacer(Modifier.height(16.dp))
            OutlineGoldButton("Cancel Timer", { player.cancelSleepTimer() }, Modifier.fillMaxWidth())
        } else {
            Text("Music will fade out and pause when the timer ends.", style = AureliusType.Body)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                items(listOf(15, 30, 45, 60)) { mins ->
                    FilterChip(
                        selected = selected == mins,
                        onClick = { selected = mins },
                        label = { Text("$mins min", style = AureliusType.Body) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AureliusColors.GoldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = AureliusColors.Elevated,
                            labelColor = AureliusColors.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selected == -1,
                        onClick = { selected = -1 },
                        label = { Text("End of Track", style = AureliusType.Body) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AureliusColors.GoldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = AureliusColors.Elevated,
                            labelColor = AureliusColors.TextSecondary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selected == -2,
                        onClick = { selected = -2 },
                        label = { Text("Custom", style = AureliusType.Body) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AureliusColors.GoldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = AureliusColors.Elevated,
                            labelColor = AureliusColors.TextSecondary
                        )
                    )
                }
            }
            if (selected == -2) {
                Spacer(Modifier.height(8.dp))
                Text("Custom: $customMinutes minutes", style = AureliusType.Body)
                Slider(
                    value = customMinutes.toFloat(),
                    onValueChange = { customMinutes = it.toInt() },
                    valueRange = 5f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = AureliusColors.GoldPrimary,
                        activeTrackColor = AureliusColors.GoldPrimary,
                        inactiveTrackColor = AureliusColors.Border
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            GoldButton("Start Timer", {
                when (selected) {
                    -1 -> player.startSleepTimerEndOfTrack()
                    -2 -> player.startSleepTimer(customMinutes * 60_000L)
                    else -> player.startSleepTimer(selected * 60_000L)
                }
                onClose()
            }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Playback speed sheet: chips + fine slider (0.25x–3x). */
@Composable
fun SpeedSheetContent(current: Float, onSpeed: (Float) -> Unit, onClose: () -> Unit) {
    var speed by remember { mutableStateOf(current) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text("Playback Speed", style = AureliusType.Heading)
        Spacer(Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(0.5f, 1f, 1.5f, 2f, 2.5f, 3f)) { s ->
                FilterChip(
                    selected = speed == s,
                    onClick = { speed = s; onSpeed(s) },
                    label = { Text("${s}x", style = AureliusType.Body) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AureliusColors.GoldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = AureliusColors.Elevated,
                        labelColor = AureliusColors.TextSecondary
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Custom: ${"%.2f".format(speed)}x", style = AureliusType.Body)
        Slider(
            value = speed,
            onValueChange = { speed = it; onSpeed(it) },
            valueRange = 0.25f..3f,
            colors = SliderDefaults.colors(
                thumbColor = AureliusColors.GoldPrimary,
                activeTrackColor = AureliusColors.GoldPrimary,
                inactiveTrackColor = AureliusColors.Border
            )
        )
        Spacer(Modifier.height(16.dp))
        GoldButton("Done", onClose, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
