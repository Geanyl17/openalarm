package io.github.geanyl17.openalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.geanyl17.openalarm.appGraph
import io.github.geanyl17.openalarm.core.Escape
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
                // An alarm still ringing after a reboot means the phone was switched off (or its battery ran out)
                // mid-ring. After an app update, there's nothing to log.
                val reason = when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED -> Escape.PhoneOff
                    Intent.ACTION_MY_PACKAGE_REPLACED -> null
                    else -> Escape.AppStopped
                }
                RingingBackup.ringAgainIfInterrupted(context, reason)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "RescheduleReceiver"
    }
}
