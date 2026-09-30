package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val LocalRecordLink = staticCompositionLocalOf<(String, String) -> Unit> { { _, _ -> } }
@Composable fun RecordLink(kind: String, id: String, label: String) {
    val open = LocalRecordLink.current
    TextButton(onClick = { open(kind, id) }) { Text(label) }
}
@Composable fun LinkChoices(label: String, options: List<Pair<String, String>>, chosen: List<String>, change: (List<String>) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    TextButton(onClick = { expanded = !expanded }) { Text("$label · ${chosen.size} zugeordnet ${if (expanded) "▴" else "▾"}") }
    if (expanded) {
        if (options.size > 8) Field(query, { query = it }, "$label suchen")
        options.filter { it.second.contains(query, true) }.forEach { (id, name) ->
            Row { Checkbox(id in chosen, { change(if (it) (chosen + id).distinct() else chosen - id) }, modifier = Modifier.semantics { contentDescription = "$label: $name" }); Text(name, Modifier.padding(top = 12.dp)) }
        }
        if (options.isEmpty()) Hint("Noch keine Einträge vorhanden.")
    }
}
@Composable fun ContactLinks(d: Data, kind: String, id: String, busy: Boolean, save: (Data) -> Unit) {
    val matching = d.contacts.filter { if (kind == "Anlagen") id in it.assetIds else id in it.entryIds }
    var edit by rememberSaveable { mutableStateOf(false) }
    Text("Ansprechpartner", fontWeight = FontWeight.Bold)
    matching.forEach { RecordLink("Adressbuch", it.id, listOf(it.name, it.role, it.company).filter(String::isNotBlank).joinToString(" · ")) }
    TextButton(onClick = { edit = true }, enabled = !busy) { Text("Kontakte zuordnen") }
    if (edit) {
        var ids by rememberSaveable { mutableStateOf(matching.map { it.id }) }
        Form("Kontakte zuordnen", !busy, { edit = false }, {
            save(d.copy(contacts = d.contacts.map { c ->
                if (kind == "Anlagen") c.copy(assetIds = if (c.id in ids) (c.assetIds + id).distinct() else c.assetIds - id)
                else c.copy(entryIds = if (c.id in ids) (c.entryIds + id).distinct() else c.entryIds - id)
            })); edit = false
        }) {
            Hint("Neue Personen unter Betrieb → Adressbuch anlegen.")
            LinkChoices("Kontakte", d.contacts.map { it.id to "${it.name} · ${it.company}" }, ids) { ids = it }
        }
    }
}
@Composable fun ContactScreen(d: Data, busy: Boolean, save: (Data) -> Unit, initialId: String?, consumed: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) }; var deleting by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(initialId) { if (initialId != null) { selected = initialId; consumed() } }
    val contact = d.contacts.find { it.id == selected }
    Section("Lokales Adressbuch")
    if (contact == null) {
        Field(query, { query = it }, "Name, Firma oder Aufgabe suchen")
        Button(onClick = { selected = null; editing = true }, enabled = !busy) { Text("+ Kontakt") }
        d.contacts.filter { listOf(it.name, it.company, it.role).any { s -> s.contains(query, true) } }.sortedBy { it.name.lowercase() }.forEach { c ->
            OutlinedButton(onClick = { selected = c.id }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) { Text(c.name, fontWeight = FontWeight.Bold); Text(listOf(c.company, c.role).filter(String::isNotBlank).joinToString(" · ")) }
            }
        }
        Hint("Verschlüsselt auf diesem Gerät. Kontakte werden nicht automatisch in Anlagenfreigaben oder Bestell-E-Mails übernommen.")
    } else {
        TextButton(onClick = { selected = null }) { Text("‹ Alle Kontakte") }
        Panel {
            Text(contact.name, style = MaterialTheme.typography.headlineSmall)
            listOf(contact.company, contact.role, contact.phone, contact.email, contact.note).filter(String::isNotBlank).forEach { Text(it) }
            TextButton(onClick = { editing = true }, enabled = !busy) { Text("Kontakt bearbeiten / Zuordnungen") }
            TextButton(onClick = { deleting = true }, enabled = !busy) { Text("Kontakt löschen") }
        }
        Section("Zugeordnete Anlagen")
        d.assets.filter { it.id in contact.assetIds }.forEach { RecordLink("Anlagen", it.id, "${it.tag} ${it.name}".trim()) }
        Section("Zugeordnete Störungen / Arbeiten")
        d.entries.filter { it.id in contact.entryIds }.forEach { RecordLink("Vorgang", it.id, "${it.title} · ${it.status}") }
    }
    if (deleting && contact != null) ConfirmRemoval("Kontakt löschen?", "Entfernt die Person und ihre Zuordnungen. Anlagen und Arbeiten bleiben erhalten.", busy, { deleting = false }) {
        save(d.copy(contacts = d.contacts.filterNot { it.id == contact.id })); selected = null; deleting = false
    }
    if (editing) {
        var name by rememberSaveable { mutableStateOf(contact?.name ?: "") }; var company by rememberSaveable { mutableStateOf(contact?.company ?: "") }
        var role by rememberSaveable { mutableStateOf(contact?.role ?: "") }; var phone by rememberSaveable { mutableStateOf(contact?.phone ?: "") }
        var email by rememberSaveable { mutableStateOf(contact?.email ?: "") }; var note by rememberSaveable { mutableStateOf(contact?.note ?: "") }
        var assets by rememberSaveable { mutableStateOf(contact?.assetIds ?: emptyList<String>()) }; var entries by rememberSaveable { mutableStateOf(contact?.entryIds ?: emptyList<String>()) }
        Form("Kontakt", name.isNotBlank() && !busy, { editing = false }, {
            val next = Contact(contact?.id ?: newId(), name.trim(), company.trim(), role.trim(), phone.trim(), email.trim(), note.trim(), assets, entries)
            save(d.copy(contacts = d.contacts.filterNot { it.id == next.id } + next)); selected = next.id; editing = false
        }) {
            Field(name, { name = it }, "Name *"); Field(company, { company = it }, "Firma / Abteilung"); Field(role, { role = it }, "Aufgabe / Zuständigkeit")
            Field(phone, { phone = it }, "Telefon"); Field(email, { email = it }, "E-Mail"); Field(note, { note = it }, "Hinweise / Erreichbarkeit", 3)
            LinkChoices("Anlagen", d.assets.map { it.id to "${it.tag} ${it.name}".trim() }, assets) { assets = it }
            LinkChoices("Störungen / Arbeiten", d.entries.map { it.id to "${it.title} · ${assetName(d, it.assetId)}" }, entries) { entries = it }
        }
    }
}
@Composable fun TradeSettings(d: Data, busy: Boolean, save: (Data) -> Unit) {
    var show by rememberSaveable { mutableStateOf(false) }; var editing by rememberSaveable { mutableStateOf(false) }
    var old by rememberSaveable { mutableStateOf("") }; var remove by rememberSaveable { mutableStateOf<String?>(null) }
    OutlinedButton(onClick = { show = !show }, modifier = Modifier.fillMaxWidth()) { Text("Gewerke verwalten") }
    if (show) Panel {
        Button(onClick = { old = ""; editing = true }, enabled = !busy) { Text("+ Gewerk") }
        availableTrades(d).forEach { trade ->
            Text(trade, fontWeight = FontWeight.Bold)
            Row { TextButton(onClick = { old = trade; editing = true }, enabled = !busy) { Text("Umbenennen") }
                TextButton(onClick = { remove = trade }, enabled = !busy && availableTrades(d).size > 1) { Text("Löschen / zuordnen") } }
        }
    }
    if (editing) {
        var name by rememberSaveable { mutableStateOf(old) }
        Form(if (old.isEmpty()) "Neues Gewerk" else "Gewerk umbenennen", name.isNotBlank() && !busy && (name.trim() == old || availableTrades(d).none { it.equals(name.trim(), true) }), { editing = false }, {
            save(renameTrade(d, old, name)); editing = false
        }) { Field(name, { name = it }, "Bezeichnung *"); Hint("Bestehende Anlagen und Textvorlagen werden beim Umbenennen mit angepasst.") }
    }
    remove?.let { trade ->
        var replacement by rememberSaveable { mutableStateOf(availableTrades(d).first { it != trade }) }
        Form("Gewerk löschen", !busy, { remove = null }, { save(renameTrade(d, trade, replacement)); remove = null }, confirmLabel = "Löschen & zuordnen") {
            Text("$trade entfernen. Betroffene Anlagen und Textvorlagen werden diesem Gewerk zugeordnet:")
            Picker("Ersatzgewerk", availableTrades(d).filterNot { it == trade }.map { it to it }, replacement) { replacement = it }
        }
    }
}

@Composable fun AssetThumbnail(image: String) {
    if (image.isBlank()) return
    val loader = LocalImageLoader.current
    var bitmap by remember(image) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(image) {
        bitmap = runCatching {
            val bytes = if (isImageRef(image)) loader(image) else java.util.Base64.getDecoder().decode(image)
            try { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = 8 }) }
            finally { bytes.fill(0) }
        }.getOrNull()
    }
    bitmap?.let { androidx.compose.foundation.Image(it.asImageBitmap(), "Anlagenbild", Modifier.size(48.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
}
