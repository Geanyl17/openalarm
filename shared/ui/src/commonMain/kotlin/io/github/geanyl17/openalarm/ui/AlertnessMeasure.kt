package io.github.geanyl17.openalarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.geanyl17.openalarm.missions.AlertnessRun
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.alertness_measure_hint
import io.github.geanyl17.openalarm.ui.resources.alertness_measure_slips
import io.github.geanyl17.openalarm.ui.resources.alertness_measure_title
import io.github.geanyl17.openalarm.ui.resources.alertness_try_again
import io.github.geanyl17.openalarm.ui.resources.cancel
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** Measures the user's daytime reaction speed, which the Alertness Gate compares mornings to. */
@Composable
internal fun AlertnessMeasureDialog(onMeasured: (Duration) -> Unit, onDismiss: () -> Unit) {
    var run by remember { mutableIntStateOf(0) }
    var slipped by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(Res.string.alertness_measure_title), style = MaterialTheme.typography.headlineSmall)
                if (slipped) {
                    Text(stringResource(Res.string.alertness_measure_slips), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = {
                        slipped = false
                        run++
                    }) { Text(stringResource(Res.string.alertness_try_again)) }
                } else {
                    Text(stringResource(Res.string.alertness_measure_hint), style = MaterialTheme.typography.bodyMedium)
                    key(run) {
                        AlertnessRun(
                            onInteraction = {},
                            onFinished = { result ->
                                // A run with slips would make the mornings too easy to pass.
                                if (result.lapses <= 1 && result.falseStarts <= 2) onMeasured(result.median) else slipped = true
                            },
                        )
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(stringResource(Res.string.cancel)) }
            }
        }
    }
}
