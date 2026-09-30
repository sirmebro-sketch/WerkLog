package de.werklog.app

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun ReadingTrend(history: List<Reading>) {
    val points = history.take(30).reversed()
    if (points.size < 2) return
    Text("Letzte ${points.size} Ablesungen · zeitlicher Verlauf", color = Muted)
    val lineColor = Accent; val resetColor = Warning
    Canvas(Modifier.fillMaxWidth().height(100.dp).padding(8.dp)) {
        val min = points.minOf { it.value }; val range = (points.maxOf { it.value } - min).coerceAtLeast(1.0)
        val first = points.first().created; val duration = (points.last().created - first).coerceAtLeast(1)
        fun point(r: Reading) = Offset(((r.created - first).toDouble() / duration * (size.width - 10) + 5).toFloat(), ((size.height - 10) * (1 - (r.value - min) / range) + 5).toFloat())
        points.zipWithNext().forEach { (a, b) -> if (!b.reset) drawLine(lineColor, point(a), point(b), 3f) }
        points.forEach { if (it.reset) drawRect(resetColor, point(it) - Offset(4f, 4f), Size(8f, 8f)) else drawCircle(lineColor, 4f, point(it)) }
    }
    Text("${points.minOf { it.value }} – ${points.maxOf { it.value }} ${points.last().unit} · Quadrat = Zählerwechsel", color = Muted)
}
@Composable fun TemplateScreen(d: Data, busy: Boolean, save: DataSaver) {
    var editing by rememberSaveable { mutableStateOf(false) }; var selected by rememberSaveable { mutableStateOf<EntryTemplate?>(null) }
    Section("Textvorlagen für Arbeiten")
    Hint("Wiederverwendbare Texte für ähnliche Arbeiten – zum Beispiel eine Pumpenkontrolle. Wähle sie beim Anlegen eines Protokolleintrags aus und ergänze deinen Befund.")
    Button(onClick = { selected = null; editing = true }, enabled = !busy) { Text("+ Vorlage") }
    (starterTemplates + d.work.templates).forEach { t -> Panel {
        Text(t.name); Text(t.body)
        TextButton(onClick = { selected = if (t in starterTemplates) t.copy(id = newId()) else t; editing = true }, enabled = !busy) { Text(if (t in starterTemplates) "Als eigene Vorlage anpassen" else "Bearbeiten") }
        if (t !in starterTemplates) TextButton(onClick = { save(d.copy(work = d.work.copy(templates = d.work.templates.filterNot { it.id == t.id }))) }, enabled = !busy) { Text("Vorlage entfernen", color = MaterialTheme.colorScheme.error) }
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
    AlertDialog(onDismissRequest = close, title = { Text(label) }, text = { Text(detail) }, confirmButton = { TextButton(onClick = remove, enabled = !busy) { Text("Löschen", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = close) { Text("Abbrechen") } })
}
