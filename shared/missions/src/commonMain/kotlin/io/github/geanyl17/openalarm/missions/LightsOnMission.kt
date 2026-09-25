package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.lights_on_keep
import io.github.geanyl17.openalarm.missions.resources.lights_on_title
import io.github.geanyl17.openalarm.missions.resources.lights_on_too_dark
import io.github.geanyl17.openalarm.missions.resources.seconds_left
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
internal fun LightsOnMission(mission: Mission, light: Flow<Float>, onInteraction: () -> Unit, onDone: () -> Unit) {
    val duration = mission.lightsOnDuration
    val timer = remember { BrightTimer(mission.difficulty.lightsOnLux) }
    // The light sensor only reports changes, so a steady light gives no new readings: the timer ticks on its own.
    var lux by remember { mutableStateOf(0f) }
    var bright by remember { mutableStateOf(Duration.ZERO) }
    val interact by rememberUpdatedState(onInteraction)
    LaunchedEffect(light) { light.collect { lux = it } }
    LaunchedEffect(Unit) {
        val start = TimeSource.Monotonic.markNow()
        var lastInteraction = Duration.ZERO
        while (true) {
            val now = start.elapsedNow()
            bright = timer.update(lux, now)
            if (bright >= duration) {
                onDone()
                break
            }
            // While the lights are on, the alarm stays quiet.
            if (bright > Duration.ZERO && now - lastInteraction >= 1.seconds) {
                interact()
                lastInteraction = now
            }
            delay(100.milliseconds)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.lights_on_title), style = MaterialTheme.typography.titleLarge)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { (bright / duration).toFloat().coerceIn(0f, 1f) },
                strokeWidth = 12.dp,
                modifier = Modifier.size(200.dp),
            )
            Text(
                text = stringResource(Res.string.seconds_left, (duration - bright).inWholeSeconds.coerceAtLeast(0).toInt()),
                style = MaterialTheme.typography.displaySmall,
            )
        }
        val dark = bright == Duration.ZERO
        Text(
            text = stringResource(if (dark) Res.string.lights_on_too_dark else Res.string.lights_on_keep),
            style = MaterialTheme.typography.titleMedium,
            color = if (dark) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
