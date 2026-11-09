package com.imdadburki.aurelius.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imdadburki.aurelius.R

/** AURELIUS AURUM DARK — locked design tokens. All colors originate here, never inline. */
object AureliusColors {
    val GoldPrimary = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFEBC468)
    val GoldDark = Color(0xFFA88A2E)
    val Background = Color(0xFF0A0A0A)
    val Surface = Color(0xFF1A1A1A)
    val Elevated = Color(0xFF242424)
    val Border = Color(0xFF2E2E2E)
    val TextPrimary = Color(0xFFF5F5F5)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextTertiary = Color(0xFF6E6E6E)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFE53935)
}

private val Poppins = FontFamily(Font(R.font.poppins_semibold, FontWeight.SemiBold))
private val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium)
)
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, FontWeight.Medium)
)
private val Cormorant = FontFamily(Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold))

object AureliusType {
    val Wordmark = TextStyle(
        fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp, letterSpacing = 8.sp, color = AureliusColors.GoldPrimary
    )
    /** Splash wordmark: same wide-spaced Cormorant, sized so "AURELIUS" fits
     * one line at phone widths (the 40sp Wordmark + literal spaces wrapped
     * to two lines on the splash screen). */
    val WordmarkSplash = TextStyle(
        fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, letterSpacing = 6.sp, color = AureliusColors.GoldPrimary
    )
    /** Single-line wordmark for narrow bars (Home top bar, drawer, player header):
     * 40sp wraps "Aurelius" into two lines on a 360dp phone — this one fits. */
    val WordmarkSmall = TextStyle(
        fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, letterSpacing = 4.sp, color = AureliusColors.GoldPrimary
    )
    /** Compact single-line tab label: the 18sp Title wrapped "Playlists" into
     * "Playlis ts" in a 5-tab row. */
    val TabLabel = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, color = AureliusColors.TextPrimary
    )
    val Heading = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, color = AureliusColors.TextPrimary
    )
    val Title = TextStyle(
        fontFamily = Poppins, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, color = AureliusColors.TextPrimary
    )
    val Body = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, color = AureliusColors.TextSecondary
    )
    val Caption = TextStyle(
        fontFamily = Inter, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, color = AureliusColors.TextTertiary
    )
    val Timer = TextStyle(
        fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium,
        fontSize = 40.sp, color = AureliusColors.GoldPrimary
    )
}

private val AureliusScheme = darkColorScheme(
    primary = AureliusColors.GoldPrimary,
    onPrimary = Color.Black,
    secondary = AureliusColors.GoldLight,
    background = AureliusColors.Background,
    surface = AureliusColors.Surface,
    surfaceVariant = AureliusColors.Elevated,
    onBackground = AureliusColors.TextPrimary,
    onSurface = AureliusColors.TextPrimary,
    onSurfaceVariant = AureliusColors.TextSecondary,
    outline = AureliusColors.Border,
    error = AureliusColors.Error
)

private val AureliusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp)
)

private val AureliusTypography = Typography(
    headlineLarge = AureliusType.Heading,
    headlineMedium = AureliusType.Title,
    titleLarge = AureliusType.Title,
    bodyLarge = AureliusType.Body,
    bodyMedium = AureliusType.Body,
    labelLarge = AureliusType.Body,
    labelMedium = AureliusType.Caption,
    labelSmall = AureliusType.Caption
)

@Composable
fun AureliusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AureliusScheme,
        typography = AureliusTypography,
        shapes = AureliusShapes,
        content = content
    )
}
