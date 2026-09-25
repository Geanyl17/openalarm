package io.github.geanyl17.openalarm.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import io.github.geanyl17.openalarm.core.WakeLogRepository
import io.github.geanyl17.openalarm.core.WakeUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Opens the wake-up log at [path]. Keep one repository per file: two open at once in the same
 * process is an error. Its background work runs in [scope].
 */
fun openWakeLogRepository(fileSystem: FileSystem, path: Path, scope: CoroutineScope): WakeLogRepository {
    val store = DataStoreFactory.create(
        storage = OkioStorage(fileSystem, WakeLogFileSerializer) { path },
        corruptionHandler = ReplaceFileCorruptionHandler {
            // Keep the unreadable file so it can still be recovered by hand, then start empty.
            runCatching { fileSystem.copy(path, "$path.corrupt".toPath()) }
            WakeLogFile()
        },
        scope = scope,
    )
    return FileWakeLogRepository(store)
}

/** The JSON file the wake-up log is stored in. Bump [version] and add a migration when the format changes. */
@Serializable
internal data class WakeLogFile(
    val version: Int = CURRENT_VERSION,
    val wakeUps: List<WakeUp> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

internal class FileWakeLogRepository(private val store: DataStore<WakeLogFile>) : WakeLogRepository {
    override val wakeUps: Flow<List<WakeUp>> = store.data.map { it.wakeUps }

    override suspend fun update(transform: (List<WakeUp>) -> List<WakeUp>) {
        store.updateData { file -> file.copy(wakeUps = transform(file.wakeUps)) }
    }
}

internal object WakeLogFileSerializer : OkioSerializer<WakeLogFile> {
    // The same rules as the alarms file: an older app version can still read a newer file.
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val defaultValue = WakeLogFile()

    override suspend fun readFrom(source: BufferedSource): WakeLogFile =
        try {
            json.decodeFromString(WakeLogFile.serializer(), source.readUtf8())
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("The wake-up log can't be read", e)
        }

    override suspend fun writeTo(t: WakeLogFile, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(WakeLogFile.serializer(), t))
    }
}
