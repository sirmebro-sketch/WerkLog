package de.werklog.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable internal fun WorkTools(mode: String, data: Data, busy: Boolean, back: () -> Unit, save: (Data) -> Unit, photo: (PhotoTarget) -> Unit, mailOrder: (PartsOrder) -> Unit) {
    TextButton(onClick = back) { Text("‹ Zurück") }
    when (mode) {
        "Zähler" -> MeterScreen(data, busy, save, photo)
        "Kalender" -> CalendarScreen(data, busy, save)
        "Anleitungen" -> GuideScreen(data, busy, save, photo)
        "Bestellungen" -> OrderScreen(data, busy, save, photo, mailOrder)
    }
}
@Composable private fun MeterScreen(d: Data, busy: Boolean, save: (Data) -> Unit, photo: (PhotoTarget) -> Unit) {
    var edit by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf<Meter?>(null) }
    var reading by remember { mutableStateOf<Meter?>(null) }
    Section("Zähler & Ablesungen")
    Hint("Zähler fest einer Anlage zuordnen. Fotoerkennung läuft offline; jeder Wert wird vor dem Speichern geprüft. Fotos bleiben nicht im Archiv.")
    Button(onClick = { selected = null; edit = true }, enabled = !busy && d.assets.isNotEmpty()) { Text("+ Zähler anlegen") }
    if (d.assets.isEmpty()) Hint("Bitte zuerst eine Anlage anlegen.")
    d.work.meters.forEach { meter -> Panel {
        Text(meter.name, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(assetName(d, meter.assetId), color = Mint)
        val history = d.readings.filter { it.meterId == meter.id }.sortedByDescending { it.created }
        history.firstOrNull()?.let { Text("Zuletzt: ${it.value} ${it.unit} · ${stamp(it.created)}") }
        if (meter.note.isNotBlank()) Text(meter.note)
        Row { TextButton(onClick = { photo(PhotoTarget("meter", meter.id)) }, enabled = !busy) { Text("Zähler fotografieren") }
            TextButton(onClick = { reading = meter }, enabled = !busy) { Text("Manuell") } }
        TextButton(onClick = { selected = meter; edit = true }, enabled = !busy) { Text("Zähler bearbeiten") }
        history.take(5).forEach { Text("${stamp(it.created)} · ${it.value} ${it.unit}", color = Muted, fontSize = 12.sp) }
    } }
    if (edit) MeterEditor(selected, d, { edit = false }) { meter -> save(d.copy(work = d.work.copy(meters = d.work.meters.filterNot { it.id == meter.id } + meter))); edit = false }
    reading?.let { m -> var value by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        Form(m.name, number(value)?.let { it >= 0 } == true && !busy, { reading = null }, {
            save(d.copy(readings = d.readings + Reading(assetId = m.assetId, label = m.name, value = number(value)!!, unit = m.unit, note = note, meterId = m.id))); reading = null
        }) { Text("${assetName(d, m.assetId)} · ${m.unit}"); Field(value, { value = it }, "Zählerstand", numeric = true); Field(note, { note = it }, "Hinweis / Zählertausch", 2) }
    }
}
@Composable private fun MeterEditor(old: Meter?, d: Data, close: () -> Unit, save: (Meter) -> Unit) {
    var asset by remember { mutableStateOf(old?.assetId ?: d.assets.firstOrNull()?.id.orEmpty()) }; var name by remember { mutableStateOf(old?.name ?: "") }
    var unit by remember { mutableStateOf(old?.unit ?: "kWh") }; var note by remember { mutableStateOf(old?.note ?: "") }
    Form("Zähler", asset.isNotEmpty() && name.isNotBlank() && unit.isNotBlank(), close, { save(Meter(old?.id ?: newId(), asset, name.trim(), unit.trim(), note.trim())) }) {
        if (old == null) Picker("Anlage", d.assets.map { it.id to it.name }, asset) { asset = it } else Text("Anlage: ${assetName(d, asset)}")
        Field(name, { name = it }, "Zählername / Nummer *"); Field(unit, { unit = it }, "Einheit *"); Field(note, { note = it }, "Hinweise / Nachkommastellen", 3)
    }
}
@Composable private fun CalendarScreen(d: Data, busy: Boolean, save: (Data) -> Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }; var day by remember { mutableStateOf(LocalDate.now()) }
    var all by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf<Appointment?>(null) }
    Section("Kraftwerkkalender")
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
                val date = month.atDay(number); val hasEvent = d.work.appointments.any { appointmentTime(it.start)?.toLocalDate() == date && it.status == "Geplant" }
                TextButton(onClick = { day = date; all = false }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), contentPadding = PaddingValues(0.dp)) {
                    Text("$number${if (hasEvent) "•" else ""}", color = if (date == day) Amber else Mint, fontWeight = if (date == day) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    } }
    TextButton(onClick = { all = !all }) { Text(if (all) "Nur gewählten Tag zeigen" else "Alle kommenden Termine zeigen") }
    Button(onClick = { selected = null; editing = true }, enabled = !busy) { Text("+ Termin") }
    val events = d.work.appointments.filter { val date = appointmentTime(it.start)!!.toLocalDate(); if (all) date >= LocalDate.now() else date == day }.sortedBy { appointmentTime(it.start) }
    if (events.isEmpty()) Hint("Keine Termine für diese Auswahl.")
    events.forEach { event -> Panel {
        Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("${event.start} · ${event.minutes} min · ${event.status}", color = Mint)
        if (event.company.isNotBlank()) Text("Fremdfirma: ${event.company}")
        if (event.responsible.isNotBlank()) Text("Zuständig: ${event.responsible}")
        if (event.contact.isNotBlank()) Text("Kontakt: ${event.contact}")
        if (event.assetId.isNotEmpty()) Text(assetName(d, event.assetId), color = Muted)
        if (event.note.isNotBlank()) Text(event.note)
        TextButton(onClick = { selected = event; editing = true }, enabled = !busy) { Text("Termin bearbeiten") }
    } }
    Hint("Lokaler Kalender. Keine Einladungen, Systemkalender-Synchronisation oder Hintergrund-Erinnerungen.")
    if (editing) AppointmentEditor(selected, day, d, { editing = false }) { e -> save(d.copy(work = d.work.copy(appointments = d.work.appointments.filterNot { it.id == e.id } + e))); editing = false }
}
@Composable private fun AppointmentEditor(old: Appointment?, date: LocalDate, d: Data, close: () -> Unit, save: (Appointment) -> Unit) {
    var title by remember { mutableStateOf(old?.title ?: "") }; var start by remember { mutableStateOf(old?.start ?: formatAppointment(date.atTime(8, 0))) }
    var minutes by remember { mutableStateOf((old?.minutes ?: 60).toString()) }; var company by remember { mutableStateOf(old?.company ?: "") }
    var contact by remember { mutableStateOf(old?.contact ?: "") }; var person by remember { mutableStateOf(old?.responsible ?: "") }
    var asset by remember { mutableStateOf(old?.assetId ?: "") }; var note by remember { mutableStateOf(old?.note ?: "") }; var status by remember { mutableStateOf(old?.status ?: "Geplant") }
    Form("Termin", title.isNotBlank() && appointmentTime(start) != null && minutes.toIntOrNull()?.let { it in 1..10080 } == true, close, {
        save(Appointment(old?.id ?: newId(), title.trim(), start.trim(), minutes.toInt(), company.trim(), contact.trim(), person.trim(), asset, note.trim(), status))
    }) { Field(title, { title = it }, "Grund / Arbeit *"); Field(start, { start = it }, "Datum und Uhrzeit (TT.MM.JJJJ HH:MM)"); Field(minutes, { minutes = it }, "Dauer in Minuten", numeric = true)
        Field(company, { company = it }, "Fremdfirma"); Field(contact, { contact = it }, "Ansprechpartner / Kontakt"); Field(person, { person = it }, "Zuständiger Mitarbeiter")
        Picker("Anlage", listOf("" to "Ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }; Field(note, { note = it }, "Vorbereitung / Hinweise", 3)
        Choices(listOf("Geplant", "Erledigt", "Abgesagt"), status) { status = it }
    }
}

@Composable private fun GuideScreen(d: Data, busy: Boolean, save: (Data) -> Unit, photo: (PhotoTarget) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }; var editor by remember { mutableStateOf(false) }
    var stepEditor by remember { mutableStateOf(false) }; var selectedStep by remember { mutableStateOf<GuideStep?>(null) }
    var deletion by remember { mutableStateOf<GuideStep?>(null) }
    val guide = d.work.guides.find { it.id == selected }
    fun update(next: Guide) = save(d.copy(work = d.work.copy(guides = d.work.guides.filterNot { it.id == next.id } + next)))
    Section("Eigene Anleitungen")
    if (guide == null) {
        Hint("Schrittfolgen für wiederkehrende Arbeiten, optional einer Anlage zugeordnet. Eigene Notizen ersetzen keine freigegebenen Betriebsanweisungen.")
        Button(onClick = { selected = null; editor = true }, enabled = !busy) { Text("+ Anleitung") }
        d.work.guides.forEach { g -> Panel {
            Text(g.title, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(if (g.assetId.isBlank()) "Allgemeine Anleitung" else assetName(d, g.assetId), color = Mint)
            Text("${g.steps.size} Schritte"); TextButton(onClick = { selected = g.id }) { Text("Anleitung öffnen") }
        } }
    } else {
        TextButton(onClick = { selected = null }) { Text("‹ Alle Anleitungen") }
        Text(guide.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { editor = true }, enabled = !busy) { Text("Titel / Anlage bearbeiten") }
        guide.steps.forEachIndexed { index, step -> Panel {
            Text("${index + 1}. ${step.title}", fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(step.body); StoredPhoto(step.image)
            Row { TextButton(onClick = { selectedStep = step; stepEditor = true }, enabled = !busy) { Text("Bearbeiten") }
                TextButton(onClick = { photo(PhotoTarget("guide", step.id)) }, enabled = !busy && (step.image.isNotEmpty() || imageCount(d.work) < MAX_IMAGES)) { Text(if (step.image.isEmpty()) "+ Foto" else "Foto ersetzen") } }
            Row { TextButton(onClick = { val steps = guide.steps.toMutableList(); val previous = steps[index - 1]; steps[index - 1] = step; steps[index] = previous; update(guide.copy(steps = steps)) }, enabled = !busy && index > 0) { Text("Nach oben") }
                if (step.image.isNotEmpty()) TextButton(onClick = { update(guide.copy(steps = guide.steps.map { if (it.id == step.id) it.copy(image = "") else it })) }, enabled = !busy) { Text("Bild entfernen") }
                TextButton(onClick = { deletion = step }, enabled = !busy) { Text("Löschen") } }
        } }
        Button(onClick = { selectedStep = null; stepEditor = true }, enabled = !busy && guide.steps.size < 100) { Text("+ Nächster Schritt") }
        Hint("Bilder: max. 160 KB pro Bild, insgesamt höchstens 30 Bilder für Anleitungen und Bestelllisten. Fotos erst nach dem Speichern des Schritts hinzufügen.")
    }
    if (editor) {
        var title by remember { mutableStateOf(guide?.title ?: "") }; var asset by remember { mutableStateOf(guide?.assetId ?: "") }
        Form("Anleitung", title.isNotBlank(), { editor = false }, { val next = Guide(guide?.id ?: newId(), title.trim(), asset, guide?.steps ?: emptyList()); update(next); selected = next.id; editor = false }) {
            Field(title, { title = it }, "Titel *"); Picker("Anlage", listOf("" to "Allgemein / ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }
        }
    }
    if (stepEditor && guide != null) {
        var title by remember { mutableStateOf(selectedStep?.title ?: "") }; var body by remember { mutableStateOf(selectedStep?.body ?: "") }
        Form("Arbeitsschritt", title.isNotBlank(), { stepEditor = false }, {
            val next = GuideStep(selectedStep?.id ?: newId(), title.trim(), body.trim(), selectedStep?.image ?: "")
            update(guide.copy(steps = if (selectedStep == null) guide.steps + next else guide.steps.map { if (it.id == next.id) next else it })); stepEditor = false
        }) { Field(title, { title = it }, "Schritt *"); Field(body, { body = it }, "Beschreibung / Voraussetzungen / Kontrolle", 6) }
    }
    deletion?.let { step -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text("Schritt löschen?") }, text = { Text("${step.title} samt Bild wird aus der Anleitung entfernt.") },
        confirmButton = { TextButton(onClick = { guide?.let { update(it.copy(steps = it.steps.filterNot { x -> x.id == step.id })) }; deletion = null }, enabled = !busy) { Text("Löschen") } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text("Abbrechen") } }) }
}

@Composable private fun OrderScreen(d: Data, busy: Boolean, save: (Data) -> Unit, photo: (PhotoTarget) -> Unit, mail: (PartsOrder) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }; var editor by remember { mutableStateOf(false) }
    var itemEditor by remember { mutableStateOf(false) }; var selectedItem by remember { mutableStateOf<OrderItem?>(null) }
    var preview by remember { mutableStateOf(false) }; var deletion by remember { mutableStateOf<OrderItem?>(null) }
    val order = d.work.orders.find { it.id == selected }
    fun update(next: PartsOrder) = save(d.copy(work = d.work.copy(orders = d.work.orders.filterNot { it.id == next.id } + next)))
    Section("Bestelllisten")
    if (order == null) {
        Button(onClick = { selected = null; editor = true }, enabled = !busy) { Text("+ Bestellliste") }
        d.work.orders.forEach { o -> Panel { Text(o.title, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("${o.items.size} Positionen · ${if (o.completed) "Erledigt" else "Offen"}", color = Mint)
            TextButton(onClick = { selected = o.id }) { Text("Öffnen / weiter erfassen") } } }
    } else {
        TextButton(onClick = { selected = null }) { Text("‹ Alle Bestelllisten") }; Text(order.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        TextButton(onClick = { editor = true }, enabled = !busy) { Text("Titel / E-Mail-Adresse bearbeiten") }
        order.items.forEachIndexed { index, item -> Panel {
            Text("${index + 1}. ${item.quantity} ${item.unit} · ${item.name}", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (item.assetId.isNotBlank()) Text("Nur lokal: ${assetName(d, item.assetId)}", color = Mint)
            if (item.reason.isNotBlank()) Text(item.reason); StoredPhoto(item.image)
            Row { TextButton(onClick = { selectedItem = item; itemEditor = true }, enabled = !busy) { Text("Bearbeiten") }
                TextButton(onClick = { photo(PhotoTarget("order", item.id)) }, enabled = !busy && (item.image.isNotEmpty() || imageCount(d.work) < MAX_IMAGES)) { Text(if (item.image.isEmpty()) "+ Foto" else "Foto ersetzen") } }
            Row { if (item.image.isNotEmpty()) TextButton(onClick = { update(order.copy(items = order.items.map { if (it.id == item.id) it.copy(image = "") else it })) }, enabled = !busy) { Text("Bild entfernen") }
                TextButton(onClick = { deletion = item }, enabled = !busy) { Text("Entfernen") } }
        } }
        Button(onClick = { selectedItem = null; itemEditor = true }, enabled = !busy) { Text("+ Nächstes Teil") }
        OutlinedButton(onClick = { preview = true }, enabled = order.items.isNotEmpty() && !busy) { Text("Bestellung als E-Mail vorbereiten") }
        TextButton(onClick = { update(order.copy(completed = !order.completed)) }, enabled = !busy) { Text(if (order.completed) "Wieder öffnen" else "Für mich als erledigt markieren") }
        Hint("Die Anlagenzuordnung bleibt lokal. Nur Bezeichnung, Menge, Zweck und hinzugefügte Bilder werden übergeben. Versand erfolgt ausschließlich durch dich in der Mail-App.")
    }
    if (editor) {
        var title by remember { mutableStateOf(order?.title ?: "") }; var recipient by remember { mutableStateOf(order?.recipient ?: "") }
        Form("Bestellliste", title.isNotBlank() && (recipient.isBlank() || (recipient.contains('@') && !recipient.contains('\n'))), { editor = false }, {
            val next = PartsOrder(order?.id ?: newId(), title.trim(), recipient.trim(), order?.items ?: emptyList(), order?.completed ?: false)
            update(next); selected = next.id; editor = false
        }) { Field(title, { title = it }, "Titel *"); Field(recipient, { recipient = it }, "E-Mail Teamleiter (optional)"); Hint("Leer lassen, wenn du den Empfänger erst in der E-Mail-App auswählen möchtest.") }
    }
    if (itemEditor && order != null) {
        var name by remember { mutableStateOf(selectedItem?.name ?: "") }; var quantity by remember { mutableStateOf(selectedItem?.quantity ?: "1") }
        var unit by remember { mutableStateOf(selectedItem?.unit ?: "Stück") }; var reason by remember { mutableStateOf(selectedItem?.reason ?: "") }; var asset by remember { mutableStateOf(selectedItem?.assetId ?: "") }
        Form("Bestellposition", name.isNotBlank() && unit.isNotBlank() && number(quantity)?.let { it > 0 } == true, { itemEditor = false }, {
            val next = OrderItem(selectedItem?.id ?: newId(), name.trim(), quantity.trim(), unit.trim(), reason.trim(), asset, selectedItem?.image ?: "")
            update(order.copy(items = if (selectedItem == null) order.items + next else order.items.map { if (it.id == next.id) next else it })); itemEditor = false
        }) { Field(name, { name = it }, "Teil / Ausrüstung / Artikelnummer *"); Field(quantity, { quantity = it }, "Menge *", numeric = true); Field(unit, { unit = it }, "Einheit *")
            Field(reason, { reason = it }, "Zweck / technische Angaben für Bestellung", 3); Picker("Anlage (nur lokal)", listOf("" to "Ohne Anlage") + d.assets.map { it.id to it.name }, asset) { asset = it }
        }
    }
    if (preview && order != null) Form("E-Mail-Vorschau", !busy, { preview = false }, { preview = false; mail(order) }, confirmLabel = "E-Mail-App öffnen") {
        Text("An: ${order.recipient.ifBlank { "Auswahl in E-Mail-App" }}", color = Mint)
        Hint("Text und ${order.items.count { it.image.isNotEmpty() }} Bilder werden unverschlüsselt an die gewählte Versand-App übergeben. Der nächste Button öffnet den Entwurf, er sendet nichts.")
        Text(orderText(order))
    }
    deletion?.let { item -> AlertDialog(onDismissRequest = { deletion = null }, title = { Text("Position entfernen?") }, text = { Text(item.name) },
        confirmButton = { TextButton(onClick = { order?.let { update(it.copy(items = it.items.filterNot { x -> x.id == item.id })) }; deletion = null }, enabled = !busy) { Text("Entfernen") } },
        dismissButton = { TextButton(onClick = { deletion = null }) { Text("Abbrechen") } }) }
}
