package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.geanyl17.openalarm.missions.resources.mission_alertness
import io.github.geanyl17.openalarm.missions.resources.mission_lights_on
import io.github.geanyl17.openalarm.missions.resources.mission_math
import io.github.geanyl17.openalarm.missions.resources.mission_memory
import io.github.geanyl17.openalarm.missions.resources.mission_nfc_tag
import io.github.geanyl17.openalarm.missions.resources.mission_progress
import io.github.geanyl17.openalarm.missions.resources.mission_reaction
import io.github.geanyl17.openalarm.missions.resources.mission_steps
import io.github.geanyl17.openalarm.missions.resources.mission_stroop
import io.github.geanyl17.openalarm.missions.resources.switch_to_math
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

private val OFFER_MATH_AFTER = 60.seconds

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
    val sensors = LocalMissionSensors.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    // Never trap anyone: a mission someone can't finish can be swapped for math problems.
    var switchedToMath by rememberSaveable(index) { mutableStateOf(false) }
    var offerMath by rememberSaveable(index) { mutableStateOf(false) }
    val mission = missions.getOrNull(index)
    if (mission == null) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }
    // A phone without the hardware a mission needs gets math instead. One that does gets the offer after
    // a while, for rooms without bright enough lights and the like.
    // With NFC switched off, there's no way to tap the tag. Opening the settings from the ringing screen would
    // only make it jump back in front, so it's math instead.
    val nfcOff = mission.type == MissionType.NfcTag && (!sensors.isNfcOn() || mission.tag == null)
    val useMath = switchedToMath || !sensors.canRun(mission.type) || nfcOff
    if (mission.type.usesHardware && !useMath) {
        LaunchedEffect(index) {
            delay(OFFER_MATH_AFTER)
            offerMath = true
        }
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
        key(index, useMath) {
            when (if (useMath) MissionType.Math else mission.type) {
                MissionType.Math -> MathMission(mission, onInteraction, onDone = { index++ })
                MissionType.Memory -> MemoryMission(
                    mission = mission,
                    onInteraction = onInteraction,
                    onDone = { index++ },
                    onSwitchToMath = { switchedToMath = true },
                )
                MissionType.Reaction -> ReactionMission(
                    mission = mission,
                    onInteraction = onInteraction,
                    onDone = { index++ },
                    onSwitchToMath = { switchedToMath = true },
                )
                MissionType.Stroop -> StroopMission(
                    mission = mission,
                    onInteraction = onInteraction,
                    onDone = { index++ },
                    onSwitchToMath = { switchedToMath = true },
                )
                MissionType.LightsOn -> LightsOnMission(mission, sensors.light!!, onInteraction, onDone = { index++ })
                MissionType.Steps -> StepsMission(mission, sensors.steps!!, onInteraction, onDone = { index++ })
                MissionType.NfcTag -> NfcMission(mission, sensors.nfcTags!!, onInteraction, onDone = { index++ })
                MissionType.Alertness -> AlertnessGateMission(
                    mission = mission,
                    baseline = LocalAlertnessBaseline.current ?: TYPICAL_REACTION,
                    onInteraction = onInteraction,
                    onDone = { index++ },
                    onSwitchToMath = { switchedToMath = true },
                )
            }
        }
        if (offerMath && !useMath) {
            TextButton(onClick = { onInteraction(); switchedToMath = true }) {
                Text(stringResource(Res.string.switch_to_math))
            }
        }
    }
}

@Composable
fun missionName(type: MissionType): String = stringResource(
    when (type) {
        MissionType.Math -> Res.string.mission_math
        MissionType.Memory -> Res.string.mission_memory
        MissionType.Reaction -> Res.string.mission_reaction
        MissionType.Stroop -> Res.string.mission_stroop
        MissionType.LightsOn -> Res.string.mission_lights_on
        MissionType.Steps -> Res.string.mission_steps
        MissionType.NfcTag -> Res.string.mission_nfc_tag
        MissionType.Alertness -> Res.string.mission_alertness
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
