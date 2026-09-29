package de.werklog.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/** Explicit mail exports: encrypted temporary files, transient keys, bounded concurrent readers. */
class OrderAttachmentProvider : ContentProvider() {
    private data class Attachment(val name: String, val size: Int, val file: File, val key: ByteArray, val ref: String)
    companion object {
        private val attachments = ConcurrentHashMap<String, Attachment>()
        private val readers = Semaphore(4)
        fun register(context: Context, authority: String, name: String, bytes: ByteArray): Uri {
            val token = newId(); val key = ByteArray(32).also { SecureRandom().nextBytes(it) }; val ref = "img:$token"
            val file = File(File(context.cacheDir, "order-exports").also { it.mkdirs() }, "$token.enc")
            try {
                require(attachments.size < MAX_IMAGES) { "Zu viele noch offene Mail-Anhänge" }
                validateImageBytes(bytes); file.writeBytes(ImageCipher.encrypt(bytes, key, ref))
                attachments[token] = Attachment(name, bytes.size, file, key, ref)
                Handler(Looper.getMainLooper()).postDelayed({ removeToken(token) }, 15 * 60 * 1000L)
                return Uri.Builder().scheme("content").authority(authority).appendPath(token).build()
            } catch (e: Exception) { key.fill(0); file.delete(); throw e } finally { bytes.fill(0) }
        }
        private fun removeToken(token: String) { attachments.remove(token)?.let { it.key.fill(0); it.file.delete() } }
        fun discard(uris: List<Uri>) { uris.forEach { it.lastPathSegment?.let(::removeToken) } }
        fun cleanup(context: Context) {
            val live = attachments.values.map { it.file.name }.toSet()
            File(context.cacheDir, "order-exports").listFiles()?.filter { it.name !in live }?.forEach { it.delete() }
        }
    }
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/jpeg"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val a = attachments[uri.lastPathSegment] ?: throw FileNotFoundException("Anhang abgelaufen; Entwurf erneut vorbereiten")
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(cols).apply { addRow(cols.map { when (it) { OpenableColumns.DISPLAY_NAME -> a.name; OpenableColumns.SIZE -> a.size; else -> null } }.toTypedArray()) }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Nur Lesen erlaubt")
        val a = attachments[uri.lastPathSegment] ?: throw FileNotFoundException("Anhang abgelaufen")
        if (!readers.tryAcquire()) throw FileNotFoundException("Zu viele parallele Lesezugriffe; erneut versuchen")
        val key = a.key.copyOf()
        val pipe = try { ParcelFileDescriptor.createPipe() } catch (e: Exception) { key.fill(0); readers.release(); throw e }
        Thread {
            var bytes: ByteArray? = null
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { out ->
                    bytes = ImageCipher.decrypt(a.file.readBounded(MAX_IMAGE_BYTES + 28), key, a.ref); out.write(bytes!!)
                }
            } catch (_: Exception) { /* Receiver closed pipe or grant expired. */ }
            finally { bytes?.fill(0); key.fill(0); readers.release() }
        }.start()
        return pipe[0]
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
