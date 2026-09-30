package de.werklog.app

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable internal fun AssetDetails(asset: Asset, data: Data, busy: Boolean, back: () -> Unit, edit: () -> Unit,
    save: (Data) -> Unit, addEntry: () -> Unit, addReading: () -> Unit, editEntry: (Entry) -> Unit, shareFile: (java.io.File) -> Unit, photo: (PhotoTarget) -> Unit) {
    var section by rememberSaveable(asset.id) { mutableStateOf("Übersicht") }
    var credentialEditor by rememberSaveable { mutableStateOf(false) }
    var selectedCredential by rememberSaveable { mutableStateOf<Credential?>(null) }
    var infoEditor by rememberSaveable { mutableStateOf(false) }
    var selectedInfo by rememberSaveable { mutableStateOf<AssetInfo?>(null) }
    var sharing by rememberSaveable { mutableStateOf(false) }
    var shareInfo by rememberSaveable { mutableStateOf<AssetInfo?>(null) }
    var qr by rememberSaveable { mutableStateOf(false) }
    var deleteAsset by rememberSaveable { mutableStateOf(false) }
    var deletion by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    TextButton(onClick = back) { Text("‹ Alle Anlagen") }
    Text(asset.name, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text("${asset.trade} · ${asset.location.ifBlank { "Standort offen" }}", color = Mint)
    Text(asset.tag, color = Muted)
    Row {
        TextButton(onClick = { save(data.copy(assets = data.assets.map { if (it.id == asset.id) it.copy(favorite = !it.favorite) else it })) }, enabled = !busy) { Text(if (asset.favorite) "★ Favorit entfernen" else "☆ Als Favorit merken") }
    }
    TextButton(onClick = { qr = true }) { Text("Anlagen-QR-Code") }
    if (qr) AssetQrDialog(asset) { qr = false }
    if (asset.parentId.isNotBlank()) Text("Gehört zu: ${assetName(data, asset.parentId)}", color = Muted)
    Choices(listOf("Übersicht", "Wissen", "Zugänge", "Verlauf"), section) { section = it }
    TextButton(onClick = { shareInfo = null; sharing = true }, enabled = !busy) { Text("Anlagenakte verschlüsselt teilen") }
    Spacer(Modifier.height(12.dp))
    when (section) {
        "Übersicht" -> {
            StoredPhoto(asset.coverImage, true)
            TextButton(onClick = { photo(PhotoTarget("cover", asset.id)) }, enabled = !busy && (asset.coverImage.isNotBlank() || imageValues(data).count { it.isNotEmpty() } < MAX_IMAGES)) { Text(if (asset.coverImage.isBlank()) "+ Anlagenbild" else "Anlagenbild ersetzen") }
            if (asset.coverImage.isNotBlank()) TextButton(onClick = { save(data.copy(assets = data.assets.map { if (it.id == asset.id) it.copy(coverImage = "") else it })) }, enabled = !busy) { Text("Anlagenbild entfernen") }
            ContactLinks(data, "Anlagen", asset.id, busy, save)
            data.work.guides.filter { it.assetId == asset.id }.forEach { RecordLink("Anleitungen", it.id, "Anleitung: ${it.title}") }
            data.work.orders.filter { it.assetId == asset.id || it.items.any { x -> x.assetId == asset.id } }.forEach { RecordLink("Bestellungen", it.id, "Bestellung: ${it.title} · ${it.status}") }
            Panel {
                Section("Stammdaten")
                Detail("Hersteller", asset.manufacturer); Detail("Typ / Modell", asset.model); Detail("Kennzeichnung / Seriennummer", asset.serial)
                if (asset.manufacturer.isBlank() && asset.model.isBlank() && asset.serial.isBlank()) Hint("Hersteller, Typ und Seriennummer helfen bei Ersatzteilbestellungen und Serviceanfragen.")
                TextButton(onClick = edit, enabled = !busy) { Text("Anlageninformationen bearbeiten") }
            }
            Panel {
                Text("Wartung & Service", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (asset.nextService.isNotBlank()) Text("${serviceState(asset.nextService)} · ${asset.nextService}", color = Amber)
                else Hint("Noch kein Wartungstermin hinterlegt.")
                Detail("Ansprechpartner / Servicekontakt", asset.contact)
                Detail("Ersatzteile / Verbrauchsmaterial", asset.spareParts)
                TextButton(onClick = edit, enabled = !busy) { Text("Serviceinformationen pflegen") }
            }
            if (asset.note.isNotBlank()) Panel { Text("Hinweise", fontWeight = FontWeight.Bold); Text(asset.note) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = addEntry, enabled = !busy, modifier = Modifier.weight(1f)) { Text("+ Tätigkeit") }
                OutlinedButton(onClick = addReading, enabled = !busy, modifier = Modifier.weight(1f)) { Text("+ Messwert") }
            }
            Hint("Wartungstermine sind eigene Merker, keine automatische Terminberechnung oder Benachrichtigung.")
        }
        "Wissen" -> {
            Hint("Eigene Informationen zur Anlage: z. B. Schaltplan-Ablage, Ersatzteilnummer, Bedienhinweis oder eine wiederkehrende Störung. Passwörter gehören unter Zugänge.")
            Button(onClick = { selectedInfo = null; infoEditor = true }, enabled = !busy) { Text("+ Information hinterlegen") }
            val infos = data.infos.filter { it.assetId == asset.id }.sortedBy { it.title.lowercase() }
            if (infos.isEmpty()) Hint("Noch keine eigenen Informationen hinterlegt.")
            infos.forEach { info -> Panel {
                Text(info.title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(info.body)
                Text("Stand: ${stamp(info.updated)}", color = Muted, fontSize = 11.sp)
                TextButton(onClick = { shareInfo = info; sharing = true }, enabled = !busy) { Text("Diesen Wissenseintrag teilen") }
                Row { TextButton(onClick = { selectedInfo = info; infoEditor = true }, enabled = !busy) { Text("Bearbeiten") }
                    TextButton(onClick = { deletion = "info" to info.id }, enabled = !busy) { Text("Entfernen") } }
            } }
        }
        "Zugänge" -> {
            Hint("Zugänge für diese Anlage. Alle Angaben bleiben im verschlüsselten Tresor und werden nicht in E-Mails übernommen. Nur betrieblich freigegebene Zugangsdaten hinterlegen.")
            Button(onClick = { selectedCredential = null; credentialEditor = true }, enabled = !busy) { Text("+ Zugang hinterlegen") }
            val credentials = data.credentials.filter { it.assetId == asset.id }.sortedBy { it.title.lowercase() }
            if (credentials.isEmpty()) Hint("Zum Beispiel Bedienpanel, Regler oder Wartungszugang. Es wird keine Verbindung zur Anlage aufgebaut.")
            credentials.forEach { credential -> key(credential.id, credential.updated) {
                CredentialCard(credential, busy, { selectedCredential = credential; credentialEditor = true }, { deletion = "credential" to credential.id })
            } }
        }
        "Verlauf" -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = addEntry, enabled = !busy, modifier = Modifier.weight(1f)) { Text("+ Tätigkeit") }
                OutlinedButton(onClick = addReading, enabled = !busy, modifier = Modifier.weight(1f)) { Text("+ Messwert") }
            }
            Section("Störungen & Tätigkeiten")
            val entries = data.entries.filter { it.assetId == asset.id }.sortedByDescending { it.updated }
            if (entries.isEmpty()) Hint("Noch keine Vorgänge für diese Anlage.")
            entries.forEach { EntryCard(it, data) { if (!busy) editEntry(it) } }
            Section("Messwertprotokoll")
            val readings = data.readings.filter { it.assetId == asset.id }.sortedByDescending { it.created }
            if (readings.isEmpty()) Hint("Noch keine Messwerte für diese Anlage.")
            readings.forEach { r -> Panel { Text(r.label, fontWeight = FontWeight.Bold); Text("${r.value} ${r.unit}", fontSize = 26.sp)
                Text(stamp(r.created), color = Muted, fontSize = 12.sp); if (r.note.isNotBlank()) Text(r.note) } }
        }
    }
    TextButton(onClick = { deleteAsset = true }, enabled = !busy) { Text("Anlage löschen", color = MaterialTheme.colorScheme.error) }
    if (deleteAsset) ConfirmRemoval("${asset.name} löschen?", "Entfernt diese Anlage, ihre Zugangsdaten, Wissenseinträge, Tätigkeiten, Zähler und Messwerte. Anleitungen, Termine, Bestellungen und untergeordnete Anlagen bleiben ohne diese Zuordnung erhalten. Sicherungen bleiben unverändert.", busy, { deleteAsset = false }) { save(removeAsset(data, asset.id)); back() }
    if (sharing) ExportAssetDialog(data, asset, shareInfo, { sharing = false }, shareFile)
    if (credentialEditor) CredentialEditor(asset.id, selectedCredential, { credentialEditor = false }) { c ->
        save(data.copy(credentials = data.credentials.filterNot { it.id == c.id } + c)); credentialEditor = false
    }
    if (infoEditor) InfoEditor(asset.id, selectedInfo, { infoEditor = false }) { i ->
        save(data.copy(infos = data.infos.filterNot { it.id == i.id } + i)); infoEditor = false
    }
    deletion?.let { target -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text("Eintrag entfernen?") },
        text = { Text("Dieser Eintrag wird aus dem lokalen Tresor entfernt. Bereits erstellte Sicherungen bleiben unverändert.") },
        confirmButton = { TextButton(onClick = {
            if (target.first == "credential") save(data.copy(credentials = data.credentials.filterNot { it.id == target.second }))
            else save(data.copy(infos = data.infos.filterNot { it.id == target.second }))
            deletion = null
        }, enabled = !busy) { Text("Entfernen") } }, dismissButton = { TextButton(onClick = { deletion = null }) { Text("Abbrechen") } }) }
}

@Composable private fun Detail(label: String, value: String) {
    if (value.isNotBlank()) { Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)); Text(value) }
}
@Composable private fun CredentialCard(c: Credential, busy: Boolean, edit: () -> Unit, delete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) { delay(20_000); visible = false } }
    Panel {
        Text(c.title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Detail("Benutzername", c.username); Detail("Adresse / Bediengerät", c.address)
        Text(if (visible) c.password else "••••••••••••", fontFamily = FontFamily.Monospace, fontSize = 21.sp,
            modifier = Modifier.padding(vertical = 12.dp))
        TextButton(onClick = { visible = !visible }) { Text(if (visible) "Passwort verbergen" else "Passwort für 20 Sekunden anzeigen") }
        if (visible && c.note.isNotBlank()) Text(c.note)
        Text("Stand: ${stamp(c.updated)}", color = Muted, fontSize = 11.sp)
        Row { TextButton(onClick = { visible = false; edit() }, enabled = !busy) { Text("Bearbeiten") }
            TextButton(onClick = delete, enabled = !busy) { Text("Entfernen") } }
    }
}
@Composable private fun CredentialEditor(assetId: String, existing: Credential?, close: () -> Unit, save: (Credential) -> Unit) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }; var username by rememberSaveable { mutableStateOf(existing?.username ?: "") }
    var password by rememberSaveable { mutableStateOf(existing?.password ?: "") }; var address by rememberSaveable { mutableStateOf(existing?.address ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }; var visible by remember { mutableStateOf(false) }
    var generated by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(visible) { if (visible) { delay(20_000); visible = false } }
    Form("Anlagenzugang", title.isNotBlank() && password.isNotEmpty(), close, {
        save(Credential(existing?.id ?: newId(), assetId, title.trim(), username, password, address.trim(), note.trim()))
    }) {
        Field(title, { title = it }, "Bezeichnung * (z. B. Bedienpanel)"); Field(username, { username = it }, "Benutzername / Rolle")
        OutlinedTextField(password, { if (it.length <= 10000) password = it }, label = { Text("Passwort *") }, singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { visible = !visible }) { Text(if (visible) "Verbergen" else "Für 20 Sekunden anzeigen") }
        OutlinedButton(onClick = { generated = PasswordGenerator.generate() }) { Text("Sicheres Passwort vorschlagen") }
        Hint("Ein Vorschlag ändert nichts am echten Anlagenzugang. Hinterlege nur das dort gültige Passwort.")
        Field(address, { address = it }, "Adresse / Bediengerät (optional)"); Field(note, { note = it }, "Vertrauliche Hinweise", 3)
    }
    generated?.let { proposal -> AlertDialog(onDismissRequest = { generated = null }, title = { Text("Passwortvorschlag") },
        text = { androidx.compose.foundation.layout.Column { Text(proposal, fontFamily = FontFamily.Monospace); Hint("20 zufällige Zeichen. Prüfe, ob das Anlagen-System diese Zeichen erlaubt. Der Vorschlag ersetzt nur das Feld in diesem Formular.") } },
        confirmButton = { TextButton(onClick = { password = proposal; generated = null; visible = false }) { Text("Ins Formular übernehmen") } },
        dismissButton = { TextButton(onClick = { generated = null }) { Text("Verwerfen") } }) }
}
@Composable private fun InfoEditor(assetId: String, existing: AssetInfo?, close: () -> Unit, save: (AssetInfo) -> Unit) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }; var body by rememberSaveable { mutableStateOf(existing?.body ?: "") }
    Form("Anlagenwissen", title.isNotBlank() && body.isNotBlank(), close, { save(AssetInfo(existing?.id ?: newId(), assetId, title.trim(), body.trim())) }) {
        Field(title, { title = it }, "Überschrift *"); Field(body, { body = it }, "Information / Hinweis *", 6)
    }
}
@Composable internal fun AssetEditor(existing: Asset?, assets: List<Asset>, tradeOptions: List<String>, close: () -> Unit, save: (Asset) -> Unit) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }; var trade by rememberSaveable { mutableStateOf(existing?.trade ?: tradeOptions.firstOrNull().orEmpty()) }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }; var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var manufacturer by rememberSaveable { mutableStateOf(existing?.manufacturer ?: "") }; var model by rememberSaveable { mutableStateOf(existing?.model ?: "") }
    var serial by rememberSaveable { mutableStateOf(existing?.serial ?: "") }; var contact by rememberSaveable { mutableStateOf(existing?.contact ?: "") }
    var parts by rememberSaveable { mutableStateOf(existing?.spareParts ?: "") }; var service by rememberSaveable { mutableStateOf(existing?.nextService ?: "") }
    var tag by rememberSaveable { mutableStateOf(existing?.tag ?: "") }; var parent by rememberSaveable { mutableStateOf(existing?.parentId ?: "") }
    Form("Anlageninformationen", name.isNotBlank() && (service.isBlank() || parseServiceDate(service) != null), close, {
        save(Asset(existing?.id ?: newId(), name.trim(), trade, location.trim(), note.trim(), manufacturer.trim(), model.trim(), serial.trim(), contact.trim(), parts.trim(), service.trim(), tag.trim(), parent, existing?.favorite ?: false, existing?.lastOpened ?: 0, existing?.coverImage ?: ""))
    }) {
        Field(name, { name = it }, "Anlagenname *"); Picker("Gewerk", tradeOptions.map { it to it }, trade) { trade = it }
        Field(tag, { tag = it }, "Anlagenkennzeichen / KKS"); Picker("Übergeordnete Anlage", listOf("" to "Keine") + assets.filter { it.id != existing?.id }.map { it.id to it.name }, parent) { parent = it }
        Field(location, { location = it }, "Gebäude / Standort / Raum"); Field(manufacturer, { manufacturer = it }, "Hersteller")
        Field(model, { model = it }, "Typ / Modell"); Field(serial, { serial = it }, "Kennzeichnung / Seriennummer")
        Field(contact, { contact = it }, "Servicekontakt / Ansprechpartner", 2)
        Field(parts, { parts = it }, "Ersatzteile, Artikelnummern, Lagerort", 3)
        Field(service, { service = it }, "Nächste Wartung (TT.MM.JJJJ)")
        if (service.isNotBlank() && parseServiceDate(service) == null) Text("Bitte ein gültiges Datum eingeben, z. B. 15.10.2026.", color = Amber)
        Field(note, { note = it }, "Allgemeine Hinweise", 3)
    }
}
