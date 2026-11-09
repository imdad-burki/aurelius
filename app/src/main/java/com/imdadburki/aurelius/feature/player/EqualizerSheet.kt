package com.imdadburki.aurelius.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusType
import com.imdadburki.aurelius.core.design.GoldButton
import com.imdadburki.aurelius.core.design.GoldSlider
import com.imdadburki.aurelius.core.design.OutlineGoldButton

/**
 * Professional 10-band equalizer sheet: enable switch, presets, vertical band
 * sliders (-15..+15 dB), bass boost, virtualizer, reset. Levels apply to the
 * platform Equalizer attached to the player audio session; unsupported devices
 * report it honestly via the player's error flow.
 *
 * v2.0.1 REBUILD: the old sheet used a rotated horizontal Slider (140dp wide)
 * inside a ~31dp column — all ten sliders overlapped and the labels collided.
 * Each band is now a real vertical slider drawn on Canvas with drag + tap,
 * one fixed-width column per band, value above and frequency below.
 */
@Composable
fun EqualizerSheetContent(vm: PlayerViewModel, onClose: () -> Unit) {
    val bands by vm.eqBands.collectAsState()
    val preset by vm.eqPreset.collectAsState()
    val eqOn by vm.eqEnabled.collectAsState()
    val bass by vm.bassBoostPct.collectAsState()
    val virt by vm.virtualizerPct.collectAsState()
    val freqs = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text("AURELIUS", style = AureliusType.Caption.copy(color = AureliusColors.GoldPrimary))
        Text(
            "PROFESSIONAL AUDIO EQUALIZER",
            style = AureliusType.Title.copy(fontSize = 16.sp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", style = AureliusType.Title, maxLines = 1)
            Switch(
                checked = eqOn,
                onCheckedChange = { vm.setEqEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AureliusColors.GoldPrimary,
                    checkedTrackColor = AureliusColors.GoldDark
                )
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EqPresets.names) { name ->
                FilterChip(
                    selected = preset == name,
                    onClick = { vm.applyPreset(name) },
                    label = { Text(name, style = AureliusType.Body, maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AureliusColors.GoldPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = AureliusColors.Elevated,
                        labelColor = AureliusColors.TextSecondary
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            bands.forEachIndexed { i, levelMillibels ->
                val db = levelMillibels / 100
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(30.dp)
                ) {
                    Text(
                        if (db > 0) "+$db" else "$db",
                        style = AureliusType.Caption.copy(color = AureliusColors.GoldPrimary),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    VerticalBandSlider(
                        valueMillibels = levelMillibels,
                        onValueChange = { vm.setBand(i, it) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        freqs[i],
                        style = AureliusType.Caption,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Bass Boost $bass%", style = AureliusType.Body, maxLines = 1)
        GoldSlider(
            value = bass.toFloat(),
            onValueChange = { vm.setBassBoost(it.toInt()) },
            valueRange = 0f..100f
        )
        Spacer(Modifier.height(8.dp))
        Text("Virtualizer $virt%", style = AureliusType.Body, maxLines = 1)
        GoldSlider(
            value = virt.toFloat(),
            onValueChange = { vm.setVirtualizer(it.toInt()) },
            valueRange = 0f..100f
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineGoldButton("Reset", { vm.applyPreset("Flat") }, Modifier.weight(1f))
            GoldButton("Done", onClose, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Pure helpers for the vertical band slider (unit-tested). */
fun bandFractionFromMillibels(mb: Int): Float = ((mb + 1500) / 3000f).coerceIn(0f, 1f)
// roundToInt, not toInt: float 0.3 is stored as 0.30000001, so a truncated
// round-trip of -600 dB came back as -599.
fun millibelsFromBandFraction(f: Float): Int =
    ((f.coerceIn(0f, 1f) * 3000f) - 1500f).roundToInt()

/** A true vertical slider: 0 dB in the middle, drag up for +dB, down for -dB. */
@Composable
private fun VerticalBandSlider(
    valueMillibels: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = bandFractionFromMillibels(valueMillibels)
    val gold = AureliusColors.GoldPrimary
    val border = AureliusColors.Border
    Box(
        modifier = modifier
            .width(28.dp)
            .height(140.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    onValueChange(millibelsFromBandFraction(1f - change.position.y / h))
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    onValueChange(millibelsFromBandFraction(1f - offset.y / h))
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val trackW = 4.dp.toPx()
            val cx = size.width / 2f
            val centerY = size.height / 2f
            val thumbY = size.height * (1f - fraction)
            // Full inactive track.
            drawLine(border, Offset(cx, 0f), Offset(cx, size.height), trackW, StrokeCap.Round)
            // Active fill from centre (0 dB) to the thumb.
            drawLine(gold, Offset(cx, centerY), Offset(cx, thumbY), trackW, StrokeCap.Round)
            // Thumb: gold disc with a black centre dot.
            drawCircle(gold, 10.dp.toPx(), Offset(cx, thumbY))
            drawCircle(Color.Black, 4.dp.toPx(), Offset(cx, thumbY))
        }
    }
}
