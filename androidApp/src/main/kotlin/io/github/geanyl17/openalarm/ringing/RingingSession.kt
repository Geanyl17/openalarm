package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.os.PowerManager
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Mission
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Alarms that are ringing together. Usually one; several if they were set for the same time. */
data class Ringing(val alarms: List<Alarm>) {
    val first: Alarm get() = alarms.first()
    val label: String get() = alarms.firstNotNullOfOrNull { it.label.ifBlank { null } }.orEmpty()

    /** Every ringing alarm's missions, all of which must be done to turn them off. */
    val missions: List<Mission> get() = alarms.flatMap { it.missions }
}

/** What's ringing right now. [RingingService] updates it, and [RingingActivity] shows it. */
object RingingSession {
    private val current = MutableStateFlow<Ringing?>(null)
    val state: StateFlow<Ringing?> = current.asStateFlow()

    private val interactions = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits each time the user taps something in a mission. */
    val missionInteractions: SharedFlow<Unit> = interactions.asSharedFlow()

    fun start(ringing: Ringing) {
        current.value = ringing
    }

    fun end() {
        current.value = null
    }

    fun missionInteraction() {
        interactions.tryEmit(Unit)
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
