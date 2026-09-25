package io.github.geanyl17.openalarm.data

import io.github.geanyl17.openalarm.core.Escape
import io.github.geanyl17.openalarm.core.WakeUp
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

class FileWakeLogRepositoryTest {
    private val fileSystem = FakeFileSystem()

    @Test
    fun wakeUpsSurviveReopeningTheFile() = runTest {
        val path = "/reopen.json".toPath()
        val wakeUp = WakeUp(alarmId = 1, label = "Work", rangAt = 1_000, offAt = 61_000, snoozes = 2, escapes = listOf(Escape.PhoneOff))
        val job = Job()
        val first = openWakeLogRepository(fileSystem, path, CoroutineScope(StandardTestDispatcher(testScheduler) + job))
        first.update { it + wakeUp }
        job.cancelAndJoin()

        val second = openWakeLogRepository(fileSystem, path, backgroundScope)
        assertEquals(listOf(wakeUp), second.wakeUps.first())
    }

    @Test
    fun anUnreadableFileIsKeptAsideAndTheLogStartsOver() = runTest {
        val path = "/broken.json".toPath()
        fileSystem.write(path) { writeUtf8("{not json") }
        val repository = openWakeLogRepository(fileSystem, path, backgroundScope)
        assertEquals(emptyList(), repository.wakeUps.first())
        assertEquals("{not json", fileSystem.read("/broken.json.corrupt".toPath()) { readUtf8() })
    }
}
