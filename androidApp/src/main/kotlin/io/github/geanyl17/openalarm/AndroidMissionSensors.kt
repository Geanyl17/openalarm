package io.github.geanyl17.openalarm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import io.github.geanyl17.openalarm.missions.MissionSensors
import io.github.geanyl17.openalarm.missions.StepDetector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * The phone's sensors, for movement missions, on [activity]. [onRequestSteps] asks for the permission to count
 * steps; screens that can't ask leave it out.
 */
class AndroidMissionSensors(private val activity: Activity, private val onRequestSteps: (() -> Unit)? = null) : MissionSensors {
    private val context: Context = activity
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    override val light: Flow<Float>? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor -> readings(sensor) { it[0] } }

    /**
     * The phone's step sensor, which is accurate and barely uses the battery, needs the "physical activity"
     * permission. Without it, or on phones without one, steps are found in the accelerometer's readings.
     */
    override val steps: Flow<Unit>?
        get() {
            val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (stepSensor != null && canCountSteps(context)) return readings(stepSensor) { }
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
            val detector = StepDetector()
            return readings(accelerometer, SensorManager.SENSOR_DELAY_GAME) { it }
                .mapNotNull { (x, y, z, time) -> Unit.takeIf { detector.onAcceleration(x, y, z, time) } }
        }

    override fun requestSteps() {
        if (!canCountSteps(context)) onRequestSteps?.invoke()
    }

    /** Reader mode only works while [activity] is in front, and Android takes care of pausing it when it isn't. */
    override val nfcTags: Flow<String>? = nfc?.let { adapter ->
        callbackFlow {
            val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE
            adapter.enableReaderMode(activity, { tag -> trySend(tag.id.toHexString(HexFormat.UpperCase)) }, flags, null)
            awaitClose { adapter.disableReaderMode(activity) }
        }
    }

    override fun isNfcOn(): Boolean = nfc?.isEnabled == true

    override fun openNfcSettings() {
        activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }

    /** The sensor's readings while collected, turned into values by [value]. */
    private fun <T> readings(sensor: Sensor, rate: Int = SensorManager.SENSOR_DELAY_NORMAL, value: (Reading) -> T): Flow<T> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(value(Reading(event.values, event.timestamp)))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, rate)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    /** One sensor event, copied out, since Android reuses the event object. */
    private class Reading(values: FloatArray, val timeNanos: Long) {
        val values: FloatArray = values.copyOf()

        operator fun get(index: Int) = values[index]

        operator fun component1() = values[0]

        operator fun component2() = values[1]

        operator fun component3() = values[2]

        operator fun component4() = timeNanos
    }

    companion object {
        /** Whether the step sensors may be used. Before Android 10 they needed no permission. */
        fun canCountSteps(context: Context): Boolean =
            Build.VERSION.SDK_INT < 29 ||
                context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }
}
