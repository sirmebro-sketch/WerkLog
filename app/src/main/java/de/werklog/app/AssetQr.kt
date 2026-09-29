package de.werklog.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
fun assetCode(a: Asset) = if (a.tag.isNotBlank()) "werklog:tag:${a.tag}" else "werklog:asset:${a.id}"
fun matchAssetCode(d: Data, code: String): Asset? {
    val matches = when {
        code.startsWith("werklog:asset:") -> d.assets.filter { it.id == code.removePrefix("werklog:asset:") }
        code.startsWith("werklog:tag:") -> d.assets.filter { it.tag == code.removePrefix("werklog:tag:") }
        else -> d.assets.filter { it.tag.isNotBlank() && it.tag == code }
    }
    return matches.singleOrNull()
}
suspend fun readAssetCode(file: File): String = withContext(Dispatchers.IO) {
    val bitmap = try { decodeCamera(file, 1920) } finally { file.delete() }
    try {
        val pixels = IntArray(bitmap.width * bitmap.height); bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))).text
    } finally { bitmap.recycle() }
}
@Composable fun AssetQrDialog(asset: Asset, close: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(asset.id, asset.tag) {
        val matrix = MultiFormatWriter().encode(assetCode(asset), BarcodeFormat.QR_CODE, 600, 600)
        Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888).apply { setPixels(IntArray(360000) { if (matrix[it % 600, it / 600]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }, 0, 600, 0, 0, 600, 600) }
    }
    AlertDialog(onDismissRequest = close, title = { Text("Anlagen-Code") }, text = { Column {
        Image(bitmap.asImageBitmap(), "QR-Code zur Anlage", Modifier.fillMaxWidth().height(250.dp))
        Text(asset.name); Hint("Enthält nur Kennzeichen bzw. ID. Keine Passwörter oder Anlageninhalte. Zum Drucken als Bild teilen.")
    } }, confirmButton = { TextButton(onClick = {
        runCatching {
            val dir = File(context.cacheDir, "shares").also { it.mkdirs() }; val file = File(dir, "Anlagen-Code-${newId()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/png").putExtra(android.content.Intent.EXTRA_STREAM, uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = android.content.ClipData.newRawUri("Anlagen-Code", uri); context.startActivity(android.content.Intent.createChooser(intent, "Code speichern / drucken"))
        }
    }) { Text("Code teilen") } }, dismissButton = { TextButton(onClick = close) { Text("Schließen") } })
}
