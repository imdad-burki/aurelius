package com.imdadburki.aurelius.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aurelius_settings")

/** All persisted user settings. Backed by DataStore Preferences. */
class SettingsStore(private val context: Context) {
    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val LANGUAGE = stringPreferencesKey("language")
        val GAPLESS = booleanPreferencesKey("gapless")
        val CROSSFADE_SECONDS = intPreferencesKey("crossfade_seconds")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val PITCH_CORRECTION = booleanPreferencesKey("pitch_correction")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS = stringPreferencesKey("eq_bands")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val UI_SOUNDS = booleanPreferencesKey("ui_sounds")
        val HAPTICS = booleanPreferencesKey("haptics")
        val THEME = stringPreferencesKey("theme")
        val DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDED] ?: false }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val gapless: Flow<Boolean> = context.dataStore.data.map { it[Keys.GAPLESS] ?: true }
    val crossfadeSeconds: Flow<Int> = context.dataStore.data.map { it[Keys.CROSSFADE_SECONDS] ?: 0 }
    val audioFocus: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUDIO_FOCUS] ?: true }
    val pitchCorrection: Flow<Boolean> = context.dataStore.data.map { it[Keys.PITCH_CORRECTION] ?: true }
    val eqEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.EQ_ENABLED] ?: false }
    val eqPreset: Flow<String> = context.dataStore.data.map { it[Keys.EQ_PRESET] ?: "Flat" }
    val eqBands: Flow<String> = context.dataStore.data.map { it[Keys.EQ_BANDS] ?: "" }
    val bassBoost: Flow<Int> = context.dataStore.data.map { it[Keys.BASS_BOOST] ?: 0 }
    val virtualizer: Flow<Int> = context.dataStore.data.map { it[Keys.VIRTUALIZER] ?: 0 }
    val uiSounds: Flow<Boolean> = context.dataStore.data.map { it[Keys.UI_SOUNDS] ?: true }
    val haptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS] ?: true }
    val theme: Flow<String> = context.dataStore.data.map { it[Keys.THEME] ?: "Aurum Dark" }
    val playbackSpeed: Flow<Float> = context.dataStore.data.map { it[Keys.PLAYBACK_SPEED] ?: 1f }

    suspend fun setOnboarded(v: Boolean) = edit { it[Keys.ONBOARDED] = v }
    suspend fun setLanguage(v: String) = edit { it[Keys.LANGUAGE] = v }
    suspend fun setGapless(v: Boolean) = edit { it[Keys.GAPLESS] = v }
    suspend fun setCrossfadeSeconds(v: Int) = edit { it[Keys.CROSSFADE_SECONDS] = v }
    suspend fun setAudioFocus(v: Boolean) = edit { it[Keys.AUDIO_FOCUS] = v }
    suspend fun setPitchCorrection(v: Boolean) = edit { it[Keys.PITCH_CORRECTION] = v }
    suspend fun setEqEnabled(v: Boolean) = edit { it[Keys.EQ_ENABLED] = v }
    suspend fun setEqPreset(v: String) = edit { it[Keys.EQ_PRESET] = v }
    suspend fun setEqBands(v: String) = edit { it[Keys.EQ_BANDS] = v }
    suspend fun setBassBoost(v: Int) = edit { it[Keys.BASS_BOOST] = v }
    suspend fun setVirtualizer(v: Int) = edit { it[Keys.VIRTUALIZER] = v }
    suspend fun setUiSounds(v: Boolean) = edit { it[Keys.UI_SOUNDS] = v }
    suspend fun setHaptics(v: Boolean) = edit { it[Keys.HAPTICS] = v }
    suspend fun setTheme(v: String) = edit { it[Keys.THEME] = v }
    suspend fun setPlaybackSpeed(v: Float) = edit { it[Keys.PLAYBACK_SPEED] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
