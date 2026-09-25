package io.github.geanyl17.openalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.geanyl17.openalarm.appGraph
import io.github.geanyl17.openalarm.ringing.RingingBackup
import kotlinx.coroutines.launch

/**
 * Re-arms the next alarm whenever the system may have dropped or shifted it: after a reboot
 * (including before the first unlock), a clock or time zone change, or an app update. An alarm
 * that was still ringing when the phone switched off rings again.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "Rescheduling after ${intent.action}")
        val pending = goAsync()
        val graph = context.appGraph
        graph.scope.launch {
            try {
                graph.controller.reschedule()
                RingingBackup.ringAgainIfInterrupted(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "RescheduleReceiver"
    }
}
