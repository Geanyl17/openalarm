package io.github.geanyl17.openalarm.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap

/** Cover photos for alarms. Photos are platform references, such as file URIs on Android. */
interface AlarmPhotos {
    /** Lets the user pick a photo, and reports it unless they cancel or it can't be read. */
    fun choose(onChosen: (String) -> Unit)

    /** Loads [photo] at about [maxSize] pixels on its longer side, or returns null if it can't be read. */
    suspend fun load(photo: String, maxSize: Int): ImageBitmap?
}

/** [photo] once it has loaded; null before that, or if there's no photo. */
@Composable
internal fun rememberPhoto(photos: AlarmPhotos, photo: String?, maxSize: Int): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, photo, maxSize) {
        value = photo?.let { photos.load(it, maxSize) }
    }
    return bitmap
}
