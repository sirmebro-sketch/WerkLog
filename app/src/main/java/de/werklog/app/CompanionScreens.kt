package de.werklog.app

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun ReadingTrend(history: List<Reading>) {
    val points = history.take(30).reversed()
    if (points.size < 2) return
    Text("Letzte ${points.size} Ablesungen · zeitlicher Verlauf", color = Muted)
    Canvas(Modifier.fillMaxWidth().height(100.dp).padding(8.dp)) {
        val min = points.minOf { it.value }; val range = (points.maxOf { it.value } - min).coerceAtLeast(1.0)
        val first = points.first().created; val duration = (points.last().created - first).coerceAtLeast(1)
        fun point(r: Reading) = Offset(((r.created - first).toDouble() / duration * size.width).toFloat(), (size.height * (1 - (r.value - min) / range)).toFloat())
        points.zipWithNext().forEach { (a, b) -> if (!b.reset) drawLine(Mint, point(a), point(b), 3f) }
        points.forEach { drawCircle(if (it.reset) Amber else Mint, 4f, point(it)) }
    }
    Text("${points.minOf { it.value }} – ${points.maxOf { it.value }} ${points.last().unit} · Orange = Zählerwechsel", color = Muted)
}
@Composable fun TemplateScreen(d: Data, busy: Boolean, save: DataSaver) {
    var editing by rememberSaveable { mutableStateOf(false) }; var selected by rememberSaveable { mutableStateOf<EntryTemplate?>(null) }
    Section("Textvorlagen für Arbeiten")
    Hint("Wiederverwendbare Texte für ähnliche Arbeiten – zum Beispiel eine Pumpenkontrolle. Wähle sie beim Anlegen eines Protokolleintrags aus und ergänze deinen Befund.")
    Button(onClick = { selected = null; editing = true }, enabled = !busy) { Text("+ Vorlage") }
    (starterTemplates + d.work.templates).forEach { t -> Panel {
        Text(t.name); Text(t.body)
        TextButton(onClick = { selected = if (t in starterTemplates) t.copy(id = newId()) else t; editing = true }, enabled = !busy) { Text(if (t in starterTemplates) "Als eigene Vorlage anpassen" else "Bearbeiten") }
        if (t !in starterTemplates) TextButton(onClick = { save(d.copy(work = d.work.copy(templates = d.work.templates.filterNot { it.id == t.id }))) }, enabled = !busy) { Text("Vorlage entfernen") }
    } }
    if (editing) {
        val recordId = rememberSaveable { selected?.id ?: newId() }
        var trade by rememberSaveable { mutableStateOf(selected?.trade ?: "Alle") }
        var name by rememberSaveable { mutableStateOf(selected?.name ?: "") }; var title by rememberSaveable { mutableStateOf(selected?.title ?: "") }; var body by rememberSaveable { mutableStateOf(selected?.body ?: "") }
        Form("Vorlage", name.isNotBlank() && title.isNotBlank() && !busy, { editing = false }, {
            val t = EntryTemplate(recordId, name.trim(), title.trim(), body, trade)
            save(d.copy(work = d.work.copy(templates = d.work.templates.filterNot { it.id == t.id } + t))) { editing = false }
        }) { Picker("Gewerk", listOf("Alle" to "Alle Gewerke") + availableTrades(d).map { it to it }, trade) { trade = it }; Field(name, { name = it }, "Name der Vorlage"); Field(title, { title = it }, "Titel der Tätigkeit"); Field(body, { body = it }, "Formular / Textbaustein", 6) }
    }
}

@Composable fun OperationTiles(open: (String) -> Unit) {
    val tiles = listOf(
        "Anlagen" to androidx.compose.material.icons.Icons.Outlined.PrecisionManufacturing,
        "Arbeitsprotokoll" to androidx.compose.material.icons.Icons.Outlined.Assignment,
        "Zähler" to androidx.compose.material.icons.Icons.Outlined.Speed,
        "Rundgang" to androidx.compose.material.icons.Icons.Outlined.Checklist,
        "Kalender" to androidx.compose.material.icons.Icons.Outlined.CalendarMonth,
        "Anleitungen" to androidx.compose.material.icons.Icons.Outlined.MenuBook,
        "Bestellungen" to androidx.compose.material.icons.Icons.Outlined.ShoppingCart,
        "Textvorlagen" to androidx.compose.material.icons.Icons.Outlined.ContentCopy,
        "Adressbuch" to androidx.compose.material.icons.Icons.Outlined.Contacts)
    tiles.chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { (name, icon) -> Card(onClick = { open(name) }, modifier = Modifier.weight(1f).padding(bottom = 12.dp).heightIn(min = 115.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null, tint = Mint, modifier = Modifier.size(32.dp)); Text(name, style = MaterialTheme.typography.titleMedium)
                if (name == "Arbeitsprotokoll" || name == "Textvorlagen") Text(if (name == "Arbeitsprotokoll") "Störungen & Arbeiten" else "Texte wiederverwenden", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        } }
    } }
}
@Composable fun PasswordChangeDialog(busy: Boolean, close: () -> Unit, save: (CharArray, CharArray) -> Unit) {
    var old by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var repeat by remember { mutableStateOf("") }
    Form("App-Passwort ändern", !busy && old.isNotEmpty() && next.length >= 10 && next == repeat && old != next, close, { save(old.toCharArray(), next.toCharArray()) }) {
        listOf(Triple("Aktuelles Passwort", old, { x: String -> old = x }), Triple("Neues Passwort (mind. 10 Zeichen)", next, { x: String -> next = x }), Triple("Wiederholen", repeat, { x: String -> repeat = x })).forEach { (label, value, change) ->
            OutlinedTextField(value, change, label = { Text(label) }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), singleLine = true, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        }
        Hint("Der gesamte Tresor samt Bildern wird neu verschlüsselt. Danach Biometrie neu aktivieren und eine neue Sicherung erstellen. Alte Sicherungen benötigen weiterhin das alte Passwort.")
    }
}
@Composable fun ConfirmRemoval(label: String, detail: String, busy: Boolean, close: () -> Unit, remove: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(label) }, text = { Text(detail) }, confirmButton = { TextButton(onClick = remove, enabled = !busy) { Text("Löschen") } }, dismissButton = { TextButton(onClick = close) { Text("Abbrechen") } })
}
