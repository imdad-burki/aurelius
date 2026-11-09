package com.imdadburki.aurelius.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.ErrorView
import com.imdadburki.aurelius.core.design.LoadingView
import com.imdadburki.aurelius.core.design.TopBar
import com.imdadburki.aurelius.core.design.formatDuration
import kotlinx.coroutines.delay

/**
 * Lyrics: current line highlighted in gold, auto-scroll follows playback.
 * Manual scrolling pauses auto-scroll for 5 seconds.
 */
@Composable
fun LyricsScreen(vm: PlayerViewModel, onBack: () -> Unit) {
    val lyrics by vm.lyrics.collectAsState()
    val state by vm.lyricsState.collectAsState()
    val player = vm.player
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val queue by player.queue.collectAsState()
    val index by player.currentIndex.collectAsState()
    val track = queue.getOrNull(index)
    val listState = rememberLazyListState()
    var autoScroll by remember { mutableStateOf(true) }
    var lastManualScroll by remember { mutableStateOf(0L) }

    val currentLine = remember(position, lyrics) {
        if (lyrics.isNullOrEmpty()) -1
        else lyrics!!.indexOfLast { it.timeMs >= 0 && it.timeMs <= position }.coerceAtLeast(0)
    }

    LaunchedEffect(currentLine, autoScroll) {
        if (autoScroll && currentLine >= 0 && !lyrics.isNullOrEmpty()) {
            listState.animateScrollToItem((currentLine - 2).coerceAtLeast(0))
        }
    }
    // Resume auto-scroll 5s after the user stops scrolling manually.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            autoScroll = false
            lastManualScroll = System.currentTimeMillis()
        } else {
            delay(5000)
            if (System.currentTimeMillis() - lastManualScroll >= 5000) autoScroll = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        TopBar(title = "LYRICS", onBack = onBack)
        when (state) {
            "loading" -> LoadingView(Modifier.fillMaxSize())
            "missing" -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No lyrics found for this song.", style = AureliusType.Body, textAlign = TextAlign.Center)
            }
            "error" -> ErrorView("Could not load lyrics.", onRetry = { track?.let { vm.fetchLyrics(it) } }, Modifier.fillMaxSize())
            else -> {
                val lines = lyrics ?: emptyList()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    itemsIndexed(lines) { i, line ->
                        Text(
                            line.text,
                            style = if (i == currentLine) AureliusType.Heading.copy(color = AureliusColors.GoldPrimary)
                            else AureliusType.Body.copy(color = AureliusColors.TextSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                        )
                    }
                }
                if (track != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${formatDuration(position)}  /  ${formatDuration(duration)}",
                        style = AureliusType.Caption,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        track.title,
                        style = AureliusType.Title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        track.artist,
                        style = AureliusType.Body,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
