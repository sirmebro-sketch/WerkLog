package de.werklog.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

/** Explicit email exports live briefly in RAM, never as plaintext image files on disk. */
class OrderAttachmentProvider : ContentProvider() {
    private data class Attachment(val name: String, val bytes: ByteArray)
    companion object {
        private val attachments = ConcurrentHashMap<String, Attachment>()
        fun register(authority: String, name: String, bytes: ByteArray): Uri {
            val token = newId(); attachments[token] = Attachment(name, bytes)
            Handler(Looper.getMainLooper()).postDelayed({ attachments.remove(token)?.bytes?.fill(0) }, 15 * 60 * 1000L)
            return Uri.Builder().scheme("content").authority(authority).appendPath(token).build()
        }
    }
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/jpeg"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val a = attachments[uri.lastPathSegment] ?: throw FileNotFoundException("Anhang abgelaufen; Entwurf erneut vorbereiten")
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(cols).apply { addRow(cols.map { when (it) { OpenableColumns.DISPLAY_NAME -> a.name; OpenableColumns.SIZE -> a.bytes.size; else -> null } }.toTypedArray()) }
    }
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Nur Lesen erlaubt")
        val bytes = attachments[uri.lastPathSegment]?.bytes?.copyOf() ?: throw FileNotFoundException("Anhang abgelaufen")
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try { ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { it.write(bytes) } } catch (_: Exception) { /* receiver closed the pipe */ }
            finally { bytes.fill(0) }
        }.start()
        return pipe[0]
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
}
