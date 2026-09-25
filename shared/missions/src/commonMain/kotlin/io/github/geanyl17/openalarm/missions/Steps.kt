package io.github.geanyl17.openalarm.missions

import io.github.geanyl17.openalarm.core.Difficulty
import io.github.geanyl17.openalarm.core.Mission
import kotlin.math.sqrt

/** How many steps to walk: 10, 20 or 30 a round, depending on the difficulty. */
internal val Mission.stepsTarget: Int
    get() = rounds * when (difficulty) {
        Difficulty.Easy -> 10
        Difficulty.Normal -> 20
        Difficulty.Hard -> 30
    }

/**
 * Finds steps in accelerometer readings, for phones without a step sensor. Each step shows up as a bump
 * in how hard the phone is accelerating overall, which is the same whichever way the phone is held.
 */
class StepDetector {
    // How hard the phone accelerates, smoothed a little against jitter and a lot to find gravity.
    private var smoothed = GRAVITY
    private var gravity = GRAVITY
    private var armed = true
    private var lastStepNanos: Long? = null

    /** Takes one reading, in m/s² and at a time in nanoseconds. Returns whether it completed a step. */
    fun onAcceleration(x: Float, y: Float, z: Float, timeNanos: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        smoothed += (magnitude - smoothed) * SMOOTHING
        gravity += (magnitude - gravity) * GRAVITY_SMOOTHING
        val bump = smoothed - gravity
        // A step is a bump up after the acceleration has settled back down, so one bump counts once.
        if (bump < SETTLED) armed = true
        val last = lastStepNanos
        if (armed && bump > STEP && (last == null || timeNanos - last >= MIN_STEP_NANOS)) {
            armed = false
            lastStepNanos = timeNanos
            return true
        }
        return false
    }

    private companion object {
        const val GRAVITY = 9.80665f
        const val SMOOTHING = 0.3f
        const val GRAVITY_SMOOTHING = 0.01f
        const val STEP = 1.2f
        const val SETTLED = 0.2f

        /** Even running, steps are at least this far apart. */
        const val MIN_STEP_NANOS = 280_000_000L
    }
}
