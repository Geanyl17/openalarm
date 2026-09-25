package io.github.geanyl17.openalarm.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import io.github.geanyl17.openalarm.missions.resources.nfc_title
import io.github.geanyl17.openalarm.missions.resources.nfc_wrong_tag
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

/** Waits for the tag registered with [mission], which is somewhere away from the bed. */
@Composable
internal fun NfcMission(mission: Mission, tags: Flow<String>, onInteraction: () -> Unit, onDone: () -> Unit) {
    var wrongTag by remember { mutableStateOf(false) }
    val interact by rememberUpdatedState(onInteraction)
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(tags) {
        tags.collect { tag ->
            interact()
            wrongTag = tag != mission.tag
            if (!wrongTag) done()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(Res.string.nfc_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        // Always takes up a line, so the layout doesn't jump when the message appears.
        Text(
            text = if (wrongTag) stringResource(Res.string.nfc_wrong_tag) else "",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
