package io.github.geanyl17.openalarm.data

import io.github.geanyl17.openalarm.core.Alarm
import io.github.geanyl17.openalarm.core.RepeatDays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileAlarmRepositoryTest {
    private val fileSystem = FakeFileSystem()

    @Test
    fun alarmsSurviveReopeningTheFile() = runTest {
        val path = "/reopen.json".toPath()
        val job = Job()
        val first = openAlarmRepository(fileSystem, path, CoroutineScope(StandardTestDispatcher(testScheduler) + job))
        val saved = first.save(Alarm(hour = 7, minute = 30, label = "Work", repeat = RepeatDays.WorkDays))
        job.cancelAndJoin()

        val second = openAlarmRepository(fileSystem, path, backgroundScope)
        assertEquals(listOf(saved), second.alarms.first())
    }

    @Test
    fun newAlarmsGetIncreasingIdsAndAreSortedByTime() = runTest {
        val repository = openAlarmRepository(fileSystem, "/ids.json".toPath(), backgroundScope)
        val late = repository.save(Alarm(hour = 9, minute = 0))
        val early = repository.save(Alarm(hour = 6, minute = 45))
        assertEquals(listOf(1L, 2L), listOf(late.id, early.id))
        assertEquals(listOf(early, late), repository.alarms.first())
    }

    @Test
    fun updateChangesOnlyTheGivenAlarms() = runTest {
        val repository = openAlarmRepository(fileSystem, "/update.json".toPath(), backgroundScope)
        val a = repository.save(Alarm(hour = 7, minute = 0))
        val b = repository.save(Alarm(hour = 8, minute = 0))
        repository.update(listOf(a.id)) { it.copy(enabled = false) }
        assertEquals(false, repository.get(a.id)?.enabled)
        assertEquals(true, repository.get(b.id)?.enabled)
    }

    @Test
    fun deleteRemovesTheAlarm() = runTest {
        val repository = openAlarmRepository(fileSystem, "/delete.json".toPath(), backgroundScope)
        val alarm = repository.save(Alarm(hour = 7, minute = 0))
        repository.delete(alarm.id)
        assertNull(repository.get(alarm.id))
    }

    @Test
    fun fileFormatStoresRepeatDaysAsAPlainNumber() = runTest {
        val path = "/format.json".toPath()
        val repository = openAlarmRepository(fileSystem, path, backgroundScope)
        repository.save(Alarm(hour = 7, minute = 0, repeat = RepeatDays.WorkDays))
        val json = fileSystem.read(path) { readUtf8() }
        assertTrue("\"version\":1" in json, json)
        assertTrue("\"repeat\":31" in json, json)
    }

    @Test
    fun unknownFieldsFromANewerVersionAreIgnored() = runTest {
        val path = "/newer.json".toPath()
        fileSystem.write(path) {
            writeUtf8("""{"version":2,"nextId":2,"alarms":[{"id":1,"hour":6,"minute":15,"mission":"math"}],"theme":"red"}""")
        }
        val repository = openAlarmRepository(fileSystem, path, backgroundScope)
        assertEquals(listOf(Alarm(id = 1, hour = 6, minute = 15)), repository.alarms.first())
    }

    @Test
    fun unreadableFileIsBackedUpAndReplaced() = runTest {
        val path = "/corrupt.json".toPath()
        fileSystem.write(path) { writeUtf8("{ this is not json") }
        val repository = openAlarmRepository(fileSystem, path, backgroundScope)
        assertTrue(repository.alarms.first().isEmpty())
        assertEquals("{ this is not json", fileSystem.read("/corrupt.json.corrupt".toPath()) { readUtf8() })
    }
}
