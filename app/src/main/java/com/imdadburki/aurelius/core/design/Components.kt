package com.imdadburki.aurelius.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.imdadburki.aurelius.core.data.Track
import com.imdadburki.aurelius.core.data.displayArtist

/** Primary gold button — 12dp radius, black text on gold. */
@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AureliusColors.GoldPrimary,
            contentColor = Color.Black,
            disabledContainerColor = AureliusColors.Elevated,
            disabledContentColor = AureliusColors.TextTertiary
        )
    ) {
        Text(text, style = AureliusType.Title.copy(color = Color.Black))
    }
}

/** Gold outline button — transparent fill, gold border and text. */
@Composable
fun OutlineGoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AureliusColors.GoldPrimary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AureliusColors.GoldPrimary)
    ) {
        Text(text, style = AureliusType.Title.copy(color = AureliusColors.GoldPrimary))
    }
}

/** Gold slider used for seek, volume, EQ sliders. */
@Composable
fun GoldSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = AureliusColors.GoldPrimary,
            activeTrackColor = AureliusColors.GoldPrimary,
            inactiveTrackColor = AureliusColors.Border
        )
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = AureliusType.Title)
        if (onSeeAll != null) {
            Text(
                "See all",
                style = AureliusType.Caption.copy(color = AureliusColors.GoldPrimary),
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

@Composable
fun TrackArtwork(track: Track, modifier: Modifier = Modifier, cornerDp: Int = 8) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerDp.dp))) {
        if (track.artworkUri != null) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                error = painterResource(id = com.imdadburki.aurelius.R.drawable.aurelius_logo),
                fallback = painterResource(id = com.imdadburki.aurelius.R.drawable.aurelius_logo)
            )
        } else {
            Image(
                painter = painterResource(id = com.imdadburki.aurelius.R.drawable.aurelius_logo),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingNumber: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    isCurrent: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingNumber != null) {
            Text(
                leadingNumber,
                style = AureliusType.Body.copy(color = AureliusColors.TextTertiary),
                modifier = Modifier.width(28.dp)
            )
        }
        TrackArtwork(track, modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = AureliusType.Title.copy(
                    color = if (isCurrent) AureliusColors.GoldPrimary else AureliusColors.TextPrimary
                ),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                displayArtist(track.artist),
                style = AureliusType.Body,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(formatDuration(track.durationMs), style = AureliusType.Body)
        trailing?.invoke()
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = AureliusColors.GoldPrimary
                )
            }
        }
        Text(
            title,
            style = AureliusType.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = if (onBack == null) 16.dp else 0.dp)
        )
        actions?.invoke()
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AureliusColors.GoldPrimary)
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = AureliusType.Body, color = AureliusColors.Error)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            OutlineGoldButton("Retry", onRetry)
        }
    }
}

@Composable
fun EmptyState(
    drawableRes: Int,
    title: String,
    subtitle: String,
    primaryAction: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryAction: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = null,
            modifier = Modifier.size(220.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(24.dp))
        Text(title, style = AureliusType.Heading)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = AureliusType.Body)
        if (primaryAction != null && onPrimary != null) {
            Spacer(Modifier.height(24.dp))
            GoldButton(primaryAction, onPrimary, Modifier.fillMaxWidth())
        }
        if (secondaryAction != null && onSecondary != null) {
            Spacer(Modifier.height(12.dp))
            OutlineGoldButton(secondaryAction, onSecondary, Modifier.fillMaxWidth())
        }
    }
}

/** mm:ss or h:mm:ss from milliseconds. */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Small gold circular icon button (player controls). */
@Composable
fun GoldCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 56,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(AureliusColors.GoldPrimary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.Black, modifier = Modifier.size((sizeDp * 0.45).dp))
    }
}
