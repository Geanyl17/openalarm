package io.github.geanyl17.openalarm.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import io.github.geanyl17.openalarm.core.Alarm
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/** The JSON file all alarms are stored in. Bump [version] and add a migration when the format changes. */
@Serializable
internal data class AlarmsFile(
    val version: Int = CURRENT_VERSION,
    val nextId: Long = 1,
    val alarms: List<Alarm> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

internal object AlarmsFileSerializer : OkioSerializer<AlarmsFile> {
    // Defaults are written out so that changing a default later can't silently change existing alarms.
    // Unknown keys are ignored so an older app version can still read a newer file.
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override val defaultValue = AlarmsFile()

    override suspend fun readFrom(source: BufferedSource): AlarmsFile =
        try {
            json.decodeFromString(AlarmsFile.serializer(), source.readUtf8())
        } catch (e: IllegalArgumentException) {
            // Covers malformed JSON (SerializationException) and out-of-range values rejected by Alarm.
            throw CorruptionException("The alarms file can't be read", e)
        }

    override suspend fun writeTo(t: AlarmsFile, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(AlarmsFile.serializer(), t))
    }
}
