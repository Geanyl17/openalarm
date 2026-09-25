package io.github.geanyl17.openalarm.ringing

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.net.toUri
import io.github.geanyl17.openalarm.R

/**
 * Plays the alarm sound on the alarm volume, plus vibration. It must never ring silently, so it
 * tries in turn: the alarm's own sound, the phone's default alarm sound, a sound bundled in the
 * app, and finally a generated tone. Call everything from the main thread.
 */
internal class AlarmPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val alarmAudio = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private var player: MediaPlayer? = null
    private var tone: ToneGenerator? = null
    private var audioFocus: AudioFocusRequest? = null
    private var volumeToRestore: Int? = null
    private var vibrating = false

    fun start(sound: String?, vibrate: Boolean, fadeIn: Boolean) {
        stop()
        raiseVolumeIfMuted()
        audioFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAudio)
            .build()
            .also { audioManager.requestAudioFocus(it) }

        val candidates = listOfNotNull(sound?.toUri(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), bundledSound())
        player = candidates.firstNotNullOfOrNull { play(it, fadeIn) }
        if (player == null) playTone()
        if (vibrate) startVibration()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        tone?.run {
            stopTone()
            release()
        }
        tone = null
        if (vibrating) vibrator.cancel()
        vibrating = false
        audioFocus?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocus = null
        volumeToRestore?.let { runCatching { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        volumeToRestore = null
    }

    private fun play(uri: Uri, fadeIn: Boolean): MediaPlayer? =
        try {
            MediaPlayer().apply {
                setAudioAttributes(alarmAudio)
                setDataSource(context, uri)
                isLooping = true
                setOnErrorListener { failed, what, extra ->
                    Log.w(TAG, "Playback failed ($what, $extra), switching to the bundled sound")
                    if (player === failed) switchToBundledSound()
                    true
                }
                prepare()
                val startVolume = if (fadeIn) MIN_VOLUME else 1f
                setVolume(startVolume, startVolume)
                start()
                if (fadeIn) fadeIn(this)
            }
        } catch (e: Exception) {
            // A deleted file, a sound on storage that's still locked after a reboot, a bad URI...
            Log.w(TAG, "Can't play $uri", e)
            null
        }

    private fun switchToBundledSound() {
        player?.release()
        player = play(bundledSound(), fadeIn = false)
        if (player == null) playTone()
    }

    /** Raises the volume gradually. Loudness is perceived logarithmically, so it follows a curve. */
    private fun fadeIn(target: MediaPlayer) {
        val start = SystemClock.elapsedRealtime()
        handler.post(object : Runnable {
            override fun run() {
                if (player !== target) return
                val progress = ((SystemClock.elapsedRealtime() - start) / FADE_IN_MILLIS.toFloat()).coerceAtMost(1f)
                val volume = MIN_VOLUME + (1f - MIN_VOLUME) * progress * progress
                target.setVolume(volume, volume)
                if (progress < 1f) handler.postDelayed(this, FADE_STEP_MILLIS)
            }
        })
    }

    private fun playTone() {
        Log.w(TAG, "No sound could be played, using a generated tone")
        tone = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME).apply {
            startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
        }
    }

    /** An alarm volume of zero would make the alarm silent, so it's raised to half while ringing. */
    private fun raiseVolumeIfMuted() {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        if (current > 0) return
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) / 2, 0)
            volumeToRestore = current
        } catch (e: SecurityException) {
            Log.w(TAG, "Can't raise the alarm volume", e)
        }
    }

    private fun startVibration() {
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, alarmAudio)
        }
        vibrating = true
    }

    private fun bundledSound(): Uri = "android.resource://${context.packageName}/${R.raw.alarm_fallback}".toUri()

    private companion object {
        const val TAG = "AlarmPlayer"
        const val MIN_VOLUME = 0.05f
        const val FADE_IN_MILLIS = 30_000L
        const val FADE_STEP_MILLIS = 250L
    }
}
