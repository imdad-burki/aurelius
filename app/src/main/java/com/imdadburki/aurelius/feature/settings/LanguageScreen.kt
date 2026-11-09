package com.imdadburki.aurelius.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.core.data.SettingsStore
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.TopBar
import kotlinx.coroutines.launch

data class LanguageOption(val code: String, val nativeName: String, val englishName: String)

val SupportedLanguages = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("hi", "हिन्दी", "Hindi"),
    LanguageOption("ur", "اُردو", "Urdu"),
    LanguageOption("ar", "العربية", "Arabic"),
    LanguageOption("es", "Español", "Spanish"),
    LanguageOption("fr", "Français", "French"),
    LanguageOption("de", "Deutsch", "German"),
    LanguageOption("pt", "Português", "Portuguese"),
    LanguageOption("ru", "Русский", "Russian"),
    LanguageOption("zh", "简体中文", "Chinese"),
    LanguageOption("ja", "日本語", "Japanese"),
    LanguageOption("id", "Bahasa Indonesia", "Indonesian")
)

/**
 * Language selector: 12 languages, native script, persistent preference.
 * The choice applies on the next launch (activity recreation); English is the
 * fallback for any string not yet translated.
 */
@Composable
fun LanguageScreen(settings: SettingsStore, onBack: () -> Unit, onLanguageChanged: () -> Unit) {
    val current by settings.language.collectAsState("en")
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
    ) {
        TopBar(title = "Language", onBack = onBack)
        Text(
            "Choose your preferred language.",
            style = AureliusType.Body,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(SupportedLanguages) { lang ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        scope.launch { settings.setLanguage(lang.code) }
                        onLanguageChanged()
                    }.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lang.nativeName, style = AureliusType.Title)
                        Text(lang.englishName, style = AureliusType.Body)
                    }
                    RadioButton(
                        selected = current == lang.code,
                        onClick = {
                            scope.launch { settings.setLanguage(lang.code) }
                            onLanguageChanged()
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = AureliusColors.GoldPrimary)
                    )
                }
                Spacer(Modifier.width(0.dp))
            }
        }
    }
}
