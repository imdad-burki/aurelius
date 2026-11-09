package com.imdadburki.aurelius.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.imdadburki.aurelius.R
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.OutlineGoldButton

/**
 * Onboarding flow (3 pages): Welcome → Permissions → Features.
 * No sign-in is offered: authentication is not implemented, and the spec forbids a
 * sign-in placeholder when auth does not exist.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier.fillMaxSize().background(AureliusColors.Background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (page < 2) {
                TextButton(onClick = onDone) {
                    Text("Skip", style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary))
                }
            }
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "onboarding"
        ) { p ->
            when (p) {
                0 -> WelcomePage { page = 1 }
                1 -> PermissionsPage(onNext = { page = 2 })
                else -> FeaturesPage(onDone)
            }
        }
        Spacer(Modifier.height(16.dp))
        OnboardingDots(page = page, count = 3)
    }
}

@Composable
private fun WelcomePage(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.illus_welcome_column),
            contentDescription = null,
            modifier = Modifier.size(280.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(24.dp))
        Text("Your Music.", style = AureliusType.Heading, textAlign = TextAlign.Center)
        Text("Your Rules.", style = AureliusType.Heading.copy(color = AureliusColors.GoldPrimary), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "A music player built for people who take listening seriously.",
            style = AureliusType.Body, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        GoldButton("Continue", onContinue, Modifier.fillMaxWidth())
    }
}

@Composable
private fun PermissionsPage(onNext: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(false) }
    val needed = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.READ_MEDIA_AUDIO)
            else add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results -> granted = results.values.all { it } }
    fun isGranted(perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.illus_hand_phone),
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text("Let's Find ", style = AureliusType.Heading, textAlign = TextAlign.Center)
        Text("Your Music.", style = AureliusType.Heading.copy(color = AureliusColors.GoldPrimary), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Aurelius needs access to your device to build your library. Nothing leaves your phone.",
            style = AureliusType.Body, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PermissionRow(
            icon = Icons.Default.Folder,
            title = "Read Music Files",
            subtitle = "Access your local songs.",
            granted = needed.all { isGranted(it) },
            onRequest = { launcher.launch(needed) }
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Default.Notifications,
            title = "Show Notifications",
            subtitle = "Control playback from notifications.",
            granted = Build.VERSION.SDK_INT < 33 || isGranted(Manifest.permission.POST_NOTIFICATIONS),
            onRequest = {
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        )
        Spacer(Modifier.height(12.dp))
        PermissionRow(
            icon = Icons.Default.PlayArrow,
            title = "Play in Background",
            subtitle = "Keep music playing when screen is off.",
            granted = true,
            onRequest = {}
        )
        Spacer(Modifier.height(24.dp))
        GoldButton("Allow All", { launcher.launch(needed); onNext() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNext) {
            Text("Not now", style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary))
        }
        if (granted) Text("Permissions granted.", style = AureliusType.Caption)
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AureliusColors.Surface),
        onClick = onRequest
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AureliusColors.GoldPrimary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = AureliusType.Title)
                Text(subtitle, style = AureliusType.Body)
            }
            Text(
                if (granted) "✓" else "○",
                style = AureliusType.Title.copy(color = AureliusColors.GoldPrimary)
            )
        }
    }
}

@Composable
private fun FeaturesPage(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.illus_magnifier),
            contentDescription = null,
            modifier = Modifier.size(240.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(16.dp))
        Text("Everything ", style = AureliusType.Heading, textAlign = TextAlign.Center)
        Text("You Need.", style = AureliusType.Heading.copy(color = AureliusColors.GoldPrimary), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Stream, download, equalize, and control playback with gestures. It's all here.",
            style = AureliusType.Body, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureLine("Powerful 10-band equalizer with presets")
            FeatureLine("YouTube search with in-app playback")
            FeatureLine("Lyrics, queue, sleep timer and gestures")
            FeatureLine("Background playback and lock-screen controls")
        }
        Spacer(Modifier.height(24.dp))
        GoldButton("Get Started", onDone, Modifier.fillMaxWidth())
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✓ ", style = AureliusType.Body.copy(color = AureliusColors.GoldPrimary))
        Text(text, style = AureliusType.Body)
    }
}
