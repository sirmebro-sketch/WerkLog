package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable internal fun WorkTools(mode: String, data: Data, busy: Boolean, save: DataSaver, photo: (PhotoTarget) -> Unit, mailOrder: (PartsOrder) -> Unit, recordId: String? = null, select: (String?) -> Unit = {}) {
    when (mode) {
        "Zähler" -> MeterScreen(data, busy, save, photo)
        "Kalender" -> CalendarScreen(data, busy, save, recordId, select)
        "Anleitungen" -> GuideScreen(data, busy, save, photo, recordId, select)
        "Bestellungen" -> OrderScreen(data, busy, save, photo, mailOrder, recordId, select)
        "Adressbuch" -> ContactScreen(data, busy, save, recordId, select)
        "Textvorlagen" -> TemplateScreen(data, busy, save)
    }
}
@Composable private fun MeterScreen(d: Data, busy: Boolean, save: DataSaver, photo: (PhotoTarget) -> Unit) {
    var edit by rememberSaveable { mutableStateOf(false) }; var selected by rememberSaveable { mutableStateOf<Meter?>(null) }
    var remove by rememberSaveable { mutableStateOf<Meter?>(null) }
    var reading by rememberSaveable { mutableStateOf<Meter?>(null) }
    Section("Zähler & Ablesungen")
    Hint("Zähler fest einer Anlage zuordnen. Fotoerkennung läuft offline; jeder Wert wird vor dem Speichern geprüft. Fotos bleiben nicht im Archiv.")
    Button(onClick = { selected = null; edit = true }, enabled = !busy && d.assets.isNotEmpty()) { Text("+ Zähler anlegen") }
    if (d.assets.isEmpty()) Hint("Bitte zuerst eine Anlage anlegen.")
    d.work.meters.sortedByDescending { it.id == d.work.lastMeter }.forEach { meter -> Panel {
        Text(meter.name, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(assetName(d, meter.assetId), color = Muted)
        val history = d.readings.filter { it.meterId == meter.id }.sortedByDescending { it.created }
        history.firstOrNull()?.let { Text("Zuletzt: ${it.value} ${it.unit} · ${stamp(it.created)}") }
        if (meter.note.isNotBlank()) Text(meter.note)
        Row { TextButton(onClick = { photo(PhotoTarget("meter", meter.id)) }, enabled = !busy) { Text("Zähler fotografieren") }
            TextButton(onClick = { reading = meter }, enabled = !busy) { Text("Manuell") } }
        TextButton(onClick = { selected = meter; edit = true }, enabled = !busy) { Text("Zähler bearbeiten") }; TextButton(onClick = { remove = meter }, enabled = !busy) { Text("Zähler löschen", color = MaterialTheme.colorScheme.error) }
        ReadingTrend(history)
        val delta = history.firstOrNull()?.let { readingDelta(it, history.getOrNull(1)) }
        if (delta != null) Text("Seit letzter Ablesung: $delta ${meter.unit}")
        history.take(5).forEach { Text("${stamp(it.created)} · ${it.value} ${it.unit}", color = Muted, fontSize = 12.sp) }
    } }
    remove?.let { m -> ConfirmRemoval("Zähler löschen?", "Die Ablesungen bleiben als Messwerte im Arbeitsprotokoll erhalten; die Zählerzuordnung entfällt.", busy, { remove = null }) {
        save(d.copy(readings = d.readings.map { if (it.meterId == m.id) it.copy(meterId = "") else it }, work = d.work.copy(meters = d.work.meters.filterNot { it.id == m.id }))) { remove = null }
    } }
    if (edit) MeterEditor(selected, d, { edit = false }) { meter -> save(d.copy(work = d.work.copy(meters = d.work.meters.filterNot { it.id == meter.id } + meter))) { edit = false } }
    reading?.let { m -> val recordId = rememberSaveable { newId() }; var value by rememberSaveable { mutableStateOf("") }; var note by rememberSaveable { mutableStateOf("") }
        var reset by rememberSaveable { mutableStateOf(false) }; var acknowledge by rememberSaveable { mutableStateOf(false) }
        val previous = d.readings.filter { it.meterId == m.id }.maxByOrNull { it.created }
        val warning = number(value)?.let { meterWarning(m, previous, it, reset) }
        Form(m.name, number(value)?.let { it >= 0 } == true && !busy && (warning == null || acknowledge) && (!reset || note.isNotBlank()), { reading = null }, {
            save(d.copy(readings = d.readings.filterNot { it.id == recordId } + Reading(id = recordId, assetId = m.assetId, label = m.name, value = number(value)!!, unit = m.unit, note = note, meterId = m.id, reset = reset), work = d.work.copy(lastMeter = m.id))) { reading = null }
        }) { Text("${assetName(d, m.assetId)} · ${m.unit}"); previous?.let { Text("Vorher: ${it.value} ${it.unit}") }
            Field(value, { value = it; acknowledge = false }, "Zählerstand", numeric = true)
            Row { Checkbox(reset, { reset = it; acknowledge = false }, enabled = !busy); Text("Zählerwechsel / neuer Ausgangsstand") }
            Field(note, { note = it }, if (reset) "Grund / neue Zählernummer *" else "Hinweis", 2)
            warning?.let { Text(it, color = Warning); Row { Checkbox(acknowledge, { acknowledge = it }, enabled = !busy); Text("Wert geprüft, trotzdem speichern") } } }
    }
}
@Composable private fun MeterEditor(old: Meter?, d: Data, close: () -> Unit, save: (Meter) -> Unit) {
    val recordId = rememberSaveable { old?.id ?: newId() }
    var asset by rememberSaveable { mutableStateOf(old?.assetId ?: d.assets.firstOrNull()?.id.orEmpty()) }; var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var unit by rememberSaveable { mutableStateOf(old?.unit ?: "kWh") }; var note by rememberSaveable { mutableStateOf(old?.note ?: "") }
    var limit by rememberSaveable { mutableStateOf(old?.maxDelta?.toString() ?: "") }
    Form("Zähler", (limit.isBlank() || number(limit)?.let { it > 0 } == true) && asset.isNotEmpty() && name.isNotBlank() && unit.isNotBlank(), close, { save(Meter(recordId, asset, name.trim(), unit.trim(), note.trim(), number(limit))) }) {
        if (old == null) Picker("Anlage", d.assets.map { it.id to it.name }, asset) { asset = it } else Text("Anlage: ${assetName(d, asset)}")
        Field(name, { name = it }, "Zählername / Nummer *"); Field(unit, { unit = it }, "Einheit *"); Field(note, { note = it }, "Hinweise / Nachkommastellen", 3); Field(limit, { limit = it }, "Eigene Prüfgrenze: Differenz pro Ablesung (optional)", numeric = true)
    }
}
@Composable private fun CalendarScreen(d: Data, busy: Boolean, save: DataSaver, initialId: String?, consumed: (String?) -> Unit) {
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }; var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var all by rememberSaveable { mutableStateOf(false) }; var editing by rememberSaveable { mutableStateOf(false) }; var selected by rememberSaveable { mutableStateOf<Appointment?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var remove by rememberSaveable { mutableStateOf<Appointment?>(null) }
    LaunchedEffect(initialId) {
        initialId?.let { id -> d.work.appointments.find { it.id == id }?.let { event ->
            selected = event; editing = true
            appointmentTime(event.start)?.toLocalDate()?.let { day = it; month = YearMonth.from(it) }
        }; consumed(null) }
    }
    Section("Kraftwerkkalender")
    TextButton(onClick = { (context as? MainActivity)?.enableReminders() }) { Text("Lokale Benachrichtigungen erlauben") }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)), modifier = Modifier.padding(top = 12.dp))
        TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
    }
    Row { listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { Text(it, modifier = Modifier.weight(1f), color = Muted) } }
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = List(offset) { 0 } + (1..month.lengthOfMonth()).toList()
    cells.chunked(7).forEach { week -> Row {
        (0..6).forEach { index -> val number = week.getOrElse(index) { 0 }
            if (number == 0) Spacer(Modifier.weight(1f).height(48.dp)) else {
                val date = month.atDay(number); val hasEvent = d.work.appointments.any { it.status == "Geplant" && occurrences(it, date, date).isNotEmpty() }
                TextButton(onClick = { day = date; all = false }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.textButtonColors(
                    containerColor = if (date == day) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (date == day) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)) {
                    Text("$number${if (hasEvent) "•" else ""}", fontWeight = if (date == day) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    } }
    TextButton(onClick = { all = !all }) { Text(if (all) "Nur gewählten Tag zeigen" else "Alle kommenden Termine zeigen") }
    Button(onClick = { selected = null; editing = true }, enabled = !busy) { Text("+ Termin") }
    val events = d.work.appointments.flatMap { occurrences(it, if (all) LocalDate.now() else day, if (all) LocalDate.now().plusMonths(6) else day) }.sortedBy { appointmentTime(it.start) }
    if (events.isEmpty()) Hint("Keine Termine für diese Auswahl.")
    PagedRecords(events, "$day:$all", { "${it.id}:${it.start}" }) { event -> Panel {
        Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("${event.start} · ${event.minutes} min", color = Muted); StatusBadge(event.status, appointmentTone(event.status))
        if (event.company.isNotBlank()) Text("Fremdfirma: ${event.company}")
        if (event.responsible.isNotBlank()) Text("Zuständig: ${event.responsible}")
        if (event.contact.isNotBlank()) Text("Kontakt: ${event.contact}")
        if (event.assetId.isNotEmpty()) Text(assetName(d, event.assetId), color = Muted)
        if (event.note.isNotBlank()) Text(event.note)
        TextButton(onClick = {
            val begin = appointmentTime(event.start)!!.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_INSERT).setData(android.provider.CalendarContract.Events.CONTENT_URI)
                .putExtra(android.provider.CalendarContract.Events.TITLE, event.title).putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "${event.company}\n${event.responsible}\n${event.note}")
                .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin).putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, begin + event.minutes * 60000L)) }
        }) { Text("Kopie im Systemkalender öffnen") }
        TextButton(onClick = { selected = d.work.appointments.single { it.id == event.id }; editing = true }, enabled = !busy) { Text("Termin bearbeiten") }; TextButton(onClick = { remove = event }, enabled = !busy) { Text("Termin / Serie löschen", color = MaterialTheme.colorScheme.error) }
    } }
    Hint("Systemkalender-Kopien können je nach Kalenderkonto synchronisiert werden; keine automatische Übertragung. Wiederholungen werden für die nächsten 6 Monate angezeigt. Bearbeiten ändert die gesamte Serie. Erinnerungen sind lokal und können durch Android verzögert werden.")
    remove?.let { event -> ConfirmRemoval("Termin / Serie löschen?", "Alle Wiederholungen dieses Termins werden entfernt. Kopien im Systemkalender bleiben unverändert.", busy, { remove = null }) { save(d.copy(work = d.work.copy(appointments = d.work.appointments.filterNot { it.id == event.id }))) { remove = null } } }
    if (editing) AppointmentEditor(selected, day, d, { editing = false }) { e -> save(d.copy(work = d.work.copy(appointments = d.work.appointments.filterNot { it.id == e.id } + e))) { editing = false } }
}
@Composable private fun AppointmentEditor(old: Appointment?, date: LocalDate, d: Data, close: () -> Unit, save: (Appointment) -> Unit) {
    val recordId = rememberSaveable { old?.id ?: newId() }
    var title by rememberSaveable { mutableStateOf(old?.title ?: "") }; var start by rememberSaveable { mutableStateOf(old?.start ?: formatAppointment(date.atTime(8, 0))) }
    var minutes by rememberSaveable { mutableStateOf((old?.minutes ?: 60).toString()) }; var company by rememberSaveable { mutableStateOf(old?.company ?: "") }
    var contact by rememberSaveable { mutableStateOf(old?.contact ?: "") }; var person by rememberSaveable { mutableStateOf(old?.responsible ?: "") }
    var asset by rememberSaveable { mutableStateOf(old?.assetId ?: "") }; var note by rememberSaveable { mutableStateOf(old?.note ?: "") }; var status by rememberSaveable { mutableStateOf(old?.status ?: "Geplant") }
    var repeat by rememberSaveable { mutableStateOf(old?.repeat ?: "Nie") }; var remind by rememberSaveable { mutableIntStateOf(old?.remind ?: -1) }
    Form("Termin", title.isNotBlank() && appointmentTime(start) != null && minutes.toIntOrNull()?.let { it in 1..10080 } == true, close, {
        save(Appointment(recordId, title.trim(), start.trim(), minutes.toInt(), company.trim(), contact.trim(), person.trim(), asset, note.trim(), status, repeat, remind))
    }) { Field(title, { title = it }, "Grund / Arbeit *"); Field(start, { start = it }, "Datum und Uhrzeit (TT.MM.JJJJ HH:MM)"); Field(minutes, { minutes = it }, "Dauer in Minuten", numeric = true)
        Field(company, { company = it }, "Fremdfirma"); Field(contact, { contact = it }, "Ansprechpartner / Kontakt"); Field(person, { person = it }, "Zuständiger Mitarbeiter")
        Picker("Anlage", listOf("" to "Ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }; Field(note, { note = it }, "Vorbereitung / Hinweise", 3)
        Choices(listOf("Geplant", "Erledigt", "Abgesagt"), status) { status = it }
        Picker("Wiederholung", repetitions.map { it to it }, repeat) { repeat = it }
        Picker("Erinnerung", listOf("-1" to "Keine", "0" to "Zum Termin", "15" to "15 Minuten vorher", "60" to "1 Stunde vorher", "1440" to "1 Tag vorher"), remind.toString()) { remind = it.toInt() }
    }
}

val LocalGuideStepAction = staticCompositionLocalOf<((() -> Unit)?) -> Unit> { {} }

@Composable private fun GuideScreen(d: Data, busy: Boolean, save: DataSaver, photo: (PhotoTarget) -> Unit, selected: String?, select: (String?) -> Unit) {
    var editor by rememberSaveable { mutableStateOf(false) }
    var stepEditor by rememberSaveable { mutableStateOf(false) }; var selectedStep by rememberSaveable { mutableStateOf<GuideStep?>(null) }
    var deletion by rememberSaveable { mutableStateOf<GuideStep?>(null) }
    var deleteGuide by rememberSaveable { mutableStateOf(false) }
    val guide = d.work.guides.find { it.id == selected }
    val setNextAction = LocalGuideStepAction.current
    DisposableEffect(guide?.id, guide?.steps?.size) {
        setNextAction(if (guide != null && guide.steps.size < 100) ({ selectedStep = null; stepEditor = true }) else null)
        onDispose { setNextAction(null) }
    }
    fun update(next: Guide, done: () -> Unit = {}) {
        val previous = d.work.guides.find { it.id == next.id }
        val contentChanged = previous != null && (previous.title != next.title || previous.steps != next.steps || previous.assetId != next.assetId)
        save(d.copy(work = d.work.copy(guides = d.work.guides.filterNot { it.id == next.id } + next.copy(revision = (previous?.revision ?: 0) + 1, checked = if (contentChanged) "" else next.checked))), done)
    }
    var query by rememberSaveable { mutableStateOf("") }
    Section("Eigene Anleitungen")
    if (guide == null) {
        Hint("Schrittfolgen für wiederkehrende Arbeiten, optional einer Anlage zugeordnet. Eigene Notizen ersetzen keine freigegebenen Betriebsanweisungen.")
        Button(onClick = { select(null); editor = true }, enabled = !busy) { Text("+ Anleitung") }
        Field(query, { query = it }, "Anleitungen suchen")
        val results = d.work.guides.filter { g -> listOf(g.title, assetName(d, g.assetId)) .any { it.contains(query, true) } || g.steps.any { "${it.title} ${it.body}".contains(query, true) } }.sortedBy { it.title.lowercase() }
        PagedRecords(results, query, { it.id }) { g -> Panel {
            Text(g.title, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(if (g.assetId.isBlank()) "Allgemeine Anleitung" else assetName(d, g.assetId), color = Muted)
            Text("${g.steps.size} Schritte"); TextButton(onClick = { select(g.id) }) { Text("Anleitung öffnen") }
        } }
    } else {
        Text(guide.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (guide.assetId.isNotBlank()) RecordLink("Anlagen", guide.assetId, "Anlage: ${assetName(d, guide.assetId)}")
        d.entries.filter { guide.id in it.guideIds }.forEach { RecordLink("Vorgang", it.id, "Verwendet bei: ${it.title}") }
        Text("Version ${guide.revision}", color = Muted)
        StatusBadge("Geprüft: ${guide.checked.ifBlank { "Noch nicht" }}", if (guide.checked.isBlank()) StatusTone.NEUTRAL else StatusTone.SUCCESS)
        guide.steps.forEachIndexed { index, step -> Panel {
            Text("${index + 1}. ${step.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(step.body); StoredPhoto(step.image)
            Row { TextButton(onClick = { selectedStep = step; stepEditor = true }, enabled = !busy) { Text("Bearbeiten") }
                TextButton(onClick = { photo(PhotoTarget("guide", step.id)) }, enabled = !busy && (step.image.isNotEmpty() || imageValues(d).count { it.isNotEmpty() } < MAX_IMAGES)) { Text(if (step.image.isEmpty()) "+ Foto" else "Foto ersetzen") } }
            Row { TextButton(onClick = { val steps = guide.steps.toMutableList(); val previous = steps[index - 1]; steps[index - 1] = step; steps[index] = previous; update(guide.copy(steps = steps)) }, enabled = !busy && index > 0) { Text("Nach oben") }
                if (step.image.isNotEmpty()) TextButton(onClick = { update(guide.copy(steps = guide.steps.map { if (it.id == step.id) it.copy(image = "") else it })) }, enabled = !busy) { Text("Bild entfernen", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { deletion = step }, enabled = !busy) { Text("Löschen", color = MaterialTheme.colorScheme.error) } }
        } }
        Section("Anleitungseinstellungen")
        TextButton(onClick = { update(guide.copy(checked = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.uuuu")))) }, enabled = !busy) { Text("Heute inhaltlich geprüft") }
        TextButton(onClick = { editor = true }, enabled = !busy) { Text("Titel / Anlage bearbeiten") }; TextButton(onClick = { deleteGuide = true }, enabled = !busy) { Text("Anleitung löschen", color = MaterialTheme.colorScheme.error) }
        if (guide.steps.isEmpty()) Hint("Füge oben den ersten Arbeitsschritt hinzu.")
        Hint("Bilder: max. 512 KiB pro Bild, insgesamt höchstens 500 Bilder für Anlagen, Anleitungen und Bestelllisten. Fotos erst nach dem Speichern des Schritts hinzufügen.")
    }
    if (deleteGuide && guide != null) ConfirmRemoval("Anleitung löschen?", "Alle Schritte und Bilder dieser Anleitung werden entfernt.", busy, { deleteGuide = false }) { save(d.copy(entries = d.entries.map { it.copy(guideIds = it.guideIds - guide.id) }, work = d.work.copy(guides = d.work.guides.filterNot { it.id == guide.id }))) { select(null); deleteGuide = false } }
    if (editor) {
        val recordId = rememberSaveable { guide?.id ?: newId() }
        var title by rememberSaveable { mutableStateOf(guide?.title ?: "") }; var asset by rememberSaveable { mutableStateOf(guide?.assetId ?: "") }
        Form("Anleitung", title.isNotBlank(), { editor = false }, { val next = (guide ?: Guide(id = recordId, title = title.trim())).copy(title = title.trim(), assetId = asset); update(next) { select(next.id); editor = false } }) {
            Field(title, { title = it }, "Titel *"); Picker("Anlage", listOf("" to "Allgemein / ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }
        }
    }
    if (stepEditor && guide != null) {
        val recordId = rememberSaveable { selectedStep?.id ?: newId() }
        var title by rememberSaveable { mutableStateOf(selectedStep?.title ?: "") }; var body by rememberSaveable { mutableStateOf(selectedStep?.body ?: "") }
        Form("Arbeitsschritt", title.isNotBlank(), { stepEditor = false }, {
            val next = GuideStep(recordId, title.trim(), body.trim(), selectedStep?.image ?: "")
            update(guide.copy(steps = if (guide.steps.none { it.id == next.id }) guide.steps + next else guide.steps.map { if (it.id == next.id) next else it })) { stepEditor = false }
        }) { Field(title, { title = it }, "Schritt *"); Field(body, { body = it }, "Beschreibung / Voraussetzungen / Kontrolle", 6) }
    }
    deletion?.let { step -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text("Schritt löschen?", color = MaterialTheme.colorScheme.error) }, text = { Text("${step.title} samt Bild wird aus der Anleitung entfernt.") },
        confirmButton = { TextButton(onClick = { guide?.let { update(it.copy(steps = it.steps.filterNot { x -> x.id == step.id })) { deletion = null } } }, enabled = !busy) { Text("Löschen", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text("Abbrechen") } }) }
}

@Composable private fun OrderScreen(d: Data, busy: Boolean, save: DataSaver, photo: (PhotoTarget) -> Unit, mail: (PartsOrder) -> Unit, selected: String?, select: (String?) -> Unit) {
    var editor by rememberSaveable { mutableStateOf(false) }
    var itemEditor by rememberSaveable { mutableStateOf(false) }; var selectedItem by rememberSaveable { mutableStateOf<OrderItem?>(null) }
    var preview by rememberSaveable { mutableStateOf(false) }; var deletion by rememberSaveable { mutableStateOf<OrderItem?>(null) }
    var deleteOrder by rememberSaveable { mutableStateOf(false) }
    val order = d.work.orders.find { it.id == selected }
    fun update(next: PartsOrder, done: () -> Unit = {}) = save(d.copy(work = d.work.copy(orders = d.work.orders.filterNot { it.id == next.id } + next)), done)
    var query by rememberSaveable { mutableStateOf("") }; var statusFilter by rememberSaveable { mutableStateOf("Offen") }
    Section("Bestelllisten")
    if (order == null) {
        Button(onClick = { select(null); editor = true }, enabled = !busy) { Text("+ Bestellliste") }
        Field(query, { query = it }, "Bestellungen, Artikel oder Anlagen suchen")
        Choices(listOf("Offen", "Alle", "Erledigt"), statusFilter) { statusFilter = it }
        val results = d.work.orders.filter { o -> (statusFilter == "Alle" || (statusFilter == "Erledigt") == (o.status in listOf("Geliefert", "Abgesagt"))) && ("${o.title} ${o.context} ${assetName(d, o.assetId)}".contains(query, true) || o.items.any { "${it.name} ${it.reason} ${assetName(d, it.assetId)}".contains(query, true) }) }.reversed()
        PagedRecords(results, "$query:$statusFilter", { it.id }) { o -> Panel { Text(o.title, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("${o.items.size} Positionen", color = Muted); StatusBadge(o.status, orderTone(o.status))
            TextButton(onClick = { select(o.id) }) { Text("Öffnen / weiter erfassen") } } }
    } else {
        Text(order.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        StatusBadge(order.status, orderTone(order.status), Modifier.padding(vertical = 8.dp))
        Panel {
            Text("Grundinformationen · nur lokal", fontWeight = FontWeight.Bold)
            if (order.assetId.isNotBlank()) RecordLink("Anlagen", order.assetId, assetName(d, order.assetId))
            d.entries.find { it.id == order.entryId }?.let { RecordLink("Vorgang", it.id, "Vorgang: ${it.title}") }
            if (order.context.isNotBlank()) Text(order.context)
            TextButton(onClick = { editor = true }, enabled = !busy) { Text("Grundinformationen bearbeiten") }
        }; TextButton(onClick = { deleteOrder = true }, enabled = !busy) { Text("Bestellliste löschen", color = MaterialTheme.colorScheme.error) }
        order.items.forEachIndexed { index, item -> Panel {
            Text("${index + 1}. ${item.quantity} ${item.unit} · ${item.name}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (item.assetId.isNotBlank()) Text("Nur lokal: ${assetName(d, item.assetId)}", color = Muted)
            if (item.reason.isNotBlank()) Text(item.reason); StoredPhoto(item.image)
            Row { TextButton(onClick = { selectedItem = item; itemEditor = true }, enabled = !busy) { Text("Bearbeiten") }
                TextButton(onClick = { photo(PhotoTarget("order", item.id)) }, enabled = !busy && (item.image.isNotEmpty() || imageValues(d).count { it.isNotEmpty() } < MAX_IMAGES)) { Text(if (item.image.isEmpty()) "+ Foto" else "Foto ersetzen") } }
            Row { if (item.image.isNotEmpty()) TextButton(onClick = { update(order.copy(items = order.items.map { if (it.id == item.id) it.copy(image = "") else it })) }, enabled = !busy) { Text("Bild entfernen", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { deletion = item }, enabled = !busy) { Text("Entfernen", color = MaterialTheme.colorScheme.error) } }
        } }
        Button(onClick = { selectedItem = null; itemEditor = true }, enabled = !busy) { Text("+ Nächstes Teil") }
        OutlinedButton(onClick = { preview = true }, enabled = order.items.isNotEmpty() && !busy) { Text("Bestellung als E-Mail vorbereiten") }
        Picker("Bestellstatus", orderStatuses.map { it to it }, order.status) { if (!busy) update(order.copy(status = it, completed = it == "Geliefert" || it == "Abgesagt")) }
        if (order.delivery.isNotBlank()) Text("Lieferdatum: ${order.delivery}")
        Hint("Die Anlagenzuordnung bleibt lokal. Nur Bezeichnung, Menge, Zweck und hinzugefügte Bilder werden übergeben. Versand erfolgt ausschließlich durch dich in der Mail-App.")
    }
    if (deleteOrder && order != null) ConfirmRemoval("Bestellliste löschen?", "Alle Positionen und Bilder dieser lokalen Liste werden entfernt. Bereits versandte E-Mails bleiben unverändert.", busy, { deleteOrder = false }) { save(d.copy(work = d.work.copy(orders = d.work.orders.filterNot { it.id == order.id }))) { select(null); deleteOrder = false } }
    if (editor) {
        val recordId = rememberSaveable { order?.id ?: newId() }
        var title by rememberSaveable { mutableStateOf(order?.title ?: "") }; var recipient by rememberSaveable { mutableStateOf(order?.recipient ?: "") }; var delivery by rememberSaveable { mutableStateOf(order?.delivery ?: "") }
        var asset by rememberSaveable { mutableStateOf(order?.assetId ?: "") }; var entry by rememberSaveable { mutableStateOf(order?.entryId ?: "") }; var context by rememberSaveable { mutableStateOf(order?.context ?: "") }
        Form("Bestellliste", (delivery.isBlank() || parseServiceDate(delivery) != null) && title.isNotBlank() && (recipient.isBlank() || (recipient.contains('@') && !recipient.contains('\n'))), { editor = false }, {
            val next = (order ?: PartsOrder(id = recordId, title = title.trim())).copy(title = title.trim(), recipient = recipient.trim(), delivery = delivery.trim(), assetId = asset, entryId = entry, context = context.trim())
            update(next) { select(next.id); editor = false }
        }) {
            Picker("Anlage (nur lokal)", listOf("" to "Ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it; if (d.entries.find { e -> e.id == entry }?.assetId != asset) entry = "" }
            Picker("Störung / Arbeit (nur lokal)", listOf("" to "Ohne Vorgang") + d.entries.filter { asset.isBlank() || it.assetId == asset }.map { it.id to it.title }, entry) { entry = it; d.entries.find { e -> e.id == it }?.let { e -> asset = e.assetId } }
            Field(context, { context = it }, "Hintergrund / Grundinformationen (nur lokal)", 3)
            Field(title, { title = it }, "Titel *"); Field(recipient, { recipient = it }, "E-Mail Teamleiter (optional)"); Field(delivery, { delivery = it }, "Lieferdatum (TT.MM.JJJJ, optional)"); Hint("Leer lassen, wenn du den Empfänger erst in der E-Mail-App auswählen möchtest.") }
    }
    if (itemEditor && order != null) {
        val recordId = rememberSaveable { selectedItem?.id ?: newId() }
        var name by rememberSaveable { mutableStateOf(selectedItem?.name ?: "") }; var quantity by rememberSaveable { mutableStateOf(selectedItem?.quantity ?: "1") }
        var unit by rememberSaveable { mutableStateOf(selectedItem?.unit ?: "Stück") }; var reason by rememberSaveable { mutableStateOf(selectedItem?.reason ?: "") }; var asset by rememberSaveable { mutableStateOf(selectedItem?.assetId ?: order.assetId) }
        Form("Bestellposition", name.isNotBlank() && unit.isNotBlank() && number(quantity)?.let { it > 0 } == true, { itemEditor = false }, {
            val next = OrderItem(recordId, name.trim(), quantity.trim(), unit.trim(), reason.trim(), asset, selectedItem?.image ?: "")
            update(order.copy(items = if (order.items.none { it.id == next.id }) order.items + next else order.items.map { if (it.id == next.id) next else it })) { itemEditor = false }
        }) {
            if (selectedItem == null) Picker("Vorhandenes Teil übernehmen", d.work.orders.flatMap { it.items }.distinctBy { it.name }.map { it.id to it.name }, "") { id -> d.work.orders.flatMap { it.items }.find { it.id == id }?.let { name = it.name; unit = it.unit; reason = it.reason; asset = it.assetId } }
            Field(name, { name = it }, "Teil / Ausrüstung / Artikelnummer *"); Field(quantity, { quantity = it }, "Menge *", numeric = true); Field(unit, { unit = it }, "Einheit *")
            Field(reason, { reason = it }, "Zweck / technische Angaben für Bestellung", 3); Picker("Anlage (nur lokal)", listOf("" to "Ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }
        }
    }
    if (preview && order != null) Form("E-Mail-Vorschau", !busy, { preview = false }, { preview = false; mail(order) }, confirmLabel = "E-Mail-App öffnen") {
        Text("An: ${order.recipient.ifBlank { "Auswahl in E-Mail-App" }}", color = Muted)
        Hint("Text und ${order.items.count { it.image.isNotEmpty() }} Bilder werden unverschlüsselt an die gewählte Versand-App übergeben. Der nächste Button öffnet den Entwurf, er sendet nichts.")
        Text(orderText(order))
    }
    deletion?.let { item -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text("Position entfernen?", color = MaterialTheme.colorScheme.error) }, text = { Text(item.name) },
        confirmButton = { TextButton(onClick = { order?.let { update(it.copy(items = it.items.filterNot { x -> x.id == item.id })) { deletion = null } } }, enabled = !busy) { Text("Entfernen", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text("Abbrechen") } }) }
}
