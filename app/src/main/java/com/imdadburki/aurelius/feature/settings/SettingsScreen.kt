package com.imdadburki.aurelius.feature.settings

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.BuildConfig
import com.imdadburki.aurelius.core.data.SettingsStore
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.TopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings: Playback / Appearance / About.
 * Every row works or states its limitation plainly.
 *
 * v2.0.1: the YouTube section is gone entirely — the API key is built into the
 * app (backend only) and no key field, dialog or option appears anywhere.
 * The Play Store row is gone too (the app is not on the Play Store).
 * About is a short centered block.
 * v2.0.3: the STORAGE section is gone entirely (Download Location, Cache Size,
 * Clear Cache) at Imdad's order — the app downloads nothing, so the rows were
 * dead weight. Crossfade subtitle shortened + value padded so the "Off" value
 * can never run into the description text.
 */
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onOpenLanguage: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val gapless by settings.gapless.collectAsState(false)
    val audioFocus by settings.audioFocus.collectAsState(true)
    val pitch by settings.pitchCorrection.collectAsState(true)
    val uiSounds by settings.uiSounds.collectAsState(true)
    val haptics by settings.haptics.collectAsState(true)
    val language by settings.language.collectAsState("en")
    val crossfade by settings.crossfadeSeconds.collectAsState(0)
    var showLicenses by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showCrossfade by remember { mutableStateOf(false) }

    fun openMail(subject: String, body: String = "") {
        val uri = "mailto:burkiimdad6@gmail.com?subject=${Uri.encode(subject)}" +
            if (body.isNotEmpty()) "&body=${Uri.encode(body)}" else ""
        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(uri)))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        item { TopBar(title = "Settings", onBack = onBack) }

        item { SectionHeader("PLAYBACK") }
        item {
            SwitchRow("Gapless Playback", "Seamless transitions between tracks.", gapless) {
                scope.launch { settings.setGapless(it) }
            }
            SwitchRow("Audio Focus", "Pause when another app needs audio.", audioFocus) {
                scope.launch { settings.setAudioFocus(it) }
            }
            SwitchRow("Pitch Correction", "Keep the original pitch at other speeds.", pitch) {
                scope.launch { settings.setPitchCorrection(it) }
            }
            ValueRow("Crossfade", if (crossfade == 0) "Off" else "$crossfade seconds", "Stored only — not applied yet.") {
                showCrossfade = true
            }
        }

        item { SectionHeader("APPEARANCE") }
        item {
            ValueRow("Theme", "Aurum Dark", null) { }
            ValueRow("Language", LanguageNames[language] ?: "English", null) { onOpenLanguage() }
            SwitchRow("UI Sounds", "Play soft sounds on taps.", uiSounds) {
                scope.launch { settings.setUiSounds(it) }
            }
            SwitchRow("Haptics", "Vibrate gently on taps.", haptics) {
                scope.launch { settings.setHaptics(it) }
            }
        }

        item { SectionHeader("ABOUT") }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AURELIUS",
                    style = AureliusType.WordmarkSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Music. Mastered.",
                    style = AureliusType.Body,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    style = AureliusType.Caption,
                    textAlign = TextAlign.Center
                )
                // Diagnostic for the YouTube-player black-screen bug: the
                // device's WebView version decides whether YouTube's
                // player JS can render at all. Shown here so it can be
                // read off the phone without any tooling.
                Text(
                    "WebView ${remember { WebView.getCurrentWebViewPackage()?.versionName ?: "unknown" }}",
                    style = AureliusType.Caption,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Developer — Imdad Burki",
                    style = AureliusType.Caption,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "burkiimdad6@gmail.com",
                    style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { openMail("Aurelius") }.padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                CenterAction("Send Feedback") { openMail("Aurelius Feedback") }
                CenterAction("Report a Bug") {
                    openMail(
                        "Aurelius Bug Report",
                        "Device: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})\nApp: ${BuildConfig.VERSION_NAME}"
                    )
                }
                CenterAction("Licenses") { showLicenses = true }
                CenterAction("Privacy Policy") { showPrivacy = true }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showCrossfade) {
        AlertDialog(
            onDismissRequest = { showCrossfade = false },
            title = { Text("Crossfade", style = AureliusType.Title) },
            text = {
                Column {
                    listOf(0, 2, 5, 10).forEach { secs ->
                        Text(
                            if (secs == 0) "Off" else "$secs seconds",
                            style = AureliusType.Body.copy(
                                color = if (crossfade == secs) AureliusColors.GoldPrimary else AureliusColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch { settings.setCrossfadeSeconds(secs) }
                                showCrossfade = false
                            }.padding(vertical = 10.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Note: the current playback engine does not apply crossfade; the value is stored for a future engine.", style = AureliusType.Caption)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCrossfade = false }) { Text("Close", style = AureliusType.Body) }
            },
            containerColor = AureliusColors.Surface
        )
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-Source Licenses", style = AureliusType.Title) },
            text = {
                Text(
                    "AndroidX Media3 (Apache 2.0)\nJetpack Compose (Apache 2.0)\nRoom (Apache 2.0)\nDataStore (Apache 2.0)\nCoil (Apache 2.0)\nRetrofit + OkHttp (Apache 2.0)\nKotlinx Serialization (Apache 2.0)\nNavigation Compose (Apache 2.0)\n\nFonts: Poppins, Inter, JetBrains Mono, Cormorant Garamond (SIL OFL 1.1)",
                    style = AureliusType.Body
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLicenses = false }) { Text("Close", style = AureliusType.Body) }
            },
            containerColor = AureliusColors.Surface
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy Policy", style = AureliusType.Title) },
            text = {
                Text(
                    "Aurelius keeps everything on your device. Your music library, playlists, favorites, history and settings never leave your phone.\n\nYouTube search uses the official YouTube Data API v3; search queries go to Google under Google's privacy policy. Lyrics lookups go to LRCLIB. No account, no tracking, no ads SDK, no analytics.",
                    style = AureliusType.Body
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("Close", style = AureliusType.Body) }
            },
            containerColor = AureliusColors.Surface
        )
    }
}

@Composable
private fun CenterAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp)
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = AureliusType.Caption.copy(color = AureliusColors.GoldPrimary),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AureliusType.Title, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = AureliusType.Body)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AureliusColors.GoldPrimary,
                checkedTrackColor = AureliusColors.GoldDark
            )
        )
    }
}

@Composable
private fun ValueRow(title: String, value: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AureliusType.Title, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = AureliusType.Body)
        }
        if (value.isNotEmpty()) {
            Text(
                value,
                style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary),
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

val LanguageNames = mapOf(
    "en" to "English", "hi" to "Hindi", "ur" to "Urdu", "ar" to "Arabic",
    "es" to "Spanish", "fr" to "French", "de" to "German", "pt" to "Portuguese",
    "ru" to "Russian", "zh" to "Chinese", "ja" to "Japanese", "id" to "Indonesian"
)
