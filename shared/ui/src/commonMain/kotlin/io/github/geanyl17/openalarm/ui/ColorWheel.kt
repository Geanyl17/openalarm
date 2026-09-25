package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.brightness
import io.github.geanyl17.openalarm.ui.resources.cancel
import io.github.geanyl17.openalarm.ui.resources.color_custom
import io.github.geanyl17.openalarm.ui.resources.color_wheel
import io.github.geanyl17.openalarm.ui.resources.ic_alarm
import io.github.geanyl17.openalarm.ui.resources.select
import io.github.geanyl17.openalarm.ui.resources.turn_off
import io.github.geanyl17.openalarm.ui.theme.OpenAlarmTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val WheelSize = 240.dp
private val ThumbRadius = 14.dp
private const val MIN_BRIGHTNESS = 0.15f
private val SideBySideControlsWidth = 240.dp

/** A color as hue (0–360°), saturation (0–1) and value, meaning brightness (0–1). */
internal data class Hsv(val hue: Float, val saturation: Float, val value: Float) {

    fun toColor(): Color = Color.hsv(hue, saturation, value)

    companion object {
        fun from(color: Color): Hsv {
            val r = color.red
            val g = color.green
            val b = color.blue
            val max = maxOf(r, g, b)
            val delta = max - minOf(r, g, b)
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            return Hsv(
                hue = if (hue < 0f) hue + 360f else hue,
                saturation = if (max == 0f) 0f else delta / max,
                value = max,
            )
        }
    }
}

/** Where [hsv] sits on a wheel of [radius], relative to its center. Hue is the angle, saturation the distance. */
internal fun wheelOffset(hsv: Hsv, radius: Float): Offset {
    val angle = hsv.hue * PI / 180
    val distance = hsv.saturation * radius
    return Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat())
}

/** The color at [offset] from the center of a wheel of [radius]. Points outside the wheel count as its edge. */
internal fun wheelHsv(offset: Offset, radius: Float, value: Float): Hsv {
    val degrees = atan2(offset.y, offset.x) * 180f / PI.toFloat()
    return Hsv(
        hue = (degrees + 360f) % 360f,
        saturation = (offset.getDistance() / radius).coerceIn(0f, 1f),
        value = value,
    )
}

internal fun Color.hex(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0').uppercase()

/** Lets the user pick any color, starting from [initial]. */
@Composable
internal fun ColorWheelDialog(initial: Color, onDismiss: () -> Unit, onSelect: (Color) -> Unit) {
    var hsv by remember {
        mutableStateOf(Hsv.from(initial).let { it.copy(value = it.value.coerceAtLeast(MIN_BRIGHTNESS)) })
    }
    val onChange = { new: Hsv -> hsv = new }
    // A short window has no room for the controls under the wheel, so they go beside it in a wider dialog.
    val sideBySide = isShortWindow()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.color_custom)) },
        text = {
            if (sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    // As big as the dialog's height allows.
                    ColorWheel(hsv, onChange, Modifier.heightIn(max = WheelSize).aspectRatio(1f, matchHeightConstraintsFirst = true))
                    ColorControls(hsv, onChange, Modifier.width(SideBySideControlsWidth).verticalScroll(rememberScrollState()))
                }
            } else {
                // Scrolls if it still doesn't fit, such as with very large text.
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorWheel(hsv, onChange, Modifier.size(WheelSize))
                    ColorControls(hsv, onChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(hsv.toColor()) }) { Text(stringResource(Res.string.select)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
        properties = DialogProperties(usePlatformDefaultWidth = !sideBySide),
    )
}

@Composable
private fun ColorWheel(hsv: Hsv, onChange: (Hsv) -> Unit, modifier: Modifier) {
    val currentHsv by rememberUpdatedState(hsv)
    val currentOnChange by rememberUpdatedState(onChange)
    // Compose's sweep gradient starts at 3 o'clock and runs clockwise, matching atan2 on screen coordinates.
    val hues = remember { (0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) } }
    val description = stringResource(Res.string.color_wheel)

    Canvas(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = minOf(size.width, size.height) / 2f
                    fun pick(position: Offset) = currentOnChange(wheelHsv(position - center, radius, currentHsv.value))
                    val down = awaitFirstDown()
                    pick(down.position)
                    drag(down.id) { change ->
                        change.consume()
                        pick(change.position)
                    }
                }
            }
            .semantics {
                contentDescription = description
                stateDescription = hsv.toColor().hex()
            },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(hues, center))
        // Saturation: white in the middle, full color at the edge.
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, radius))
        // Brightness darkens the whole wheel.
        drawCircle(Color.Black.copy(alpha = 1f - hsv.value))

        val thumb = center + wheelOffset(hsv, radius)
        val thumbRadius = ThumbRadius.toPx()
        drawCircle(hsv.toColor(), thumbRadius, thumb)
        drawCircle(Color.White, thumbRadius, thumb, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black.copy(alpha = 0.35f), thumbRadius + 2.dp.toPx(), thumb, style = Stroke(1.dp.toPx()))
    }
}

/** The color's hex code, a brightness slider and a preview of the alarm in the color. */
@Composable
private fun ColorControls(hsv: Hsv, onChange: (Hsv) -> Unit, modifier: Modifier = Modifier) {
    val color = hsv.toColor()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(color.hex(), style = MaterialTheme.typography.labelLarge)
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.brightness), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = hsv.value,
                onValueChange = { onChange(hsv.copy(value = it)) },
                valueRange = MIN_BRIGHTNESS..1f,
            )
        }
        ThemePreview(color)
    }
}

/** How an alarm in [seed] will look. The app builds a readable theme from the color, which can shift its shade. */
@Composable
private fun ThemePreview(seed: Color) {
    OpenAlarmTheme(seedColor = seed) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Res.drawable.ic_alarm), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.weight(1f))
                Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape) {
                    Text(
                        text = stringResource(Res.string.turn_off),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
