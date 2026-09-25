package io.github.geanyl17.openalarm.missions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.memory_progress
import io.github.geanyl17.openalarm.missions.resources.memory_repeat
import io.github.geanyl17.openalarm.missions.resources.memory_switch_to_math
import io.github.geanyl17.openalarm.missions.resources.memory_tile
import io.github.geanyl17.openalarm.missions.resources.memory_watch
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val LIT_MILLIS = 550L
private const val GAP_MILLIS = 200L
private const val TAP_FLASH_MILLIS = 180L
private const val MISSES_BEFORE_EASIER = 3

@Composable
internal fun MemoryMission(mission: Mission, onInteraction: () -> Unit, onDone: () -> Unit, onSwitchToMath: () -> Unit) {
    val patterns = remember { MemoryPatterns() }
    var round by rememberSaveable { mutableIntStateOf(1) }
    var misses by rememberSaveable { mutableIntStateOf(0) }
    var length by rememberSaveable { mutableIntStateOf(mission.difficulty.patternLength) }
    // Goes up by one for every new pattern, which (re)starts playing it.
    var attempt by remember { mutableIntStateOf(0) }
    var pattern by remember { mutableStateOf(patterns.next(length)) }
    var progress by remember { mutableIntStateOf(0) }
    var watching by remember { mutableStateOf(true) }
    var lit by remember { mutableStateOf<Int?>(null) }
    var wrongTile by remember { mutableStateOf<Int?>(null) }
    // The tile the user just tapped, with a counter so tapping the same tile again flashes again.
    var tapped by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun startNewPattern() {
        pattern = patterns.next(length)
        progress = 0
        attempt++
    }

    LaunchedEffect(attempt) {
        watching = true
        // After a miss, leave the wrong tile red for a moment before playing the next pattern.
        delay(if (wrongTile == null) 600 else 1_000)
        wrongTile = null
        for (tile in pattern) {
            lit = tile
            delay(LIT_MILLIS)
            lit = null
            delay(GAP_MILLIS)
        }
        watching = false
    }
    LaunchedEffect(tapped) {
        if (tapped != null) {
            delay(TAP_FLASH_MILLIS)
            tapped = null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (watching) Res.string.memory_watch else Res.string.memory_repeat),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (mission.rounds > 1) {
            Text(
                text = stringResource(Res.string.memory_progress, round, mission.rounds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TileGrid(
            lit = lit ?: tapped?.first,
            wrong = wrongTile,
            enabled = !watching,
            onTap = { tile ->
                onInteraction()
                if (pattern[progress] == tile) {
                    progress++
                    tapped = tile to progress
                    if (progress == pattern.size) {
                        if (round == mission.rounds) {
                            onDone()
                        } else {
                            round++
                            startNewPattern()
                        }
                    }
                } else {
                    misses++
                    wrongTile = tile
                    // Repeated misses make the patterns a little shorter, so nobody gets stuck.
                    if (misses % MISSES_BEFORE_EASIER == 0 && length > MemoryPatterns.MIN_LENGTH) length--
                    startNewPattern()
                }
            },
        )
        if (misses >= MISSES_BEFORE_EASIER) {
            TextButton(onClick = { onInteraction(); onSwitchToMath() }) {
                Text(stringResource(Res.string.memory_switch_to_math))
            }
        }
    }
}

@Composable
private fun TileGrid(lit: Int?, wrong: Int?, enabled: Boolean, onTap: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (column in 0 until 3) {
                    val tile = row * 3 + column
                    val color by animateColorAsState(
                        targetValue = when (tile) {
                            wrong -> MaterialTheme.colorScheme.error
                            lit -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        animationSpec = tween(durationMillis = 120),
                    )
                    val description = stringResource(Res.string.memory_tile, tile + 1)
                    Box(
                        Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(color)
                            .clickable(enabled = enabled) { onTap(tile) }
                            .semantics { contentDescription = description },
                    )
                }
            }
        }
    }
}
