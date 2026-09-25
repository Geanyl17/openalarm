package io.github.geanyl17.openalarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.geanyl17.openalarm.missions.MissionSensors
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The phone's sensors, for movement missions. */
class AndroidMissionSensors(context: Context) : MissionSensors {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    override val light: Flow<Float>? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor -> readings(sensor) { it[0] } }

    /** The sensor's readings while collected, turned into values by [value]. */
    private fun <T> readings(sensor: Sensor, value: (FloatArray) -> T): Flow<T> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(value(event.values))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
