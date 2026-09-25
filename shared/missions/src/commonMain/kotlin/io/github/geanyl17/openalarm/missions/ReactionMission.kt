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
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.reaction_go
import io.github.geanyl17.openalarm.missions.resources.reaction_in_time
import io.github.geanyl17.openalarm.missions.resources.reaction_progress
import io.github.geanyl17.openalarm.missions.resources.reaction_too_slow
import io.github.geanyl17.openalarm.missions.resources.reaction_too_soon
import io.github.geanyl17.openalarm.missions.resources.reaction_wait
import io.github.geanyl17.openalarm.missions.resources.switch_to_math
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val MIN_WAIT_MILLIS = 1_500L
private const val MAX_WAIT_MILLIS = 4_000L
private const val RESULT_MILLIS = 900L
private const val MISSES_BEFORE_EASIER = 3

private enum class Phase { Waiting, Go, TooSoon, TooSlow, InTime }

@Composable
internal fun ReactionMission(mission: Mission, onInteraction: () -> Unit, onDone: () -> Unit, onSwitchToMath: () -> Unit) {
    val limit = mission.difficulty.reactionLimit
    var round by rememberSaveable { mutableIntStateOf(1) }
    var misses by rememberSaveable { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.Waiting) }
    // Goes up by one after every tap, which starts the next wait.
    var attempt by remember { mutableIntStateOf(0) }
    var shownAt by remember { mutableStateOf<TimeMark?>(null) }
    var reaction by remember { mutableStateOf(Duration.ZERO) }

    fun finish(result: ReactionResult) {
        if (result == ReactionResult.InTime) {
            if (round == mission.rounds) {
                onDone()
                return
            }
            round++
        } else {
            misses++
            // Tapping away at random doesn't get anywhere: every miss costs a round.
            if (round > 1) round--
        }
        phase = when (result) {
            ReactionResult.TooSoon -> Phase.TooSoon
            ReactionResult.TooSlow -> Phase.TooSlow
            ReactionResult.InTime -> Phase.InTime
        }
        attempt++
    }

    LaunchedEffect(attempt) {
        // Leave the result of the last tap up for a moment.
        delay(RESULT_MILLIS)
        phase = Phase.Waiting
        shownAt = null
        delay(Random.nextLong(MIN_WAIT_MILLIS, MAX_WAIT_MILLIS))
        phase = Phase.Go
        // Start timing on the frame that shows "Tap!", not when it's asked for.
        withFrameMillis { }
        shownAt = TimeSource.Monotonic.markNow()
        delay(limit)
        finish(ReactionResult.TooSlow)
    }

    val onTap by rememberUpdatedState {
        if (phase == Phase.Waiting || phase == Phase.Go) {
            onInteraction()
            val sinceGo = shownAt?.elapsedNow()
            reaction = sinceGo ?: Duration.ZERO
            finish(judgeReaction(sinceGo, limit))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (mission.rounds > 1) {
            Text(
                text = stringResource(Res.string.reaction_progress, round, mission.rounds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val colors = MaterialTheme.colorScheme
        val targetColor = when (phase) {
            Phase.Waiting -> colors.surfaceContainerHighest
            Phase.Go -> colors.primary
            Phase.InTime -> colors.primaryContainer
            Phase.TooSoon, Phase.TooSlow -> colors.errorContainer
        }
        val color by animateColorAsState(
            targetValue = targetColor,
            // "Tap!" shows up at once, so the time it takes to fade in doesn't count against anyone.
            animationSpec = tween(durationMillis = if (phase == Phase.Go) 0 else 150),
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = color,
            contentColor = colors.contentColorFor(targetColor),
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .height(280.dp)
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
                        Phase.Waiting -> stringResource(Res.string.reaction_wait)
                        Phase.Go -> stringResource(Res.string.reaction_go)
                        Phase.InTime -> stringResource(Res.string.reaction_in_time, reaction.inWholeMilliseconds)
                        Phase.TooSoon -> stringResource(Res.string.reaction_too_soon)
                        Phase.TooSlow -> stringResource(Res.string.reaction_too_slow)
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        if (misses >= MISSES_BEFORE_EASIER) {
            TextButton(onClick = { onInteraction(); onSwitchToMath() }) {
                Text(stringResource(Res.string.switch_to_math))
            }
        }
    }
}
