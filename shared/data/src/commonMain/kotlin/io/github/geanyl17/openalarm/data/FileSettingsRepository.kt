package io.github.geanyl17.openalarm.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import io.github.geanyl17.openalarm.core.Settings
import io.github.geanyl17.openalarm.core.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

/**
 * Opens the settings file at [path]. Keep one repository per file: two open at once in the same
 * process is an error. Its background work runs in [scope].
 */
fun openSettingsRepository(fileSystem: FileSystem, path: Path, scope: CoroutineScope): SettingsRepository {
    val store = DataStoreFactory.create(
        storage = OkioStorage(fileSystem, SettingsSerializer) { path },
        // Losing the settings only resets how the app looks, so an unreadable file just starts over.
        corruptionHandler = ReplaceFileCorruptionHandler { Settings() },
        scope = scope,
    )
    return FileSettingsRepository(store)
}

internal class FileSettingsRepository(private val store: DataStore<Settings>) : SettingsRepository {
    override val settings: Flow<Settings> = store.data

    override suspend fun update(transform: (Settings) -> Settings) {
        store.updateData(transform)
    }
}

internal object SettingsSerializer : OkioSerializer<Settings> {
    // The same rules as the alarms file: an older app version can still read a newer file.
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override val defaultValue = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings =
        try {
            json.decodeFromString(Settings.serializer(), source.readUtf8())
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("The settings file can't be read", e)
        }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        sink.writeUtf8(json.encodeToString(Settings.serializer(), t))
    }
}
