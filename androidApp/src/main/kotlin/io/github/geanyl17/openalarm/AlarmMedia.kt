package io.github.geanyl17.openalarm

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import io.github.geanyl17.openalarm.core.Alarm
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.time.Duration.Companion.days

/**
 * The user's own sounds and cover photos. They're copied into device-protected storage, so they
 * still work before the first unlock after a reboot, and after the original file is deleted.
 */
internal object AlarmMedia {
    private const val TAG = "AlarmMedia"
    private const val SOUNDS = "sounds"
    private const val PHOTOS = "photos"
    private const val MAX_SOUND_BYTES = 50L * 1024 * 1024

    /** Copies of sounds from the phone's shared storage are kept in folders named after their media ID. */
    private const val SHARED_PREFIX = "shared-"

    /** MediaStore.VOLUME_INTERNAL, the built-in sounds. The constant is only public from Android 10. */
    private const val INTERNAL_VOLUME = "internal"

    /** Photos are stored at up to this many pixels on their longer side. */
    const val PHOTO_SIZE = 1600
    private const val PHOTO_QUALITY = 90

    /** Unused imports stay this long, in case an alarm that's still being edited is about to use them. */
    private val KEEP_UNUSED = 1.days

    /**
     * What to store for a sound picked in the phone's sound picker. The user's own sounds there live on
     * shared storage, which can't be read before the first unlock after a reboot, so those are copied into
     * the app, once each. Built-in sounds, and anything that can't be copied, are stored as they are.
     */
    fun soundForAlarm(context: Context, uri: Uri): String {
        val id = sharedSoundId(uri) ?: return uri.toString()
        val folder = File(storage(context, SOUNDS), "$SHARED_PREFIX$id")
        folder.listFiles()?.firstOrNull()?.let { return Uri.fromFile(it).toString() }
        val resolver = context.contentResolver
        return try {
            folder.mkdirs()
            val file = File(folder, fileName(resolver, uri))
            val input = resolver.openInputStream(uri) ?: throw IOException("No data")
            input.use { source -> file.outputStream().use { target -> copy(source, target, MAX_SOUND_BYTES) } }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Can't copy $uri", e)
            folder.deleteRecursively()
            uri.toString()
        }
    }

    /** How the phone's sound picker refers to [sound], so it can mark it as the current one. */
    fun pickerUri(sound: String): Uri {
        val id = file(sound)?.parentFile?.name?.takeIf { it.startsWith(SHARED_PREFIX) }?.removePrefix(SHARED_PREFIX)?.toLongOrNull()
        return if (id != null) ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id) else sound.toUri()
    }

    /** The name of a sound copied into the app, without its extension, or null for any other sound. */
    fun localSoundName(context: Context, sound: String): String? {
        val file = file(sound) ?: return null
        return file.nameWithoutExtension.takeIf { file.parentFile?.parentFile == storage(context, SOUNDS) }
    }

    /** The media ID of [uri] if it's a sound on shared storage, like the user's own sounds in the sound picker. */
    private fun sharedSoundId(uri: Uri): Long? {
        val segments = uri.pathSegments
        val shared = uri.authority == MediaStore.AUTHORITY && segments.size >= 4 && segments[0] != INTERNAL_VOLUME &&
            segments[1] == "audio" && segments[2] == "media"
        return if (shared) segments[3].toLongOrNull() else null
    }

    /** Copies the image at [uri] into the app, upright and scaled down. Returns its URI, or null if it can't be read. */
    fun importPhoto(context: Context, uri: Uri): String? = try {
        val bitmap = decodeUpright(context.contentResolver, uri, PHOTO_SIZE)
        val file = File(storage(context, PHOTOS).apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, it) }
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        Log.w(TAG, "Can't import $uri", e)
        null
    }

    /** Loads a photo from [importPhoto] at about [maxSize] pixels on its longer side, or null if it's gone. */
    fun loadPhoto(photo: String, maxSize: Int): ImageBitmap? {
        val path = file(photo)?.path ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize) }
        return BitmapFactory.decodeFile(path, options)?.asImageBitmap()
    }

    /** Deletes imported files that no alarm uses anymore. */
    fun deleteUnused(context: Context, alarms: List<Alarm>) {
        val used = alarms.flatMap { listOfNotNull(it.sound, it.photo) }.mapNotNull(::file).toSet()
        val cutoff = System.currentTimeMillis() - KEEP_UNUSED.inWholeMilliseconds
        val imports = listOf(SOUNDS, PHOTOS).flatMap { storage(context, it).listFiles()?.toList().orEmpty() }
        for (item in imports) {
            if (item.lastModified() < cutoff && item.walk().none { it in used }) item.deleteRecursively()
        }
    }

    private fun decodeUpright(resolver: ContentResolver, uri: Uri, maxSize: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSize) }
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Not an image")
        // Cameras save pictures sideways and note the rotation in EXIF. The stored copy is turned upright instead.
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0) }
        }.getOrNull()
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
    }

    /** The largest power of two that shrinks an image without taking it below [maxSize]. */
    private fun sampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxSize) sample *= 2
        return sample
    }

    private fun copy(source: InputStream, target: OutputStream, limit: Long) {
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = source.read(buffer)
            if (read < 0) return
            total += read
            if (total > limit) throw IOException("Larger than $limit bytes")
            target.write(buffer, 0, read)
        }
    }

    /** The file's name as the user knows it, made safe to use as a file name. */
    private fun fileName(resolver: ContentResolver, uri: Uri): String {
        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        return name?.replace(Regex("[^\\p{L}\\p{N} ._()-]"), "_")?.trim('.', ' ')?.take(120)?.ifBlank { null } ?: "sound"
    }

    private fun file(reference: String): File? = reference.toUri().takeIf { it.scheme == "file" }?.path?.let(::File)

    // Device-protected, like the alarms, so the files can be read before the first unlock after a reboot.
    private fun storage(context: Context, name: String) = File(context.createDeviceProtectedStorageContext().filesDir, name)
}
