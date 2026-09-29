package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable internal fun ExportAssetDialog(data: Data, asset: Asset, info: AssetInfo?, close: () -> Unit, share: (ByteArray) -> Unit) {
    var history by remember { mutableStateOf(false) }; var credentials by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf<String?>(null) }; var encrypted by remember { mutableStateOf<ByteArray?>(null) }
    var noted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selection = assetPackage(data, asset.id, credentials, history, info?.id)
    AlertDialog(onDismissRequest = { if (!working) close() }, title = { Text(if (code == null) "Verschlüsselt weitergeben" else "Dein Freigabecode") },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            if (code == null) {
                Text(asset.name, fontWeight = FontWeight.Bold)
                Text(if (info == null) "Stammdaten und ${selection.infos.size} Wissenseinträge" else "Nur Wissenseintrag: ${info.title}")
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
                    try { val generated = Exchange.newCode(); val bytes = withContext(Dispatchers.Default) { Exchange.encrypt(selection, generated) }; encrypted = bytes; code = generated }
                    catch (_: Exception) { error = "Freigabe konnte nicht erstellt werden. Datenmenge prüfen." }
                    finally { working = false }
                }
            }) { Text("Datei & Code erstellen") }
            else TextButton(enabled = noted, onClick = { encrypted?.let(share); close() }) { Text("Datei teilen") }
        }, dismissButton = { TextButton(onClick = close, enabled = !working) { Text("Abbrechen") } })
}

@Composable internal fun ImportAssetDialog(bytes: ByteArray, busy: Boolean, close: () -> Unit, save: (Data) -> Unit) {
    var code by remember { mutableStateOf("") }; var incoming by remember { mutableStateOf<Data?>(null) }
    var error by remember { mutableStateOf<String?>(null) }; var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
                Text("${preview.infos.size} Wissenseinträge · ${preview.entries.size} Vorgänge · ${preview.readings.size} Messwerte · ${preview.credentials.size} Zugänge")
                Hint("Importiert als neue Anlagenkopie. Bestehende Daten werden nicht überschrieben. Herkunft und Richtigkeit des Inhalts bitte selbst prüfen.")
                listOf("Hersteller" to a.manufacturer, "Typ" to a.model, "Seriennummer" to a.serial,
                    "Servicekontakt" to a.contact, "Ersatzteile" to a.spareParts, "Wartung" to a.nextService, "Hinweise" to a.note).forEach { (label, value) ->
                    if (value.isNotBlank()) Text("$label: $value", modifier = Modifier.padding(top = 8.dp))
                }
                preview.infos.forEach { Text(it.title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(it.body) }
                preview.entries.forEach { Text("${it.status} · ${it.title}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(it.note) }
                preview.readings.forEach { Text("${it.label}: ${it.value} ${it.unit} · ${stamp(it.created)}", modifier = Modifier.padding(top = 8.dp)) }
                preview.credentials.forEach { Text("Zugang: ${it.title} (Passwort verborgen)", color = Amber, modifier = Modifier.padding(top = 8.dp)) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy && !working && (incoming != null || code.isNotBlank()), onClick = {
            val ready = incoming
            if (ready != null) { save(ready); close() }
            else { working = true; error = null; val submittedCode = code; code = ""
                scope.launch { try { incoming = withContext(Dispatchers.Default) { Exchange.decrypt(bytes, submittedCode) } }
                    catch (_: Exception) { error = "Code falsch, Datei beschädigt oder Format nicht unterstützt. Es wurde nichts importiert." }
                    finally { working = false } }
            }
        }) { Text(if (incoming == null) "Entschlüsseln & prüfen" else "Als neue Anlage importieren") } },
        dismissButton = { TextButton(onClick = close, enabled = !working) { Text("Abbrechen") } })
}
