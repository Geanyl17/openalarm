package io.github.geanyl17.openalarm

import android.app.Application
import android.content.Context
import io.github.geanyl17.openalarm.alarm.AndroidAlarmScheduler
import io.github.geanyl17.openalarm.core.AlarmController
import io.github.geanyl17.openalarm.core.ThemeSettings
import io.github.geanyl17.openalarm.core.WakeLog
import io.github.geanyl17.openalarm.data.openAlarmRepository
import io.github.geanyl17.openalarm.data.openSettingsRepository
import io.github.geanyl17.openalarm.data.openWakeLogRepository
import io.github.geanyl17.openalarm.ringing.RingingNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.FileSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.setResourceReaderAndroidContext

class OpenAlarmApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    @OptIn(ExperimentalResourceApi::class)
    override fun onCreate() {
        super.onCreate()
        // Compose resources normally get their context from a ContentProvider, but Android doesn't start
        // it before the first unlock after a reboot, and the ringing screen needs its strings and icons then.
        setResourceReaderAndroidContext(this)
        RingingNotification.createChannel(this)
    }
}

/** App-wide singletons. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Device-protected storage can be read before the first unlock after a reboot,
    // so alarms still ring, in the chosen theme, if the phone restarts overnight.
    private val files = app.createDeviceProtectedStorageContext().filesDir.toOkioPath()

    val wakeLog = WakeLog(openWakeLogRepository(FileSystem.SYSTEM, files / "wakeups.json", scope))

    val controller = AlarmController(
        repository = openAlarmRepository(FileSystem.SYSTEM, files / "alarms.json", scope),
        scheduler = AndroidAlarmScheduler(app),
        wakeLog = wakeLog,
    )

    val settings = openSettingsRepository(FileSystem.SYSTEM, files / "settings.json", scope)

    /** Null until the settings file has been read. If it can't be, the app still works in the default theme. */
    val theme: StateFlow<ThemeSettings?> = settings.settings
        .map { it.theme }
        .catch { emit(ThemeSettings()) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun setTheme(theme: ThemeSettings) {
        scope.launch { settings.update { it.copy(theme = theme) } }
    }

    /** The user's daytime reaction speed, for the Alertness Gate, or null until it's measured. */
    val alertnessBaseline: StateFlow<Duration?> = settings.settings
        .map { settings -> settings.alertnessBaselineMillis?.milliseconds }
        .catch { emit(null) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun setAlertnessBaseline(baseline: Duration) {
        scope.launch { settings.update { it.copy(alertnessBaselineMillis = baseline.inWholeMilliseconds) } }
    }
}

val Context.appGraph: AppGraph get() = (applicationContext as OpenAlarmApp).graph
