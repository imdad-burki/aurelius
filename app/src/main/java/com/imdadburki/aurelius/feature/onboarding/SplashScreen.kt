package com.imdadburki.aurelius.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import kotlinx.coroutines.delay

/** Splash: obsidian black, golden A + waveform logo, wordmark, subtle loading bar. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableStateOf(0f) }
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1600), label = "splash"
    )
    LaunchedEffect(Unit) {
        progress = 1f
        delay(1800)
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.aurelius_logo),
                contentDescription = "Aurelius",
                modifier = Modifier.size(220.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("AURELIUS", style = AureliusType.WordmarkSplash, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
            Spacer(Modifier.height(8.dp))
            Text("Music. Mastered.", style = AureliusType.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.width(160.dp),
                color = AureliusColors.GoldPrimary,
                trackColor = AureliusColors.Border
            )
        }
    }
}

@Composable
fun OnboardingDots(page: Int, count: Int) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == page) 10.dp else 8.dp)
                    .background(
                        if (i == page) AureliusColors.GoldPrimary else AureliusColors.Border,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}
