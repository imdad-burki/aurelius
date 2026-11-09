package com.imdadburki.aurelius.core.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.imdadburki.aurelius.core.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Central playback engine: one ExoPlayer instance for the whole app.
 * Queue, shuffle/repeat, speed, A-B loop, sleep timer and audio effects live here.
 * Audio effects use the platform android.media.audiofx stack attached to the player's
 * audio session id — the standard supported route on Android.
 */
class PlayerManager(private val context: Context) {
    val player: ExoPlayer = createPlayer()

    @UnstableApi
    private fun createPlayer(): ExoPlayer =
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var abJob: Job? = null

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()
    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    /** 0 = off, 1 = all, 2 = one */
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val currentTrack: Track?
        get() = _queue.value.getOrNull(_currentIndex.value)

    // Audio effects (created lazily once the audio session exists)
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionPolling() else positionJob?.cancel()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentIndex.value = player.currentMediaItemIndex
                _durationMs.value = player.duration.coerceAtLeast(0)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _error.value = error.message ?: "Playback error"
                // One bad file must not kill the queue: skip to the next track
                // and keep playing instead of stalling silently.
                if (player.hasNextMediaItem()) {
                    try {
                        player.seekToNextMediaItem()
                        player.prepare()
                        player.play()
                    } catch (e: Exception) {
                        // Recovery failed; the error above is shown.
                    }
                }
            }
        })
        startPositionPolling()
    }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        ensureServiceRunning()
        _queue.value = tracks
        val items = tracks.map { t ->
            MediaItem.Builder().setUri(Uri.parse(t.uri)).setMediaId(t.id).build()
        }
        player.setMediaItems(items, startIndex.coerceIn(0, tracks.lastIndex), 0)
        player.prepare()
        player.play()
        _currentIndex.value = startIndex
    }

    fun play() {
        ensureServiceRunning()
        player.play()
    }
    fun pause() = player.pause()
    fun toggle() { if (player.isPlaying) player.pause() else player.play() }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem() else if (_repeatMode.value == 1) {
            player.seekTo(0, 0); player.play()
        }
    }

    fun previous() {
        if (player.currentPosition > 3000) player.seekTo(0) else player.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceIn(0, player.duration.coerceAtLeast(0)))
        _positionMs.value = player.currentPosition
    }

    fun seekBy(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun setShuffle(enabled: Boolean) {
        _shuffle.value = enabled
        player.shuffleModeEnabled = enabled
    }

    fun cycleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
        player.repeatMode = when (_repeatMode.value) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed.coerceIn(0.25f, 3f)
        applyPlaybackParameters()
    }

    /** When false, pitch follows speed (classic chipmunk effect). Default true. */
    var pitchCorrection: Boolean = true
        set(value) {
            field = value
            applyPlaybackParameters()
        }

    private fun applyPlaybackParameters() {
        player.playbackParameters = PlaybackParameters(
            _speed.value,
            if (pitchCorrection) 1f else _speed.value
        )
    }

    /** Rebuilds the ExoPlayer queue after a queue edit, preserving position. */
    private fun rebuildQueue(keepIndex: Int, playFrom: Int = -1) {
        val tracks = _queue.value
        if (tracks.isEmpty()) {
            player.clearMediaItems()
            _currentIndex.value = -1
            return
        }
        val items = tracks.map { t ->
            MediaItem.Builder().setUri(Uri.parse(t.uri)).setMediaId(t.id).build()
        }
        val wasPlaying = player.isPlaying
        val pos = player.currentPosition
        player.setMediaItems(items, keepIndex.coerceIn(0, tracks.lastIndex), pos)
        player.prepare()
        if (wasPlaying || playFrom >= 0) player.play()
        _currentIndex.value = keepIndex
    }

    fun moveQueueItem(from: Int, to: Int) {
        val list = _queue.value.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        val currentId = currentTrack?.id
        _queue.value = list
        val newIndex = list.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        rebuildQueue(newIndex)
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index !in list.indices) return
        val wasCurrent = index == _currentIndex.value
        list.removeAt(index)
        _queue.value = list
        if (list.isEmpty()) {
            player.clearMediaItems()
            _currentIndex.value = -1
            return
        }
        val newIndex = when {
            wasCurrent -> index.coerceAtMost(list.lastIndex)
            index < _currentIndex.value -> _currentIndex.value - 1
            else -> _currentIndex.value
        }
        rebuildQueue(newIndex, playFrom = if (wasCurrent) newIndex else -1)
    }

    fun clearQueue() {
        _queue.value = emptyList()
        player.clearMediaItems()
        _currentIndex.value = -1
    }

    // --- A-B loop ---
    private var abA: Long? = null
    private var abB: Long? = null
    val abLoopActive: Boolean get() = abA != null && abB != null

    fun toggleABPoint(): String {
        return when {
            abA == null -> {
                abA = player.currentPosition
                "A"
            }
            abB == null -> {
                abB = player.currentPosition
                if (abB!! <= abA!!) { abA = null; abB = null; return "cleared" }
                startABPolling()
                "B"
            }
            else -> {
                abA = null; abB = null; abJob?.cancel(); "cleared"
            }
        }
    }

    private fun startABPolling() {
        abJob?.cancel()
        abJob = scope.launch {
            while (isActive && abA != null && abB != null) {
                if (player.currentPosition >= abB!!) player.seekTo(abA!!)
                delay(100)
            }
        }
    }

    // --- Sleep timer: fades out over the last 30s, then pauses. Survives rotation
    // (it lives in the app-scoped PlayerManager, not in a screen ViewModel). ---
    fun startSleepTimer(millis: Long) {
        cancelSleepTimer()
        val endAt = System.currentTimeMillis() + millis
        _sleepRemainingMs.value = millis
        sleepJob = scope.launch {
            while (isActive) {
                val remaining = endAt - System.currentTimeMillis()
                if (remaining <= 0) {
                    fadeOutAndPause()
                    _sleepRemainingMs.value = null
                    return@launch
                }
                _sleepRemainingMs.value = remaining
                delay(1000)
            }
        }
    }

    fun startSleepTimerEndOfTrack() {
        cancelSleepTimer()
        sleepJob = scope.launch {
            val startIndex = _currentIndex.value
            while (isActive) {
                if (_currentIndex.value != startIndex) {
                    fadeOutAndPause()
                    _sleepRemainingMs.value = null
                    return@launch
                }
                delay(500)
            }
        }
        _sleepRemainingMs.value = -1L // marker: end-of-track mode
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemainingMs.value = null
        player.volume = 1f
    }

    private suspend fun fadeOutAndPause() {
        var v = player.volume
        while (v > 0.05f) {
            v -= 0.1f
            player.volume = v.coerceAtLeast(0f)
            delay(300)
        }
        player.pause()
        player.volume = 1f
    }

    // --- Audio effects ---
    @UnstableApi
    private fun ensureEffects(): Boolean {
        val sessionId = player.audioSessionId
        if (sessionId == 0) return false
        if (equalizer == null) {
            try {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
                bassBoost = BassBoost(0, sessionId)
                virtualizer = Virtualizer(0, sessionId)
            } catch (e: Exception) {
                _error.value = "Audio effects not supported on this device"
                return false
            }
        }
        return true
    }

    val eqBandCount: Int get() = try { ensureEffects(); equalizer?.numberOfBands?.toInt() ?: 0 } catch (e: Exception) { 0 }
    val eqBandLevelRange: IntRange get() = try {
        ensureEffects()
        val r = equalizer?.bandLevelRange
        if (r != null) IntRange(r[0].toInt(), r[1].toInt()) else IntRange(0, 0)
    } catch (e: Exception) { IntRange(0, 0) }

    fun eqBandFreqHz(band: Int): Int = try {
        ensureEffects()
        (equalizer?.getCenterFreq(band.toShort()) ?: 0) / 1000
    } catch (e: Exception) { 0 }

    fun setEqEnabled(enabled: Boolean) {
        if (ensureEffects()) equalizer?.enabled = enabled
    }

    fun setEqBand(band: Int, levelMillibels: Int) {
        if (ensureEffects()) {
            try { equalizer?.setBandLevel(band.toShort(), levelMillibels.toShort()) } catch (e: Exception) { }
        }
    }

    fun applyEqPreset(levelsMillibels: List<Int>) {
        if (!ensureEffects()) return
        levelsMillibels.forEachIndexed { i, level ->
            try { equalizer?.setBandLevel(i.toShort(), level.toShort()) } catch (e: Exception) { }
        }
    }

    fun setBassBoost(strengthPermille: Int) {
        if (ensureEffects()) {
            try {
                bassBoost?.enabled = strengthPermille > 0
                bassBoost?.setStrength(strengthPermille.toShort())
            } catch (e: Exception) { }
        }
    }

    fun setVirtualizer(strengthPermille: Int) {
        if (ensureEffects()) {
            try {
                virtualizer?.enabled = strengthPermille > 0
                virtualizer?.setStrength(strengthPermille.toShort())
            } catch (e: Exception) { }
        }
    }

    fun clearError() { _error.value = null }

    /**
     * (Re)starts the foreground playback service so the notification and
     * lock-screen controls come back after the system stopped the service
     * (idle stop, or the phone's power manager killing it in background).
     * No-op when it is already running. The try/catch covers the API 26+
     * background-start restriction — play is a foreground user action, but
     * the call must never crash the tap.
     */
    private fun ensureServiceRunning() {
        try {
            val intent = android.content.Intent(context, PlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            // Service could not be (re)started; playback still proceeds in-app.
        }
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition
                _durationMs.value = player.duration.coerceAtLeast(0)
                delay(500)
            }
        }
    }
}
