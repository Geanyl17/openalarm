package io.github.geanyl17.openalarm

import android.app.Application
import android.content.Context
import io.github.geanyl17.openalarm.alarm.AndroidAlarmScheduler
import io.github.geanyl17.openalarm.core.AlarmController
import io.github.geanyl17.openalarm.data.openAlarmRepository
import io.github.geanyl17.openalarm.ringing.RingingNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class OpenAlarmApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        RingingNotification.createChannel(this)
    }
}

/** App-wide singletons. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val controller = AlarmController(
        repository = openAlarmRepository(
            fileSystem = FileSystem.SYSTEM,
            // Device-protected storage can be read before the first unlock after a reboot,
            // so alarms still ring if the phone restarts overnight.
            path = app.createDeviceProtectedStorageContext().filesDir.toOkioPath() / "alarms.json",
            scope = scope,
        ),
        scheduler = AndroidAlarmScheduler(app),
    )
}

val Context.appGraph: AppGraph get() = (applicationContext as OpenAlarmApp).graph
