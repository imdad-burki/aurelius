package com.imdadburki.aurelius.feature.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldCircleButton
import com.imdadburki.aurelius.core.design.GoldSlider
import com.imdadburki.aurelius.core.design.TrackArtwork
import com.imdadburki.aurelius.core.design.formatDuration
import kotlinx.coroutines.delay

/**
 * Main player: artwork, blur background, transport controls, seek bar, favorite,
 * EQ / Lyrics / Sleep / Speed actions, and MX-style gestures:
 * left-half vertical = volume, right-half vertical = brightness (window-only,
 * temporary), horizontal = seek, tap = show/hide controls, double-tap = play/pause.
 * Every gesture shows a gold feedback pill (Volume 65% / Brightness 40% / Seek +10s).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    vm: PlayerViewModel,
    favoriteIds: List<String>,
    onToggleFavorite: (Track) -> Unit,
    onBack: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val player = vm.player
    val queue by player.queue.collectAsState()
    val index by player.currentIndex.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val position by player.positionMs.collectAsState()
    val duration by player.durationMs.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val repeat by player.repeatMode.collectAsState()
    val speed by player.speed.collectAsState()
    val track = queue.getOrNull(index)
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    var controlsVisible by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var showEq by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var pendingSeek by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(1200)
            feedback = null
        }
    }
    // Hide the feedback pill value after the gesture ends (seek commits on release).
    LaunchedEffect(pendingSeek) {
        if (pendingSeek != 0f) {
            delay(900)
            pendingSeek = 0f
        }
    }

    if (track == null) {
        Box(Modifier.fillMaxSize().background(AureliusColors.Background), Alignment.Center) {
            Text("Nothing playing yet.", style = AureliusType.Body)
        }
        return
    }

    fun setVolume(deltaFraction: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (cur + (deltaFraction * max).toInt()).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        feedback = "Volume ${(next * 100 / max)}%"
    }

    fun setBrightness(deltaFraction: Float) {
        val window = activity?.window ?: return
        val attrs = window.attributes
        val cur = if (attrs.screenBrightness < 0) 0.5f else attrs.screenBrightness
        val next = (cur + deltaFraction).coerceIn(0.05f, 1f)
        attrs.screenBrightness = next
        window.attributes = attrs
        feedback = "Brightness ${(next * 100).toInt()}%"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AureliusColors.Background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { controlsVisible = !controlsVisible },
                    onDoubleTap = { player.toggle() }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (pendingSeek != 0f) {
                            player.seekBy(pendingSeek.toLong())
                            feedback = if (pendingSeek > 0) "Seek +${(pendingSeek / 1000).toInt()}s"
                            else "Seek ${(pendingSeek / 1000).toInt()}s"
                            pendingSeek = 0f
                        }
                    }
                ) { _, dragAmount ->
                    pendingSeek += dragAmount * 30f
                }
            }
            .pointerInput(Unit) {
                // Vertical drags: left half = volume, right half = brightness.
                var lastY = 0f
                var startX = 0f
                val halfWidth = size.width / 2f
                detectDragGestures(
                    onDragStart = { offset -> lastY = offset.y; startX = offset.x },
                    onDrag = { change, dragAmount ->
                        if (kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x)) {
                            val frac = -dragAmount.y / 600f
                            if (startX < halfWidth) setVolume(frac) else setBrightness(frac)
                        }
                        lastY = change.position.y
                    }
                )
            }
    ) {
        // Blurred artwork background (ignored on API < 31; dark overlay keeps contrast).
        if (track.artworkUri != null) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(28.dp),
                alpha = 0.35f
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowDown, "Close", tint = AureliusColors.GoldPrimary, modifier = Modifier.size(28.dp))
                    }
                    Text("AURELIUS", style = AureliusType.WordmarkSmall, maxLines = 1, softWrap = false)
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.Default.MoreVert, "Queue", tint = AureliusColors.GoldPrimary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TrackArtwork(track, modifier = Modifier.size(300.dp), cornerDp = 16)
            Spacer(Modifier.height(24.dp))
            Text(track.title, style = AureliusType.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Text("${track.artist} · ${track.album}", style = AureliusType.Body, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            // Favorite is Room-backed (via LibraryViewModel): the old local
            // remember-state heart reset to empty every time you left the
            // player, so favorites "disappeared" on the way back.
            val isFavorite = favoriteIds.contains(track.id)
            IconButton(onClick = { onToggleFavorite(track) }) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "Favorite", tint = AureliusColors.GoldPrimary, modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            GoldSlider(
                value = if (duration > 0) position.toFloat() / duration else 0f,
                onValueChange = { player.seekTo((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(position), style = AureliusType.Caption.copy(fontFamily = AureliusType.Timer.fontFamily))
                Text(formatDuration(duration), style = AureliusType.Caption.copy(fontFamily = AureliusType.Timer.fontFamily))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.setShuffle(!shuffle) }) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) AureliusColors.GoldPrimary else AureliusColors.TextTertiary, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = { player.previous() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = AureliusColors.TextPrimary, modifier = Modifier.size(40.dp))
                }
                GoldCircleButton(
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    onClick = { player.toggle() },
                    sizeDp = 76,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = AureliusColors.TextPrimary, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        if (repeat == 2) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (repeat == 0) AureliusColors.TextTertiary else AureliusColors.GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayerAction("EQ") { showEq = true }
                PlayerAction("Lyrics", onOpenLyrics)
                PlayerAction("Sleep") { showSleep = true }
                PlayerAction("Speed") { showSpeed = true }
                PlayerAction("Queue", onOpenQueue)
            }
            Spacer(Modifier.height(16.dp))
            Text("◂ Swipe to Seek ▸", style = AureliusType.Caption, textAlign = TextAlign.Center)
        }

        // Gesture feedback pill (Volume 65% / Brightness 40% / Seek +10s).
        AnimatedVisibility(
            visible = feedback != null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier.background(AureliusColors.Elevated, RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(feedback ?: "", style = AureliusType.Title.copy(color = AureliusColors.GoldPrimary))
            }
        }
    }

    if (showEq) {
        ModalBottomSheet(
            onDismissRequest = { showEq = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AureliusColors.Surface
        ) { EqualizerSheetContent(vm) { showEq = false } }
    }
    if (showSleep) {
        ModalBottomSheet(
            onDismissRequest = { showSleep = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AureliusColors.Surface
        ) { SleepTimerSheetContent(vm.player) { showSleep = false } }
    }
    if (showSpeed) {
        ModalBottomSheet(
            onDismissRequest = { showSpeed = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AureliusColors.Surface
        ) { SpeedSheetContent(speed, { player.setSpeed(it) }) { showSpeed = false } }
    }
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AureliusColors.Surface
        ) { QueueSheetContent(vm) { showQueue = false } }
    }
}

@Composable
private fun PlayerAction(label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Text(label, style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary))
    }
}
