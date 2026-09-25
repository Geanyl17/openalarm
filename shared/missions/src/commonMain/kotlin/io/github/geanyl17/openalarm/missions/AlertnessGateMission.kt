package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.alertness_result
import io.github.geanyl17.openalarm.missions.resources.alertness_slower
import io.github.geanyl17.openalarm.missions.resources.alertness_try_again
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/**
 * Passes once the user reacts about as fast as they do during the day ([baseline]). After [ALERTNESS_TRIES]
 * failed tries, it gives way to math.
 */
@Composable
internal fun AlertnessGateMission(
    mission: Mission,
    baseline: Duration,
    onInteraction: () -> Unit,
    onDone: () -> Unit,
    onSwitchToMath: () -> Unit,
) {
    var failed by rememberSaveable { mutableIntStateOf(0) }
    var lastResult by remember { mutableStateOf<AlertnessResult?>(null) }
    val result = lastResult
    if (result == null) {
        key(failed) {
            AlertnessRun(
                onInteraction = onInteraction,
                onFinished = {
                    when {
                        it.passes(baseline, mission.difficulty) -> onDone()
                        failed + 1 >= ALERTNESS_TRIES -> onSwitchToMath()
                        else -> {
                            failed++
                            lastResult = it
                        }
                    }
                },
            )
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.alertness_slower), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(Res.string.alertness_result, result.median.inWholeMilliseconds, baseline.inWholeMilliseconds),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                onInteraction()
                lastResult = null
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.alertness_try_again))
        }
    }
}
