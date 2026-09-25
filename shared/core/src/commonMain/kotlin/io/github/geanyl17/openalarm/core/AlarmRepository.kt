package io.github.geanyl17.openalarm.core

import kotlinx.coroutines.flow.Flow

/** Where alarms are stored. */
interface AlarmRepository {
    /** All alarms, sorted by time of day. */
    val alarms: Flow<List<Alarm>>

    suspend fun get(id: Long): Alarm?

    /** Adds [alarm] when its id is 0 (assigning a new id), otherwise replaces the alarm with that id. */
    suspend fun save(alarm: Alarm): Alarm

    suspend fun delete(id: Long)

    /** Applies [transform] to each alarm in [ids] as one atomic update. Unknown ids are ignored. */
    suspend fun update(ids: Collection<Long>, transform: (Alarm) -> Alarm)
}

/** Tells the operating system when to wake the app for the next alarm. */
fun interface AlarmScheduler {
    /** Makes [next] the only pending alarm, or cancels the pending alarm when [next] is null. */
    fun schedule(next: UpcomingAlarm?)
}
