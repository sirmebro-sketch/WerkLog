package de.werklog.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Mint = Color(0xFF64DECB)
private val Amber = Color(0xFFFFCC80)
private val Muted = Color(0xFFABC1C7)
private val theme = darkColorScheme(primary = Mint, onPrimary = Color(0xFF00382F), secondary = Amber,
    background = Color(0xFF0C191E), surface = Color(0xFF14262D), surfaceVariant = Color(0xFF20363E), onSurface = Color(0xFFE8F2F3))
fun stamp(time: Long) = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))

class MainActivity : ComponentActivity() {
    private val model: WorkModel by viewModels()
    private var pendingBackup: ByteArray? = null
    private var restoreBytes by mutableStateOf<ByteArray?>(null)
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pendingBackup; pendingBackup = null
        if (uri != null && bytes != null) runCatching {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: error("Kein Zugriff")
        }.onFailure { model.error = "Sicherung konnte nicht geschrieben werden." }
    }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytesLimited() } ?: error("Kein Zugriff")
            Vault.saltOf(bytes); restoreBytes = bytes
        }.onFailure { model.error = "Datei ungültig oder größer als 8 MB." }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = theme) {
            Surface(Modifier.fillMaxSize(), color = theme.background) {
                key(model.session) {
                    if (model.data == null) LockScreen(model, restoreBytes, { restore.launch(arrayOf("*/*")) }, { restoreBytes = null })
                    else Workspace(model, onExport = {
                        runCatching { pendingBackup = model.backup(); export.launch("WerkLog-${java.time.LocalDate.now()}.werklog") }
                            .onFailure { model.error = "Sicherung konnte nicht geöffnet werden." }
                    }, onShare = ::share)
                }
                model.error?.let { message -> AlertDialog(onDismissRequest = { model.error = null }, title = { Text("Hinweis") },
                    text = { Text(message) }, confirmButton = { TextButton(onClick = { model.error = null }) { Text("Verstanden") } }) }
            }
        } }
    }
    override fun onStop() { super.onStop(); model.lock() }
    private fun share(subject: String, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(intent, "E-Mail-Entwurf öffnen"))
        } catch (_: Exception) { model.error = "Keine passende E-Mail-App verfügbar." }
    }
}
private fun java.io.InputStream.readBytesLimited(): ByteArray {
    val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
    while (true) { val n = read(buffer); if (n < 0) break; require(out.size() + n <= Vault.MAX_BYTES); out.write(buffer, 0, n) }
    return out.toByteArray()
}

@Composable private fun LockScreen(model: WorkModel, backup: ByteArray?, onRestore: () -> Unit, cancelRestore: () -> Unit) {
    var password by remember { mutableStateOf("") }; var repeat by remember { mutableStateOf("") }
    var confirmed by remember(backup) { mutableStateOf(false) }
    val creating = !model.exists && backup == null
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("W /", color = Mint, fontSize = 58.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp)); Text("WerkLog", fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text("Dein Technikalltag. Gut dokumentiert.", color = Muted)
        Spacer(Modifier.height(36.dp))
        Text(if (backup != null) "Sicherung wiederherstellen" else if (creating) "Deinen Tresor einrichten" else "Willkommen zurück", fontSize = 22.sp)
        Text(if (creating) "Ein lokales Passwort schützt deine Arbeitsdaten. Mindestens 10 Zeichen; keine Wiederherstellung möglich."
            else if (backup != null) "Gib das Passwort dieser Sicherung ein. Eine Wiederherstellung ersetzt alle aktuellen Daten."
            else "Deine Daten bleiben verschlüsselt auf diesem Gerät.", color = Muted, modifier = Modifier.padding(vertical = 12.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
        if (creating) OutlinedTextField(repeat, { repeat = it }, label = { Text("Passwort wiederholen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        if (backup != null && model.exists) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(confirmed, { confirmed = it }); Text("Aktuelle Daten durch diese Sicherung ersetzen", modifier = Modifier.weight(1f))
        }
        Button(onClick = { model.unlock(password.toCharArray(), backup); password = ""; repeat = ""; cancelRestore() },
            enabled = !model.busy && password.isNotEmpty() && (!creating || (password.length >= 10 && password == repeat)) && (backup == null || !model.exists || confirmed),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).heightIn(min = 52.dp)) {
            Text(if (model.busy) "Tresor wird geöffnet …" else if (creating) "Tresor erstellen" else "Entsperren")
        }
        TextButton(onClick = if (backup == null) onRestore else cancelRestore, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text(if (backup == null) "Verschlüsselte Sicherung laden" else "Wiederherstellung abbrechen") }
        Spacer(Modifier.height(24.dp)); Text("OFFLINE  ·  OHNE KONTO  ·  VERSCHLÜSSELT", color = Mint, fontSize = 11.sp)
    }
}

@Composable private fun Workspace(model: WorkModel, onExport: () -> Unit, onShare: (String, String) -> Unit) {
    val d = model.data ?: return
    var page by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var editAsset by remember { mutableStateOf<Asset?>(null) }
    var editEntry by remember { mutableStateOf<Entry?>(null) }
    var run by remember { mutableStateOf<Round?>(null) }
    var mail by remember { mutableStateOf<Pair<String, String>?>(null) }
    val tabs = listOf("Heute", "Anlagen", "Journal", "Rundgang", "Mehr")
    val icons = listOf(Icons.Outlined.Dashboard, Icons.Outlined.PrecisionManufacturing, Icons.Outlined.Assignment, Icons.Outlined.Checklist, Icons.Outlined.MoreHoriz)
    Scaffold(containerColor = theme.background, bottomBar = {
        NavigationBar(containerColor = theme.surface) { tabs.forEachIndexed { i, title -> NavigationBarItem(selected = page == i, onClick = { page = i }, icon = { Icon(icons[i], title) }, label = { Text(title, fontSize = 10.sp) }) } }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("WERKLOG  /  LOKAL", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(tabs[page], fontSize = 30.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = { model.lock() }) { Icon(Icons.Outlined.Lock, "App sperren", tint = Mint) }
            }
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            when (page) {
                0 -> {
                    Panel {
                        Text("Alles im Blick.", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM", java.util.Locale.GERMAN)), color = Muted)
                        Spacer(Modifier.height(22.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric(d.entries.count { it.status != "Erledigt" }.toString(), "Offen")
                            Metric(d.entries.count { it.priority == "Dringend" && it.status != "Erledigt" }.toString(), "Dringend", Amber)
                            Metric(d.assets.size.toString(), "Anlagen")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editEntry = null; dialog = "entry" }, modifier = Modifier.weight(1f), enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Störung") }
                        OutlinedButton(onClick = { dialog = "reading" }, modifier = Modifier.weight(1f), enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Messwert") }
                    }
                    if (d.assets.isEmpty()) Empty("Dein Arbeitsplatz, deine Struktur", "Lege zuerst eine Anlage an. Danach kannst du Störungen und Messwerte direkt zuordnen.") { editAsset = null; dialog = "asset" }
                    Section("Für die nächste Übergabe")
                    val open = d.entries.filter { it.status != "Erledigt" }.sortedWith(compareByDescending<Entry> { priorities.indexOf(it.priority) }.thenByDescending { it.updated })
                    if (open.isEmpty()) Hint("Keine offenen Einträge. Neue Störungen erscheinen hier.")
                    open.take(5).forEach { e -> EntryCard(e, d, { editEntry = e; dialog = "entry" }) }
                    OutlinedButton(onClick = { mail = "WerkLog · Schichtübergabe" to handover(d) }, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Übergabe ansehen & per E-Mail teilen") }
                }
                1 -> {
                    var query by remember { mutableStateOf("") }
                    Field(query, { query = it }, "Anlage oder Standort suchen")
                    Button(onClick = { editAsset = null; dialog = "asset" }, enabled = !model.busy) { Text("+ Anlage anlegen") }
                    val filtered = d.assets.filter { "${it.name} ${it.location} ${it.trade}".contains(query, true) }
                    if (filtered.isEmpty()) Hint("Keine Anlagen gefunden. Lege deine erste Anlage an oder ändere die Suche.")
                    filtered.forEach { a -> Panel {
                        Text(a.trade.uppercase(), color = Mint, fontSize = 11.sp); Text(a.name, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text(a.location.ifBlank { "Kein Standort hinterlegt" }, color = Muted)
                        if (a.note.isNotBlank()) Text(a.note, modifier = Modifier.padding(top = 8.dp))
                        Text("${d.entries.count { it.assetId == a.id && it.status != "Erledigt" }} offen · ${d.readings.count { it.assetId == a.id }} Messwerte", color = Amber, modifier = Modifier.padding(top = 12.dp))
                        TextButton(onClick = { editAsset = a; dialog = "asset" }) { Text("Anlage bearbeiten") }
                    } }
                }
                2 -> {
                    var query by remember { mutableStateOf("") }; var filter by remember { mutableStateOf("Alle") }; var kind by remember { mutableStateOf("Störungen") }
                    Choices(listOf("Störungen", "Messwerte"), kind) { kind = it }
                    Field(query, { query = it }, "Journal durchsuchen")
                    if (kind == "Störungen") {
                        Choices(listOf("Alle") + statuses, filter) { filter = it }
                        Button(onClick = { editEntry = null; dialog = "entry" }, enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Eintrag") }
                        val list = d.entries.filter { (filter == "Alle" || it.status == filter) && "${it.title} ${it.note} ${assetName(d, it.assetId)}".contains(query, true) }.sortedByDescending { it.updated }
                        if (list.isEmpty()) Hint("Keine passenden Einträge.")
                        list.forEach { e -> EntryCard(e, d, { editEntry = e; dialog = "entry" }) }
                    } else {
                        Button(onClick = { dialog = "reading" }, enabled = d.assets.isNotEmpty() && !model.busy) { Text("+ Messwert erfassen") }
                        val list = d.readings.filter { "${it.label} ${assetName(d, it.assetId)}".contains(query, true) }.sortedByDescending { it.created }
                        if (list.isEmpty()) Hint("Noch keine passenden Messwerte. Es werden keine Grenzwerte oder automatischen Sicherheitsbewertungen angenommen.")
                        list.forEach { r -> Panel {
                            Text(assetName(d, r.assetId), color = Mint, fontSize = 12.sp); Text(r.label, fontWeight = FontWeight.Bold)
                            Text("${r.value} ${r.unit}", fontSize = 28.sp); Text(stamp(r.created), color = Muted, fontSize = 12.sp)
                            if (r.note.isNotBlank()) Text(r.note)
                        } }
                    }
                }
                3 -> {
                    Hint("Eigene Checklisten für wiederkehrende Kontrollen. Ein übersprungener Punkt bleibt ausdrücklich als ungeprüft dokumentiert.")
                    Button(onClick = { dialog = "round" }, enabled = !model.busy) { Text("+ Checkliste erstellen") }
                    d.rounds.forEach { r -> Panel { Text(r.title, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("${r.checks.size} Prüfpunkte", color = Muted)
                        Button(onClick = { run = r }, enabled = !model.busy) { Text("Rundgang starten") } } }
                    Section("Abgeschlossene Rundgänge")
                    if (d.runs.isEmpty()) Hint("Noch kein Rundgang dokumentiert.")
                    d.runs.sortedByDescending { it.created }.forEach { r -> Panel { Text(r.title, fontWeight = FontWeight.Bold); Text(stamp(r.created), color = Muted)
                        r.results.forEach { Text(it, modifier = Modifier.padding(top = 6.dp)) }; if (r.note.isNotBlank()) Text(r.note)
                    } }
                }
                4 -> {
                    Panel { Text("Dein Datentresor", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Lokal verschlüsselt · Ohne Internetberechtigung", color = Mint)
                        Text("Beim Verlassen wird die App gesperrt. Screenshots sind blockiert. Ein verlorenes Passwort lässt sich nicht zurücksetzen.", modifier = Modifier.padding(top = 12.dp)) }
                    Button(onClick = onExport, enabled = !model.busy, modifier = Modifier.fillMaxWidth()) { Text("Verschlüsselte Sicherung speichern") }
                    Hint("Wähle einen lokalen Ordner, wenn die Sicherung auf dem Gerät bleiben soll. Der Android-Dateidialog kann auch Cloud-Anbieter anzeigen. Wiederherstellen ist am Sperrbildschirm möglich.")
                    OutlinedButton(onClick = { mail = "WerkLog · Schichtübergabe" to handover(d) }, modifier = Modifier.fillMaxWidth()) { Text("Schichtübergabe vorbereiten") }
                    Section("Für deinen Arbeitsalltag")
                    Hint("WerkLog dokumentiert Beobachtungen und Tätigkeiten. Freigaben, Betriebsanweisungen und eure offiziellen Meldewege bleiben maßgeblich. Keine Anlagensteuerung oder Verbindung zur GLT.")
                    Text("WERKLOG 0.1.0 · KOTLIN / ANDROID", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (dialog == "asset") AssetEditor(editAsset, { dialog = null }) { a -> model.update(d.copy(assets = d.assets.filterNot { it.id == a.id } + a)); dialog = null }
    if (dialog == "entry") EntryEditor(editEntry, d.assets, { dialog = null }) { e -> model.update(d.copy(entries = d.entries.filterNot { it.id == e.id } + e)); dialog = null }
    if (dialog == "reading") ReadingEditor(d.assets, { dialog = null }) { r -> model.update(d.copy(readings = d.readings + r)); dialog = null }
    if (dialog == "round") RoundEditor({ dialog = null }) { r -> model.update(d.copy(rounds = d.rounds + r)); dialog = null }
    run?.let { r -> RoundRunner(r, { run = null }) { result -> model.update(d.copy(runs = d.runs + result)); run = null } }
    mail?.let { m -> AlertDialog(onDismissRequest = { mail = null }, title = { Text("E-Mail-Vorschau") }, text = {
        Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState())) {
            Text("Der folgende Text wird unverschlüsselt an deine E-Mail-App übergeben. Empfänger und Versand bestimmst du dort.", color = Amber)
            Text(m.second, modifier = Modifier.padding(top = 16.dp))
        }
    }, confirmButton = { TextButton(onClick = { mail = null; onShare(m.first, m.second) }) { Text("E-Mail-App öffnen") } }, dismissButton = { TextButton(onClick = { mail = null }) { Text("Abbrechen") } }) }
}

private fun assetName(d: Data, id: String) = d.assets.find { it.id == id }?.name ?: "Anlage"
private fun handover(d: Data) = buildString {
    append("SCHICHTÜBERGABE · WERKLOG\n${stamp(System.currentTimeMillis())}\n\nOFFENE VORGÄNGE\n")
    val open = d.entries.filter { it.status != "Erledigt" }.sortedByDescending { priorities.indexOf(it.priority) }
    if (open.isEmpty()) append("Keine offenen Vorgänge.\n")
    open.forEach { append("\n[${it.priority} · ${it.status}] ${it.title}\n${assetName(d, it.assetId)}\n${it.note}\nAktualisiert: ${stamp(it.updated)}\nAufwand: ${it.minutes} min\n") }
    val today = java.time.LocalDate.now()
    val done = d.entries.filter { it.status == "Erledigt" && Instant.ofEpochMilli(it.updated).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    append("\nHEUTE ERLEDIGT\n"); if (done.isEmpty()) append("Keine Einträge.\n")
    done.forEach { append("• ${assetName(d, it.assetId)}: ${it.title}\n${it.note}\n") }
}
@Composable private fun Metric(value: String, label: String, color: Color = Mint) { Column { Text(value, fontSize = 36.sp, color = color, fontWeight = FontWeight.Bold); Text(label, color = Muted, fontSize = 13.sp) } }
@Composable private fun Panel(content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = theme.surface)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content) } }
@Composable private fun Section(title: String) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)) }
@Composable private fun Hint(text: String) { Text(text, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp)) }
@Composable private fun Empty(title: String, body: String, click: () -> Unit) { Panel { Text(title, fontWeight = FontWeight.Bold); Hint(body); TextButton(onClick = click) { Text("Erste Anlage anlegen") } } }
@Composable private fun EntryCard(e: Entry, d: Data, click: () -> Unit) { Panel {
    Text("${e.priority.uppercase()}  ·  ${e.status}", color = if (e.priority == "Dringend") Amber else Mint, fontSize = 11.sp)
    Text(e.title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(assetName(d, e.assetId), color = Muted)
    if (e.note.isNotBlank()) Text(e.note, maxLines = 3)
    Text(stamp(e.updated), fontSize = 11.sp, color = Muted); TextButton(onClick = click) { Text("Öffnen & bearbeiten") }
} }
@Composable private fun Field(value: String, change: (String) -> Unit, label: String, lines: Int = 1, numeric: Boolean = false) {
    OutlinedTextField(value, { if (it.length <= 10000) change(it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), minLines = lines, maxLines = if (lines == 1) 1 else 8,
        singleLine = lines == 1, keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable private fun Choices(options: List<String>, selected: String, change: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.forEach { FilterChip(selected == it, { change(it) }, label = { Text(it) }) } }
}
@Composable private fun Picker(label: String, values: List<Pair<String, String>>, selected: String, change: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${values.find { it.first == selected }?.second ?: "Auswählen"}") }
        DropdownMenu(expanded, { expanded = false }) { values.forEach { v -> DropdownMenuItem(text = { Text(v.second) }, onClick = { change(v.first); expanded = false }) } }
    }
}
@Composable private fun Form(title: String, valid: Boolean, close: () -> Unit, save: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), content = content) },
        confirmButton = { TextButton(onClick = save, enabled = valid) { Text("Speichern") } }, dismissButton = { TextButton(onClick = close) { Text("Abbrechen") } })
}
@Composable private fun AssetEditor(existing: Asset?, close: () -> Unit, save: (Asset) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }; var trade by remember { mutableStateOf(existing?.trade ?: trades.first()) }
    var location by remember { mutableStateOf(existing?.location ?: "") }; var note by remember { mutableStateOf(existing?.note ?: "") }
    Form("Anlage", name.isNotBlank(), close, { save(Asset(existing?.id ?: newId(), name.trim(), trade, location.trim(), note.trim())) }) {
        Field(name, { name = it }, "Anlagenname *"); Picker("Gewerk", trades.map { it to it }, trade) { trade = it }
        Field(location, { location = it }, "Standort / Raum"); Field(note, { note = it }, "Kennzeichnung, Typ, Hinweise", 3)
    }
}
@Composable private fun EntryEditor(existing: Entry?, assets: List<Asset>, close: () -> Unit, save: (Entry) -> Unit) {
    var asset by remember { mutableStateOf(existing?.assetId ?: assets.firstOrNull()?.id.orEmpty()) }
    var title by remember { mutableStateOf(existing?.title ?: "") }; var note by remember { mutableStateOf(existing?.note ?: "") }
    var priority by remember { mutableStateOf(existing?.priority ?: "Normal") }; var status by remember { mutableStateOf(existing?.status ?: "Offen") }
    var minutes by remember { mutableStateOf((existing?.minutes ?: 0).toString()) }
    Form("Störung / Tätigkeit", title.isNotBlank() && asset.isNotEmpty() && (minutes.toIntOrNull()?.let { it >= 0 } == true), close, {
        save(Entry(existing?.id ?: newId(), asset, title.trim(), note.trim(), priority, status, existing?.created ?: System.currentTimeMillis(), System.currentTimeMillis(), minutes.toInt()))
    }) { Picker("Anlage", assets.map { it.id to it.name }, asset) { asset = it }; Field(title, { title = it }, "Kurzbeschreibung *")
        Field(note, { note = it }, "Beobachtung, Maßnahmen, nächste Schritte", 4); Text("Priorität"); Choices(priorities, priority) { priority = it }
        Text("Status"); Choices(statuses, status) { status = it }; Field(minutes, { minutes = it }, "Zeitaufwand in Minuten", numeric = true)
    }
}
@Composable private fun ReadingEditor(assets: List<Asset>, close: () -> Unit, save: (Reading) -> Unit) {
    var asset by remember { mutableStateOf(assets.firstOrNull()?.id.orEmpty()) }; var label by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }; var unit by remember { mutableStateOf("bar") }; var note by remember { mutableStateOf("") }
    Form("Messwert erfassen", asset.isNotEmpty() && label.isNotBlank() && number(value) != null && unit.isNotBlank(), close, {
        save(Reading(assetId = asset, label = label.trim(), value = number(value)!!, unit = unit.trim(), note = note.trim()))
    }) { Picker("Anlage", assets.map { it.id to it.name }, asset) { asset = it }; Field(label, { label = it }, "Messpunkt *")
        Field(value, { value = it }, "Messwert *", numeric = true); Field(unit, { unit = it }, "Einheit *"); Field(note, { note = it }, "Beobachtung", 2) }
}
@Composable private fun RoundEditor(close: () -> Unit, save: (Round) -> Unit) {
    var title by remember { mutableStateOf("") }; var points by remember { mutableStateOf("") }
    val checks = points.lines().map { it.trim() }.filter { it.isNotEmpty() }
    Form("Checkliste erstellen", title.isNotBlank() && checks.size in 1..100, close, { save(Round(title = title.trim(), checks = checks)) }) {
        Field(title, { title = it }, "Name *"); Field(points, { points = it }, "Ein Prüfpunkt pro Zeile *", 6)
        Hint("Nur eure freigegebenen Kontrollen übernehmen. Maximal 100 Punkte.")
    }
}
@Composable private fun RoundRunner(round: Round, close: () -> Unit, save: (RoundRun) -> Unit) {
    var results by remember { mutableStateOf(List(round.checks.size) { "Ungeprüft" }) }; var note by remember { mutableStateOf("") }
    Form(round.title, results.any { it != "Ungeprüft" } && (!results.contains("Auffällig") || note.isNotBlank()), close, {
        save(RoundRun(title = round.title, results = round.checks.mapIndexed { i, check -> "${results[i]} · $check" }, note = note.trim()))
    }) { round.checks.forEachIndexed { i, text -> Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Choices(listOf("Ungeprüft", "In Ordnung", "Auffällig"), results[i]) { v -> results = results.toMutableList().also { it[i] = v } }
    }; Field(note, { note = it }, if (results.contains("Auffällig")) "Auffälligkeit / Maßnahme *" else "Notiz", 3)
        Hint("Auffälligkeiten lösen keine automatische Störungsmeldung aus. Falls nötig, zusätzlich im Journal erfassen.")
    }
}
