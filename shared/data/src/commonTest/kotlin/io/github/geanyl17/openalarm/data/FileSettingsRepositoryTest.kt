package io.github.geanyl17.openalarm.data

import io.github.geanyl17.openalarm.core.Settings
import io.github.geanyl17.openalarm.core.ThemeMode
import io.github.geanyl17.openalarm.core.ThemeSettings
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

class FileSettingsRepositoryTest {
    private val fileSystem = FakeFileSystem()

    @Test
    fun startsWithTheDefaults() = runTest {
        val repository = openSettingsRepository(fileSystem, "/new.json".toPath(), backgroundScope)
        assertEquals(Settings(), repository.settings.first())
    }

    @Test
    fun themeSurvivesReopeningTheFile() = runTest {
        val path = "/reopen.json".toPath()
        val theme = ThemeSettings(colorArgb = 0xFF1976D2.toInt(), mode = ThemeMode.NightRed)
        val job = Job()
        val first = openSettingsRepository(fileSystem, path, CoroutineScope(StandardTestDispatcher(testScheduler) + job))
        first.update { it.copy(theme = theme) }
        job.cancelAndJoin()

        val second = openSettingsRepository(fileSystem, path, backgroundScope)
        assertEquals(theme, second.settings.first().theme)
    }

    @Test
    fun unknownModesFromNewerVersionsFallBackToTheDefault() = runTest {
        val path = "/newer.json".toPath()
        fileSystem.write(path) { writeUtf8("""{"theme":{"colorArgb":-15108398,"mode":"sepia","font":"big"}}""") }
        val repository = openSettingsRepository(fileSystem, path, backgroundScope)
        assertEquals(ThemeSettings(colorArgb = -15108398), repository.settings.first().theme)
    }

    @Test
    fun anUnreadableFileStartsOver() = runTest {
        val path = "/broken.json".toPath()
        fileSystem.write(path) { writeUtf8("{not json") }
        val repository = openSettingsRepository(fileSystem, path, backgroundScope)
        assertEquals(Settings(), repository.settings.first())
    }
}
