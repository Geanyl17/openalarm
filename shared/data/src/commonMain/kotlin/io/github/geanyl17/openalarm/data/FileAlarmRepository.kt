package io.github.geanyl17.openalarm.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Opens the alarms file at [path]. Keep one repository per file: two open at once in the same
 * process is an error. Its background work runs in [scope].
 */
fun openAlarmRepository(fileSystem: FileSystem, path: Path, scope: CoroutineScope): AlarmRepository {
    val store = DataStoreFactory.create(
        storage = OkioStorage(fileSystem, AlarmsFileSerializer) { path },
        corruptionHandler = ReplaceFileCorruptionHandler {
            // Keep the unreadable file so it can still be recovered by hand, then start empty.
            runCatching { fileSystem.copy(path, "$path.corrupt".toPath()) }
            AlarmsFile()
        },
        scope = scope,
    )
    return FileAlarmRepository(store)
}

private val byTimeOfDay = compareBy<Alarm>({ it.hour }, { it.minute }, { it.id })

internal class FileAlarmRepository(private val store: DataStore<AlarmsFile>) : AlarmRepository {

    override val alarms: Flow<List<Alarm>> = store.data.map { file -> file.alarms.sortedWith(byTimeOfDay) }

    override suspend fun get(id: Long): Alarm? = store.data.first().alarms.find { it.id == id }

    override suspend fun save(alarm: Alarm): Alarm {
        var saved = alarm
        store.updateData { file ->
            if (alarm.id == 0L) {
                saved = alarm.copy(id = file.nextId)
                file.copy(nextId = file.nextId + 1, alarms = file.alarms + saved)
            } else {
                file.copy(
                    nextId = maxOf(file.nextId, alarm.id + 1),
                    alarms = file.alarms.filterNot { it.id == alarm.id } + alarm,
                )
            }
        }
        return saved
    }

    override suspend fun delete(id: Long) {
        store.updateData { file -> file.copy(alarms = file.alarms.filterNot { it.id == id }) }
    }

    override suspend fun update(ids: Collection<Long>, transform: (Alarm) -> Alarm) {
        store.updateData { file ->
            file.copy(alarms = file.alarms.map { if (it.id in ids) transform(it) else it })
        }
    }
}
