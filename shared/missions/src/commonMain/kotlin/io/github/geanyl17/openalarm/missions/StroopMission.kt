package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.ink_blue
import io.github.geanyl17.openalarm.missions.resources.ink_green
import io.github.geanyl17.openalarm.missions.resources.ink_orange
import io.github.geanyl17.openalarm.missions.resources.ink_pink
import io.github.geanyl17.openalarm.missions.resources.math_wrong
import io.github.geanyl17.openalarm.missions.resources.stroop_progress
import io.github.geanyl17.openalarm.missions.resources.stroop_prompt
import io.github.geanyl17.openalarm.missions.resources.switch_to_math
import org.jetbrains.compose.resources.stringResource

private const val MISSES_BEFORE_EASIER = 3

// Every ink is easy to read on black, in light and dark theme alike.
private val Board = Color.Black
private val BoardBorder = Color(0xFF5E5E5E)

@Composable
internal fun StroopMission(mission: Mission, onInteraction: () -> Unit, onDone: () -> Unit, onSwitchToMath: () -> Unit) {
    val rounds = remember { StroopRounds() }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var misses by rememberSaveable { mutableIntStateOf(0) }
    var wrong by rememberSaveable { mutableStateOf(false) }
    var current by remember { mutableStateOf(rounds.next(mission.difficulty)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (mission.rounds > 1) {
            Text(
                text = stringResource(Res.string.stroop_progress, round, mission.rounds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(Res.string.stroop_prompt),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Board,
            contentColor = Color.White,
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = inkName(current.word).uppercase(),
                    color = Color(current.ink.argb),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
                current.choices.withIndex().chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (index, choice) ->
                            OutlinedButton(
                                onClick = {
                                    onInteraction()
                                    if (choice == current.ink) {
                                        wrong = false
                                        if (round == mission.rounds) {
                                            onDone()
                                            return@OutlinedButton
                                        }
                                        round++
                                    } else {
                                        wrong = true
                                        misses++
                                        // Guessing doesn't get anywhere: every miss costs a round.
                                        if (round > 1) round--
                                    }
                                    current = rounds.next(mission.difficulty)
                                },
                                border = BorderStroke(1.dp, BoardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = current.choiceInks?.get(index)?.let { Color(it.argb) } ?: Color.White,
                                ),
                                modifier = Modifier.weight(1f).height(64.dp),
                            ) {
                                Text(inkName(choice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        // Always takes up a line, so the buttons don't jump when the message appears.
        Text(
            text = if (wrong) stringResource(Res.string.math_wrong) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        if (misses >= MISSES_BEFORE_EASIER) {
            TextButton(onClick = { onInteraction(); onSwitchToMath() }) {
                Text(stringResource(Res.string.switch_to_math))
            }
        }
    }
}

@Composable
private fun inkName(ink: Ink): String = stringResource(
    when (ink) {
        Ink.Blue -> Res.string.ink_blue
        Ink.Orange -> Res.string.ink_orange
        Ink.Pink -> Res.string.ink_pink
        Ink.Green -> Res.string.ink_green
    },
)
