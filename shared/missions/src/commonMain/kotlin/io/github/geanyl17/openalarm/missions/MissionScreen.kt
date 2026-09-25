package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.core.MissionType
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.difficulty_easy
import io.github.geanyl17.openalarm.missions.resources.difficulty_hard
import io.github.geanyl17.openalarm.missions.resources.difficulty_normal
import io.github.geanyl17.openalarm.missions.resources.mission_math
import io.github.geanyl17.openalarm.missions.resources.mission_memory
import io.github.geanyl17.openalarm.missions.resources.mission_progress
import org.jetbrains.compose.resources.stringResource

/**
 * Runs [missions] one after another, then calls [onComplete]. [onInteraction] is called on every
 * tap, which keeps the alarm quiet while the user is working on it.
 */
@Composable
fun MissionScreen(
    missions: List<Mission>,
    onInteraction: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    // Never trap anyone: a mission someone can't finish can be swapped for math problems.
    var switchedToMath by rememberSaveable(index) { mutableStateOf(false) }
    val mission = missions.getOrNull(index)
    if (mission == null) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (missions.size > 1) {
            Text(
                text = stringResource(Res.string.mission_progress, index + 1, missions.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        key(index, switchedToMath) {
            when (if (switchedToMath) MissionType.Math else mission.type) {
                MissionType.Math -> MathMission(mission, onInteraction, onDone = { index++ })
                MissionType.Memory -> MemoryMission(
                    mission = mission,
                    onInteraction = onInteraction,
                    onDone = { index++ },
                    onSwitchToMath = { switchedToMath = true },
                )
            }
        }
    }
}

@Composable
fun missionName(type: MissionType): String = stringResource(
    when (type) {
        MissionType.Math -> Res.string.mission_math
        MissionType.Memory -> Res.string.mission_memory
    },
)

@Composable
fun difficultyName(difficulty: Difficulty): String = stringResource(
    when (difficulty) {
        Difficulty.Easy -> Res.string.difficulty_easy
        Difficulty.Normal -> Res.string.difficulty_normal
        Difficulty.Hard -> Res.string.difficulty_hard
    },
)
