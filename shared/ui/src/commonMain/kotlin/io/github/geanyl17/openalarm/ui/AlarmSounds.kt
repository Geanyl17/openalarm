package io.github.geanyl17.openalarm.ui

/** The phone's alarm sounds, including the user's own. Sounds are platform references, such as URIs on Android. */
interface AlarmSounds {
    /** The display name of [sound], or null if it can't be read. */
    fun name(sound: String): String?

    /** Lets the user pick a sound, starting from [current], and reports the choice. Null means the default sound. */
    fun choose(current: String?, onChosen: (String?) -> Unit)
}
