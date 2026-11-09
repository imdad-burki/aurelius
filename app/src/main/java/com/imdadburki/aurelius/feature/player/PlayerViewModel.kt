package com.imdadburki.aurelius.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.di.AppContainer
import com.imdadburki.aurelius.core.network.LyricLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** EQ presets as band levels in millibels (10 bands: 31..16k Hz). */
object EqPresets {
    val names = listOf("Flat", "Bass Boost", "Vocal", "Rock", "Jazz", "Classical", "Custom")
    private val flat = List(10) { 0 }
    private val bass = listOf(600, 500, 400, 200, 0, 0, 0, 100, 200, 300)
    private val vocal = listOf(-200, -100, 0, 200, 400, 400, 200, 0, -100, -200)
    private val rock = listOf(400, 300, 200, 0, -100, 0, 200, 300, 400, 400)
    private val jazz = listOf(300, 200, 100, 0, 100, 200, 200, 100, 200, 300)
    private val classical = listOf(300, 200, 100, 0, 0, 100, 200, 300, 400, 400)
    fun levels(name: String): List<Int> = when (name) {
        "Bass Boost" -> bass
        "Vocal" -> vocal
        "Rock" -> rock
        "Jazz" -> jazz
        "Classical" -> classical
        else -> flat
    }
}

class PlayerViewModel(private val container: AppContainer) : ViewModel() {
    val player = container.playerManager
    private val _lyrics = MutableStateFlow<List<LyricLine>?>(null)
    val lyrics: StateFlow<List<LyricLine>?> = _lyrics.asStateFlow()
    private val _lyricsState = MutableStateFlow("idle") // idle | loading | done | missing | error
    val lyricsState: StateFlow<String> = _lyricsState.asStateFlow()
    private val _eqBands = MutableStateFlow<List<Int>>(List(10) { 0 })
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()
    private val _eqPreset = MutableStateFlow("Flat")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()
    private val _bassBoostPct = MutableStateFlow(0)
    val bassBoostPct: StateFlow<Int> = _bassBoostPct.asStateFlow()
    private val _virtualizerPct = MutableStateFlow(0)
    val virtualizerPct: StateFlow<Int> = _virtualizerPct.asStateFlow()

    init {
        viewModelScope.launch {
            _eqPreset.value = container.settings.eqPreset.first()
            _eqBands.value = parseBands(container.settings.eqBands.first())
            _eqEnabled.value = container.settings.eqEnabled.first()
            _bassBoostPct.value = container.settings.bassBoost.first()
            _virtualizerPct.value = container.settings.virtualizer.first()
            if (_eqEnabled.value) {
                player.applyEqPreset(_eqBands.value)
                player.setEqEnabled(true)
            }
        }
    }

    fun playTracks(tracks: List<Track>, index: Int) {
        player.playTracks(tracks, index)
        val t = tracks.getOrNull(index)
        if (t != null) {
            fetchLyrics(t)
        }
    }

    fun fetchLyrics(track: Track) {
        viewModelScope.launch {
            _lyricsState.value = "loading"
            try {
                val lines = container.lyricsApi.fetchLyrics(track.artist, track.title)
                if (lines.isNullOrEmpty()) {
                    _lyrics.value = null
                    _lyricsState.value = "missing"
                } else {
                    _lyrics.value = lines
                    _lyricsState.value = "done"
                }
            } catch (e: Exception) {
                _lyrics.value = null
                _lyricsState.value = "error"
            }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        player.setEqEnabled(enabled)
        viewModelScope.launch { container.settings.setEqEnabled(enabled) }
    }

    fun applyPreset(name: String) {
        _eqPreset.value = name
        val levels = EqPresets.levels(name)
        _eqBands.value = levels
        player.applyEqPreset(levels)
        viewModelScope.launch {
            container.settings.setEqPreset(name)
            container.settings.setEqBands(levels.joinToString(","))
        }
    }

    fun setBand(band: Int, levelMillibels: Int) {
        _eqPreset.value = "Custom"
        val updated = _eqBands.value.toMutableList().also { it[band] = levelMillibels }
        _eqBands.value = updated
        player.setEqBand(band, levelMillibels)
        viewModelScope.launch {
            container.settings.setEqPreset("Custom")
            container.settings.setEqBands(updated.joinToString(","))
        }
    }

    fun setBassBoost(percent: Int) {
        _bassBoostPct.value = percent
        player.setBassBoost(percent * 10)
        viewModelScope.launch { container.settings.setBassBoost(percent) }
    }

    fun setVirtualizer(percent: Int) {
        _virtualizerPct.value = percent
        player.setVirtualizer(percent * 10)
        viewModelScope.launch { container.settings.setVirtualizer(percent) }
    }

    private fun parseBands(saved: String): List<Int> {
        if (saved.isBlank()) return EqPresets.levels("Flat")
        val parts = saved.split(",").mapNotNull { it.toIntOrNull() }
        return if (parts.size == 10) parts else EqPresets.levels("Flat")
    }
}
