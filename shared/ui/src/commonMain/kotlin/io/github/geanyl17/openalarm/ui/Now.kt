package io.github.geanyl17.openalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** The current time, updated at every [tick] boundary (for example, on each full second). */
@Composable
fun rememberNow(tick: Duration): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(tick) {
        val tickMillis = tick.inWholeMilliseconds
        while (true) {
            delay(tickMillis - Clock.System.now().toEpochMilliseconds() % tickMillis)
            now = Clock.System.now()
        }
    }
    return now
}
