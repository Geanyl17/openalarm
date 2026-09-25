package io.github.geanyl17.openalarm.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.github.geanyl17.openalarm.missions.LocalMissionSensors
import io.github.geanyl17.openalarm.ui.resources.Res
import io.github.geanyl17.openalarm.ui.resources.cancel
import io.github.geanyl17.openalarm.ui.resources.nfc_off
import io.github.geanyl17.openalarm.ui.resources.nfc_scan_hint
import io.github.geanyl17.openalarm.ui.resources.nfc_scan_title
import io.github.geanyl17.openalarm.ui.resources.nfc_turn_on
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/** Waits for an NFC tag to be held to the phone, and passes on its ID. */
@Composable
internal fun NfcTagScanDialog(onScanned: (String) -> Unit, onDismiss: () -> Unit) {
    val sensors = LocalMissionSensors.current
    var nfcOn by remember { mutableStateOf(sensors.isNfcOn()) }
    val scanned by rememberUpdatedState(onScanned)
    // The user may switch NFC on in the settings and come back.
    LaunchedEffect(Unit) {
        while (true) {
            nfcOn = sensors.isNfcOn()
            delay(1.seconds)
        }
    }
    LaunchedEffect(Unit) { sensors.nfcTags?.first()?.let(scanned) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nfc_scan_title)) },
        text = { Text(stringResource(if (nfcOn) Res.string.nfc_scan_hint else Res.string.nfc_off)) },
        confirmButton = {
            if (!nfcOn) TextButton(onClick = sensors::openNfcSettings) { Text(stringResource(Res.string.nfc_turn_on)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
