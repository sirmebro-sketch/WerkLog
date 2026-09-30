package de.werklog.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

internal fun decodeCamera(file: File, maximum: Int): Bitmap {
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
suspend fun compactPhoto(file: File, maximum: Int = 1920, maxBytes: Int = MAX_IMAGE_BYTES): String = withContext(Dispatchers.IO) {
    var bitmap: Bitmap? = null
    try {
        bitmap = decodeCamera(file, maximum)
        var result: ByteArray
        var quality = 78
        while (true) {
            val out = ByteArrayOutputStream(); bitmap!!.compress(Bitmap.CompressFormat.JPEG, quality, out); result = out.toByteArray()
            if (result.size <= maxBytes) break
            if (quality > 38) quality -= 10 else {
                val old = bitmap!!; bitmap = Bitmap.createScaledBitmap(old, (old.width * .75).toInt().coerceAtLeast(1), (old.height * .75).toInt().coerceAtLeast(1), true)
                if (old !== bitmap) old.recycle()
            }
        }
        Base64.getEncoder().encodeToString(result)
    } finally { bitmap?.recycle() }
}
suspend fun recognizeMeter(file: File): String {
    val bitmap = withContext(Dispatchers.IO) { decodeCamera(file, 2048) }
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
@Composable internal fun StoredPhoto(encoded: String, initiallyOpen: Boolean = false) {
    if (encoded.isEmpty()) return
    var fullScreen by remember(encoded) { mutableStateOf(false) }
    var expanded by remember(encoded) { mutableStateOf(initiallyOpen) }
    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Bild verbergen" else "Bild anzeigen") }
    if (!expanded) return
    val loader = LocalImageLoader.current
    var bitmap by remember(encoded) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(encoded) {
        bitmap = runCatching {
            val bytes = if (isImageRef(encoded)) loader(encoded) else Base64.getDecoder().decode(encoded)
            try { withContext(Dispatchers.Default) {
                require(bytes.size <= MAX_IMAGE_BYTES)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                require(bounds.outWidth in 1..4096 && bounds.outHeight in 1..4096)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options())
            } } finally { bytes.fill(0) }
        }.getOrNull()
    }
    bitmap?.let { loaded ->
        Image(loaded.asImageBitmap(), "Bild in Vollbild öffnen", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).clickable { fullScreen = true })
        TextButton(onClick = { fullScreen = true }) { Text("Groß öffnen / vergrößern") }
        if (fullScreen) Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(
            usePlatformDefaultWidth = false, decorFitsSystemWindows = false, securePolicy = SecureFlagPolicy.SecureOn)) {
            var zoom by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                Column(Modifier.fillMaxSize().systemBarsPadding()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { zoom = 1f; offset = Offset.Zero }) { Text("Zurücksetzen") }
                        TextButton(onClick = { fullScreen = false }) { Text("Schließen") }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().pointerInput(Unit) {
                        detectTransformGestures { _, pan, change, _ ->
                            zoom = (zoom * change).coerceIn(1f, 6f)
                            val limitX = size.width * (zoom - 1) / 2f
                            val limitY = size.height * (zoom - 1) / 2f
                            offset = Offset((offset.x + pan.x).coerceIn(-limitX, limitX), (offset.y + pan.y).coerceIn(-limitY, limitY))
                        }
                    }, contentAlignment = Alignment.Center) {
                        Image(loaded.asImageBitmap(), "Vergrößertes Bild", contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoom, scaleY = zoom, translationX = offset.x, translationY = offset.y))
                    }
                    Text("Mit zwei Fingern zoomen und verschieben", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
@Composable internal fun PhotoReview(file: File, target: PhotoTarget, data: Data, busy: Boolean, close: () -> Unit, openAsset: (String) -> Unit, save: (Data) -> Unit) {
    var result by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }
    var value by rememberSaveable { mutableStateOf("") }; var confirmed by rememberSaveable { mutableStateOf(false) }
    val meters = data.work.meters
    var meterId by rememberSaveable { mutableStateOf(target.id) }
    val meter = meters.find { it.id == meterId }
    LaunchedEffect(file.path) {
        try { result = when (target.kind) { "meter" -> recognizeMeter(file); "asset" -> readAssetCode(file); "profile" -> compactPhoto(file, 384, MAX_PROFILE_IMAGE_BYTES); else -> compactPhoto(file) } }
        catch (_: Exception) { error = "Foto konnte nicht verarbeitet werden. Bitte erneut aufnehmen oder den Zählerstand manuell eingeben." }
    }
    if (target.kind == "asset") {
        val asset = result?.let { matchAssetCode(data, it) }
        Form("Anlage erkennen", asset != null && !busy, close, { asset?.let { openAsset(it.id) } }, confirmLabel = "Anlagenakte öffnen") {
            if (result == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(asset?.name ?: if (result != null) "Kein eindeutiger Treffer. Kennzeichen prüfen oder Anlage zuerst importieren." else "Code wird lokal gelesen …")
            error?.let { Text(it, color = Amber) }
        }
        return
    }
    val previous = meter?.let { m -> data.readings.filter { it.meterId == m.id }.maxByOrNull { it.created } }
    val warning = meter?.let { m -> number(value)?.let { meterWarning(m, previous, it, false) } }
    val valid = if (target.kind == "meter") meter != null && number(value)?.let { it >= 0 } == true && confirmed else result != null
    Form(if (target.kind == "meter") "Zählerstand prüfen" else "Verkleinertes Bild", valid && !busy, close, {
        try {
        if (target.kind == "meter" && meter != null) save(data.copy(readings = data.readings + Reading(assetId = meter.assetId, label = meter.name, value = number(value)!!, unit = meter.unit, note = "Fotoerkennung / manuell bestätigt" + (warning?.let { " · Auffälligkeit bestätigt: $it" } ?: ""), meterId = meter.id), work = data.work.copy(lastMeter = meter.id)))
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
            previous?.let { Text("Vorher: ${it.value} ${it.unit}") }; warning?.let { Text(it, color = Amber) }
            Field(value, { value = it; confirmed = false }, "Bestätigter Zählerstand (${meter?.unit ?: "Einheit"})", numeric = true)
            Row { Checkbox(confirmed, { confirmed = it }); Text("Zähler, Einheit und Nachkommastellen am Original geprüft", modifier = Modifier.weight(1f).padding(top = 12.dp)) }
            Hint("Das Zählerfoto wird nicht gespeichert. Nur der bestätigte Zahlenwert kommt ins Protokoll.")
        } else {
            result?.let { StoredPhoto(it, true); Hint("JPEG · höchstens 512 KiB · wird nur verschlüsselt gespeichert.") }
        }
    }
}
