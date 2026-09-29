package de.werklog.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private fun decodeCamera(file: File, maximum: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(file.path, bounds)
    require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000)
    val options = BitmapFactory.Options(); var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maximum) sample *= 2
    options.inSampleSize = sample
    val original = BitmapFactory.decodeFile(file.path, options) ?: error("Bild nicht lesbar")
    val exif = ExifInterface(file)
    val matrix = Matrix().apply { postRotate(exif.rotationDegrees.toFloat()); if (exif.isFlipped) postScale(-1f, 1f) }
    val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
    if (rotated !== original) original.recycle()
    val ratio = maximum.toFloat() / maxOf(rotated.width, rotated.height)
    if (ratio >= 1) return rotated
    return Bitmap.createScaledBitmap(rotated, (rotated.width * ratio).toInt().coerceAtLeast(1), (rotated.height * ratio).toInt().coerceAtLeast(1), true).also { if (it !== rotated) rotated.recycle() }
}
suspend fun compactPhoto(file: File): String = withContext(Dispatchers.IO) {
    var bitmap: Bitmap? = null
    try {
        bitmap = decodeCamera(file, 1920)
        var result: ByteArray
        var quality = 78
        while (true) {
            val out = ByteArrayOutputStream(); bitmap!!.compress(Bitmap.CompressFormat.JPEG, quality, out); result = out.toByteArray()
            if (result.size <= MAX_IMAGE_BYTES) break
            if (quality > 38) quality -= 10 else {
                val old = bitmap!!; bitmap = Bitmap.createScaledBitmap(old, (old.width * .75).toInt().coerceAtLeast(1), (old.height * .75).toInt().coerceAtLeast(1), true)
                if (old !== bitmap) old.recycle()
            }
        }
        Base64.getEncoder().encodeToString(result)
    } finally { bitmap?.recycle(); file.delete() }
}
suspend fun recognizeMeter(file: File): String {
    val bitmap = try { withContext(Dispatchers.IO) { decodeCamera(file, 2048) } } finally { file.delete() }
    return suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).addOnCompleteListener { task ->
                try {
                    if (continuation.isActive) {
                        if (task.isSuccessful) continuation.resume(task.result.text)
                        else continuation.resumeWithException(task.exception ?: IllegalStateException("Erkennung fehlgeschlagen"))
                    }
                } finally { recognizer.close(); bitmap.recycle() }
            }
        } catch (e: Exception) { recognizer.close(); bitmap.recycle(); if (continuation.isActive) continuation.resumeWithException(e) }
    }
}
val LocalImageLoader = staticCompositionLocalOf<suspend (String) -> ByteArray> { { java.util.Base64.getDecoder().decode(it) } }
@Composable internal fun StoredPhoto(encoded: String) {
    if (encoded.isEmpty()) return
    val loader = LocalImageLoader.current
    val bitmap by produceState<Bitmap?>(null, encoded) {
        value = runCatching {
            val bytes = if (isImageRef(encoded)) loader(encoded) else Base64.getDecoder().decode(encoded)
            try { withContext(Dispatchers.Default) {
                require(bytes.size <= MAX_IMAGE_BYTES)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = 2 })
            } } finally { bytes.fill(0) }
        }.getOrNull()
    }
    bitmap?.let { Image(it.asImageBitmap(), "Hinterlegtes Bild", modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) }
}
@Composable internal fun PhotoReview(file: File, target: PhotoTarget, data: Data, busy: Boolean, close: () -> Unit, save: (Data) -> Unit) {
    var result by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    var value by remember { mutableStateOf("") }; var confirmed by remember { mutableStateOf(false) }
    val meters = data.work.meters
    var meterId by remember { mutableStateOf(target.id) }
    val meter = meters.find { it.id == meterId }
    LaunchedEffect(file.path) {
        try { result = if (target.kind == "meter") recognizeMeter(file) else compactPhoto(file) }
        catch (_: Exception) { error = "Foto konnte nicht verarbeitet werden. Bitte erneut aufnehmen oder den Zählerstand manuell eingeben." }
    }
    DisposableEffect(file.path) { onDispose { file.delete() } }
    val valid = if (target.kind == "meter") meter != null && number(value)?.let { it >= 0 } == true && confirmed else result != null
    Form(if (target.kind == "meter") "Zählerstand prüfen" else "Verkleinertes Bild", valid && !busy, close, {
        try {
        if (target.kind == "meter" && meter != null) save(data.copy(readings = data.readings + Reading(assetId = meter.assetId, label = meter.name, value = number(value)!!, unit = meter.unit, note = "Fotoerkennung / manuell bestätigt", meterId = meter.id)))
        else result?.let { save(attachImage(data, target, it)) }
        close()
        } catch (_: Exception) { error = "Bildlimit erreicht oder Ziel nicht mehr vorhanden. Bitte ein altes Bild entfernen und erneut versuchen." }
    }) {
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (result == null && error == null) { LinearProgressIndicator(Modifier.fillMaxWidth()); Hint("Verarbeitung auf diesem Gerät …") }
        if (target.kind == "meter") {
            Picker("Zähler", meters.map { it.id to "${it.name} · ${assetName(data, it.assetId)}" }, meterId) { meterId = it; confirmed = false }
            result?.let { recognized ->
                Hint("Erkannte Zahlen (Vorschläge, keine automatische Zuordnung):")
                Choices(meterCandidates(recognized), value) { value = it; confirmed = false }
                if (meterCandidates(recognized).isEmpty()) Hint("Keine eindeutige Zahl erkannt. Wert bitte selbst eintragen.")
            }
            Field(value, { value = it; confirmed = false }, "Bestätigter Zählerstand (${meter?.unit ?: "Einheit"})", numeric = true)
            Row { Checkbox(confirmed, { confirmed = it }); Text("Zähler, Einheit und Nachkommastellen am Original geprüft", modifier = Modifier.weight(1f).padding(top = 12.dp)) }
            Hint("Das Zählerfoto wird nicht gespeichert. Nur der bestätigte Zahlenwert kommt ins Protokoll.")
        } else {
            result?.let { StoredPhoto(it); Hint("JPEG · höchstens 512 KiB · wird nur verschlüsselt gespeichert.") }
        }
    }
}
