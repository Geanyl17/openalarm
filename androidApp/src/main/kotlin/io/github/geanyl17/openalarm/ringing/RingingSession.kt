package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.os.PowerManager
import io.github.geanyl17.openalarm.core.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Alarms that are ringing together. Usually one; several if they were set for the same time. */
data class Ringing(val alarms: List<Alarm>) {
    val first: Alarm get() = alarms.first()
    val label: String get() = alarms.firstNotNullOfOrNull { it.label.ifBlank { null } }.orEmpty()
}

/** What's ringing right now. [RingingService] updates it, and [RingingActivity] shows it. */
object RingingSession {
    private val current = MutableStateFlow<Ringing?>(null)
    val state: StateFlow<Ringing?> = current.asStateFlow()

    fun start(ringing: Ringing) {
        current.value = ringing
    }

    fun end() {
        current.value = null
    }
}

/**
 * Keeps the CPU awake between [io.github.geanyl17.openalarm.alarm.AlarmReceiver] returning and
 * [RingingService] taking its own wake lock. Times out on its own as a safety net.
 */
object StartupWakeLock {
    private const val TIMEOUT_MILLIS = 30_000L
    private var lock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        val wakeLock = lock ?: context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenAlarm:starting")
            .apply { setReferenceCounted(false) }
            .also { lock = it }
        wakeLock.acquire(TIMEOUT_MILLIS)
    }

    @Synchronized
    fun release() {
        lock?.takeIf { it.isHeld }?.release()
    }
}
