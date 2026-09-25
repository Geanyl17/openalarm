package io.github.geanyl17.openalarm.missions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.alertness_progress
import io.github.geanyl17.openalarm.missions.resources.alertness_title
import io.github.geanyl17.openalarm.missions.resources.reaction_too_soon
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val MIN_WAIT_MILLIS = 2_000L
private const val MAX_WAIT_MILLIS = 5_000L
private const val RESULT_MILLIS = 1_000L

private enum class RunPhase { Waiting, Counting, Result, FalseStart }

/**
 * Runs the alertness test once: [ALERTNESS_TRIALS] times, a counter starts at a random moment and the user
 * taps to stop it. [onInteraction] is called on every tap, and [onFinished] with the result.
 */
@Composable
fun AlertnessRun(onInteraction: () -> Unit, onFinished: (AlertnessResult) -> Unit, modifier: Modifier = Modifier) {
    val reactions = remember { mutableStateListOf<Duration>() }
    var falseStarts by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(RunPhase.Waiting) }
    // Goes up by one after every tap, which starts the next wait.
    var attempt by remember { mutableIntStateOf(0) }
    var startedAt by remember { mutableStateOf<TimeMark?>(null) }
    var counter by remember { mutableStateOf(Duration.ZERO) }
    val finished by rememberUpdatedState(onFinished)

    fun record(reaction: Duration) {
        reactions += reaction
        counter = reaction
        phase = RunPhase.Result
        attempt++
    }

    LaunchedEffect(attempt) {
        // Leave the last reaction time, or "Too soon", up for a moment.
        if (attempt > 0) delay(RESULT_MILLIS)
        if (reactions.size == ALERTNESS_TRIALS) {
            finished(AlertnessResult(reactions.toList(), falseStarts))
            return@LaunchedEffect
        }
        phase = RunPhase.Waiting
        startedAt = null
        delay(Random.nextLong(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS))
        phase = RunPhase.Counting
        // Start timing on the frame that shows the counter, not when it's asked for.
        withFrameMillis { }
        val mark = TimeSource.Monotonic.markNow()
        startedAt = mark
        while (true) {
            withFrameMillis { }
            counter = mark.elapsedNow()
            if (counter >= NO_RESPONSE) {
                record(NO_RESPONSE)
                break
            }
        }
    }

    val onTap by rememberUpdatedState {
        when (phase) {
            RunPhase.Waiting -> {
                onInteraction()
                falseStarts++
                phase = RunPhase.FalseStart
                attempt++
            }
            RunPhase.Counting -> {
                onInteraction()
                // Tapped in the moment between showing the counter and timing it: that's too soon too.
                startedAt?.let { record(it.elapsedNow()) } ?: run {
                    falseStarts++
                    phase = RunPhase.FalseStart
                    attempt++
                }
            }
            RunPhase.Result, RunPhase.FalseStart -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.alertness_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = stringResource(Res.string.alertness_progress, (reactions.size + 1).coerceAtMost(ALERTNESS_TRIALS), ALERTNESS_TRIALS),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val colors = MaterialTheme.colorScheme
        val target = when (phase) {
            RunPhase.Waiting -> colors.surfaceContainerHighest
            RunPhase.Counting -> colors.primary
            RunPhase.Result -> colors.primaryContainer
            RunPhase.FalseStart -> colors.errorContainer
        }
        // The counter shows up at once, so the time it takes to fade in doesn't count against anyone.
        val color by animateColorAsState(target, animationSpec = tween(durationMillis = if (phase == RunPhase.Counting) 0 else 150))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = color,
            contentColor = colors.contentColorFor(target),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(240.dp)
                // Reacts to the finger touching down, which is when the reaction happened.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onTap()
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = when (phase) {
                        RunPhase.Waiting -> ""
                        RunPhase.Counting, RunPhase.Result -> counter.inWholeMilliseconds.toString()
                        RunPhase.FalseStart -> stringResource(Res.string.reaction_too_soon)
                    },
                    style = MaterialTheme.typography.displayMedium,
                )
            }
        }
    }
}
