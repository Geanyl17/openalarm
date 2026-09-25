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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.geanyl17.openalarm.core.Mission
import io.github.geanyl17.openalarm.missions.resources.Res
import io.github.geanyl17.openalarm.missions.resources.steps_count
import io.github.geanyl17.openalarm.missions.resources.steps_title
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StepsMission(mission: Mission, steps: Flow<Unit>, onInteraction: () -> Unit, onDone: () -> Unit) {
    val target = mission.stepsTarget
    var taken by rememberSaveable { mutableIntStateOf(0) }
    val interact by rememberUpdatedState(onInteraction)
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(steps) {
        steps.collect {
            // While you walk, the alarm stays quiet.
            interact()
            taken++
            if (taken >= target) done()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.steps_title), style = MaterialTheme.typography.titleLarge)
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { taken.toFloat() / target },
                strokeWidth = 12.dp,
                modifier = Modifier.size(200.dp),
            )
            Text(stringResource(Res.string.steps_count, taken, target), style = MaterialTheme.typography.displaySmall)
        }
    }
}
