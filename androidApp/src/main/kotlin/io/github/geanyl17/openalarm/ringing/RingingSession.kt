package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.Mission
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.minutes

/** Alarms that are ringing together. Usually one; several if they were set for the same time. */
data class Ringing(val alarms: List<Alarm>) {
    val first: Alarm get() = alarms.first()
    val label: String get() = alarms.firstNotNullOfOrNull { it.label.ifBlank { null } }.orEmpty()

    /** Every ringing alarm's missions, all of which must be done to turn them off. */
    val missions: List<Mission> get() = alarms.flatMap { it.missions }

    val photo: String? get() = alarms.firstNotNullOfOrNull { it.photo }
}

/** What's ringing right now. [RingingService] updates it, and [RingingActivity] shows it. */
object RingingSession {
    private val current = MutableStateFlow<Ringing?>(null)
    val state: StateFlow<Ringing?> = current.asStateFlow()

    private val interactions = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits each time the user taps something in a mission. */
    val missionInteractions: SharedFlow<Unit> = interactions.asSharedFlow()

    private val screenShown = MutableStateFlow(false)

    /** Whether [RingingActivity] is on screen. While an alarm rings, [RingingService] brings it back when it isn't. */
    val screenVisible: StateFlow<Boolean> = screenShown.asStateFlow()

    private var emergencyCallUntil = 0L

    /** True for a while after the user opened the emergency dialer from the ringing screen. */
    val onEmergencyCall: Boolean get() = SystemClock.elapsedRealtime() < emergencyCallUntil

    fun start(ringing: Ringing) {
        current.value = ringing
    }

    fun end() {
        current.value = null
        emergencyCallUntil = 0
    }

    fun missionInteraction() {
        interactions.tryEmit(Unit)
    }

    fun setScreenVisible(visible: Boolean) {
        screenShown.value = visible
        // Back on the ringing screen, the emergency call is over, or never happened. A call that's still going
        // keeps the alarm silent anyway.
        if (visible) emergencyCallUntil = 0
    }

    /** The user is calling emergency services: the alarm stays silent and lets them use the phone for a while. */
    fun emergencyCall() {
        emergencyCallUntil = SystemClock.elapsedRealtime() + EMERGENCY_CALL_HOLD.inWholeMilliseconds
    }

    private val EMERGENCY_CALL_HOLD = 3.minutes
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
