package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalTransferScope = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable internal fun ExportAssetDialog(data: Data, asset: Asset, info: AssetInfo?, close: () -> Unit, share: (java.io.File) -> Unit) {
    val transferScope = LocalTransferScope.current
    DisposableEffect(Unit) { transferScope(true); onDispose { transferScope(false) } }
    var history by rememberSaveable { mutableStateOf(false) }; var credentials by rememberSaveable { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var code by rememberSaveable { mutableStateOf<String?>(null) }; var encrypted by rememberSaveable { mutableStateOf<java.io.File?>(null) }
    var noted by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadImage = LocalImageLoader.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val selection = assetPackage(data, asset.id, credentials, history, info?.id)
    AlertDialog(onDismissRequest = { if (!working) close() }, title = { Text(if (code == null) "Verschlüsselt weitergeben" else "Dein Freigabecode") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            if (code == null) {
                Text(asset.name, fontWeight = FontWeight.Bold)
                Text(if (info == null) "Stammdaten, ${selection.infos.size} Wissenseinträge und ${selection.work.guides.size} Anleitungen (mit Bildern)" else "Nur Wissenseintrag: ${info.title}")
                if (info == null) {
                    Row { Checkbox(history, { history = it }, enabled = !working); Text("Störungen und Messwerte einschließen", modifier = Modifier.padding(top = 12.dp).weight(1f)) }
                    Row { Checkbox(credentials, { credentials = it }, enabled = !working); Text("Zugangsdaten ausdrücklich einschließen", modifier = Modifier.padding(top = 12.dp).weight(1f)) }
                    if (credentials) Text("Enthält ${selection.credentials.size} Zugänge samt Passwörtern. Nur an berechtigte Kollegen geben.", color = Amber)
                }
                Hint("Verschlüsselte Datei mit eigenem Zufallscode. Dein persönliches App-Passwort wird nicht weitergegeben. Datei und Code getrennt übermitteln.")
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                Text(code!!, fontFamily = FontFamily.Monospace, fontSize = 22.sp, color = Mint)
                Hint("Jetzt separat notieren. Der Code wird nach dem Schließen nicht gespeichert. Teile ihn persönlich oder über einen anderen Kommunikationsweg als die Datei.")
                Row { Checkbox(noted, { noted = it }); Text("Code separat notiert", modifier = Modifier.padding(top = 12.dp).weight(1f)) }
                Text("Die Empfänger-App zeigt den Inhalt vor dem Import. Jede Person mit Datei UND Code kann den Inhalt entschlüsseln.")
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = {
            if (code == null) TextButton(enabled = !working, onClick = {
                working = true; error = null
                scope.launch {
                    try { val generated = Exchange.newCode(); val file = java.io.File(java.io.File(context.cacheDir, "shares").also { it.mkdirs() }, "WerkLog-${newId()}.werkshare")
                        withContext(Dispatchers.IO) { ShareArchive.create(selection, generated, loadImage, file, context.cacheDir) }; encrypted = file; code = generated }
                    catch (_: Exception) { error = "Freigabe konnte nicht erstellt werden. Datenmenge prüfen." }
                    finally { working = false }
                }
            }) { Text("Datei & Code erstellen") }
            else TextButton(enabled = noted, onClick = { encrypted?.let(share); close() }) { Text("Datei teilen") }
        }, dismissButton = { TextButton(onClick = close, enabled = !working) { Text("Abbrechen") } })
}

@Composable internal fun ImportAssetDialog(file: java.io.File, current: Data, busy: Boolean, close: () -> Unit, save: (OpenShare, String?, Boolean) -> Unit) {
    val transferScope = LocalTransferScope.current
    DisposableEffect(Unit) { transferScope(true); onDispose { transferScope(false) } }
    var code by rememberSaveable { mutableStateOf("") }; var opened by remember { mutableStateOf<OpenShare?>(null) }
    var resumePreview by rememberSaveable { mutableStateOf(false) }
    val incoming = opened?.data
    val loadLocalImage = LocalImageLoader.current
    var comparison by remember { mutableStateOf<ImportChanges?>(null) }
    var comparing by remember { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf("") }; var replace by rememberSaveable { mutableStateOf(false) }
    var handedOff by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) { onDispose { if (!handedOff) opened?.close() } }
    var error by remember { mutableStateOf<String?>(null) }; var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(file.path) {
        if (resumePreview && code.isNotBlank() && opened == null) {
            working = true
            try { opened = withContext(Dispatchers.IO) { ShareArchive.open(file, code, context.cacheDir) } }
            catch (_: Exception) { error = "Freigabe bitte erneut entschlüsseln."; resumePreview = false }
            finally { working = false }
        }
    }
    LaunchedEffect(target, opened) {
        comparison = null
        if (target.isNotEmpty() && opened != null) {
            comparing = true
            try {
                val source = opened!!
                comparison = withContext(Dispatchers.IO) {
                    val local = fingerprintImages(assetPackage(current, target, true, true), loadLocalImage)
                    val remote = fingerprintImages(source.data) { source.image(it) }
                    importChanges(local, remote, target)
                }
            } catch (_: Exception) { error = "Vergleich nicht möglich. Bilddateien prüfen oder als neue Kopie importieren." }
            finally { comparing = false }
        }
    }
    AlertDialog(onDismissRequest = { if (!working) close() }, title = { Text("Anlagenfreigabe importieren") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            val preview = incoming
            if (preview == null) {
                Hint("Gib den separat erhaltenen Freigabecode ein. Das App-Passwort des Kollegen wird nicht benötigt.")
                Field(code, { code = it }, "Freigabecode")
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                val a = preview.assets.single()
                Text(a.name, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("${a.trade} · ${a.location}")
                Text("${preview.infos.size} Wissenseinträge · ${preview.entries.size} Vorgänge · ${preview.readings.size} Messwerte · ${preview.credentials.size} Zugänge · ${preview.work.guides.size} Anleitungen")
                Picker("Importziel", listOf("" to "Neue Anlagenkopie") + current.assets.map { it.id to it.name }, target) { target = it; replace = false }
                if (target.isNotEmpty()) {
                    if (comparing) LinearProgressIndicator(Modifier.fillMaxWidth())
                    comparison?.let { Text("${it.fresh} neu · ${it.changed} geändert · ${it.unchanged} unverändert", color = Mint) }
                    Hint("Vorhandene Anlagen-Stammdaten bleiben lokal. Neue Inhalte werden ergänzt. Wiederholte Importe derselben IDs erzeugen keine Duplikate.")
                    Row { Checkbox(replace, { replace = it }); Text("Auch bereits zugeordnete Inhalte durch diese Version ersetzen") }
                    if (replace) Text("Ersetzt auch lokal bearbeitete Inhalte mit gleicher Herkunft. Es werden keine fehlenden Inhalte gelöscht.", color = Amber)
                } else Hint("Neue Anlagenkopie mit eigener Identität. Herkunft und Richtigkeit bitte prüfen.")
                listOf("Hersteller" to a.manufacturer, "Typ" to a.model, "Seriennummer" to a.serial,
                    "Servicekontakt" to a.contact, "Ersatzteile" to a.spareParts, "Wartung" to a.nextService, "Hinweise" to a.note).forEach { (label, value) ->
                    if (value.isNotBlank()) Text("$label: $value", modifier = Modifier.padding(top = 8.dp))
                }
                preview.infos.forEach { Text(it.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(it.body) }
                preview.entries.forEach { Text("${it.status} · ${it.title}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(it.note) }
                preview.readings.forEach { Text("${it.label}: ${it.value} ${it.unit} · ${stamp(it.created)}", modifier = Modifier.padding(top = 8.dp)) }
                preview.work.guides.forEach { g -> Text("Anleitung: ${g.title}", fontWeight = FontWeight.Bold); g.steps.forEachIndexed { index, s -> Text("${index + 1}. ${s.title}"); Text(s.body); CompositionLocalProvider(LocalImageLoader provides { value -> withContext(Dispatchers.IO) { opened!!.image(value) } }) { StoredPhoto(s.image) } } }
                preview.credentials.forEach { Text("Zugang: ${it.title} (Passwort verborgen)", color = Amber, modifier = Modifier.padding(top = 8.dp)) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy && !working && !comparing && (target.isEmpty() || comparison != null) && (incoming != null || code.isNotBlank()), onClick = {
            val ready = incoming
            if (ready != null) { handedOff = true; save(opened!!, target.takeIf { it.isNotEmpty() }, replace); close() }
            else { working = true; error = null; val submittedCode = code
                scope.launch { try { opened = withContext(Dispatchers.IO) { ShareArchive.open(file, submittedCode, context.cacheDir) }; resumePreview = true }
                    catch (_: Exception) { error = "Code falsch, Datei beschädigt oder Format nicht unterstützt. Es wurde nichts importiert." }
                    finally { working = false } }
            }
        }) { Text(if (incoming == null) "Entschlüsseln & prüfen" else if (target.isEmpty()) "Als neue Anlage importieren" else "In Anlage übernehmen") } },
        dismissButton = { TextButton(onClick = close, enabled = !working) { Text("Abbrechen") } })
}
