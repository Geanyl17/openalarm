package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.edit
import io.github.geanyl17.openalarm.AndroidMissionSensors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Skips check-ins for someone who's clearly up: the phone's step counter is read when an alarm is turned
 * off and again when a check-in is due. It needs a step counter and the permission to use it.
 */
internal object WalkingCheck {
    private const val PREFERENCES = "walking"
    private const val KEY_STEPS = "steps_at_turn_off"

    /** More than a trip to the bathroom and back to bed. */
    private const val WALKING_STEPS = 100

    /** Notes the step count as an alarm with check-ins is turned off. */
    suspend fun rememberSteps(context: Context) {
        val steps = stepCount(context)
        preferences(context).edit { if (steps != null) putFloat(KEY_STEPS, steps) else remove(KEY_STEPS) }
    }

    /** Whether the user has walked around since the alarm was turned off. */
    suspend fun walkedSinceTurnOff(context: Context): Boolean {
        val before = preferences(context).getFloat(KEY_STEPS, -1f).takeIf { it >= 0 } ?: return false
        val now = stepCount(context) ?: return false
        // The counter starts over after a reboot, which makes the difference negative.
        return now - before >= WALKING_STEPS
    }

    /** The step counter's total, which counts from the last reboot. The sensor reports it as soon as it's switched on. */
    private suspend fun stepCount(context: Context): Float? {
        if (!AndroidMissionSensors.canCountSteps(context)) return null
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
        return withTimeoutOrNull(2.seconds) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) continuation.resume(event.values[0])
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                continuation.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }

    // Device-protected, so a check-in before the first unlock after a reboot can read it.
    private fun preferences(context: Context): SharedPreferences =
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
