package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.ic_backspace
import io.github.geanyl17.openalarm.missions.resources.ic_check
import io.github.geanyl17.openalarm.missions.resources.math_check
import io.github.geanyl17.openalarm.missions.resources.math_delete
import io.github.geanyl17.openalarm.missions.resources.math_progress
import io.github.geanyl17.openalarm.missions.resources.math_title
import io.github.geanyl17.openalarm.missions.resources.math_wrong
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val MAX_DIGITS = 5

@Composable
internal fun MathMission(mission: Mission, onInteraction: () -> Unit, onDone: () -> Unit) {
    val problems = remember { MathProblems() }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var problem by remember { mutableStateOf(problems.next(mission.difficulty)) }
    var answer by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.math_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = stringResource(Res.string.math_progress, round, mission.rounds),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${problem.question} =",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
        )
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (wrong) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.widthIn(min = 160.dp),
        ) {
            Text(
                text = answer.ifEmpty { "?" },
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        // Always takes up a line, so the keypad doesn't jump when the message appears.
        Text(
            text = if (wrong) stringResource(Res.string.math_wrong) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        NumberPad(
            onDigit = { digit ->
                onInteraction()
                wrong = false
                if (answer.length < MAX_DIGITS) answer += digit
            },
            onDelete = {
                onInteraction()
                answer = answer.dropLast(1)
            },
            onCheck = {
                onInteraction()
                if (answer.toIntOrNull() == problem.answer) {
                    wrong = false
                    if (round == mission.rounds) {
                        onDone()
                    } else {
                        round++
                        problem = problems.next(mission.difficulty)
                    }
                } else {
                    wrong = true
                    // A new problem after each miss, so answers can't be found by trying them one by one.
                    problem = problems.next(mission.difficulty)
                }
                answer = ""
            },
        )
    }
}

@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onCheck: () -> Unit) {
    Column(Modifier.widthIn(max = 360.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf("123", "456", "789")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in row) {
                    PadButton(onClick = { onDigit(digit) }) {
                        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadButton(onClick = onDelete) {
                Icon(painterResource(Res.drawable.ic_backspace), contentDescription = stringResource(Res.string.math_delete))
            }
            PadButton(onClick = { onDigit('0') }) {
                Text("0", style = MaterialTheme.typography.headlineSmall)
            }
            PadButton(onClick = onCheck, primary = true) {
                Icon(painterResource(Res.drawable.ic_check), contentDescription = stringResource(Res.string.math_check))
            }
        }
    }
}

@Composable
private fun RowScope.PadButton(onClick: () -> Unit, primary: Boolean = false, content: @Composable RowScope.() -> Unit) {
    val modifier = Modifier.weight(1f).height(64.dp)
    if (primary) {
        Button(onClick = onClick, modifier = modifier, content = content)
    } else {
        FilledTonalButton(onClick = onClick, modifier = modifier, content = content)
    }
}
