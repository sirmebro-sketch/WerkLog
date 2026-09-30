package de.werklog.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
val repetitions = listOf("Nie", "Täglich", "Wöchentlich", "Monatlich", "Jährlich")
val orderStatuses = listOf("Entwurf", "Angefragt", "Bestellt", "Teilgeliefert", "Geliefert", "Abgesagt")
fun occurrences(event: Appointment, from: LocalDate, to: LocalDate): List<Appointment> {
    val start = appointmentTime(event.start) ?: return emptyList()
    if (event.repeat == "Nie" || event.status != "Geplant") return if (start.toLocalDate() in from..to) listOf(event) else emptyList()
    val result = mutableListOf<Appointment>(); var n = 0L
    while (n < 40000) {
        val next = when (event.repeat) { "Täglich" -> start.plusDays(n); "Wöchentlich" -> start.plusWeeks(n); "Monatlich" -> start.plusMonths(n); "Jährlich" -> start.plusYears(n); else -> start }
        if (next.toLocalDate() > to) break
        if (next.toLocalDate() >= from) result += event.copy(start = formatAppointment(next))
        n++
    }
    return result
}
fun meterWarning(meter: Meter, previous: Reading?, value: Double, reset: Boolean): String? {
    if (reset || (previous != null && previous.unit != meter.unit)) return null
    if (previous != null && value < previous.value) return "Stand kleiner als zuvor. Eingabe prüfen oder Zählerwechsel markieren."
    if (previous != null && meter.maxDelta != null && value - previous.value > meter.maxDelta) return "Differenz überschreitet deine hinterlegte Prüfgrenze (${meter.maxDelta} ${meter.unit})."
    return null
}
fun readingDelta(reading: Reading, previous: Reading?): Double? = if (previous == null || reading.unit != previous.unit || reading.reset || reading.value < previous.value) null else reading.value - previous.value
fun searchAssets(d: Data, query: String): List<Asset> = d.assets.filter { a ->
    val text = listOf(a.name, a.tag, a.location, a.trade, a.manufacturer, a.model, a.serial, a.note) +
        d.infos.filter { it.assetId == a.id }.flatMap { listOf(it.title, it.body) } +
        d.work.guides.filter { it.assetId == a.id }.flatMap { listOf(it.title) + it.steps.map { s -> s.title + " " + s.body } }
    text.any { it.contains(query, true) }
}.sortedWith(compareByDescending<Asset> { it.favorite }.thenByDescending { it.lastOpened }.thenBy { it.name.lowercase() })
fun importId(target: String, kind: String, source: String) = UUID.nameUUIDFromBytes("$target:$kind:$source".toByteArray()).toString()
fun mergePackage(current: Data, incoming: Data, target: String, replace: Boolean): Data {
    require(current.assets.any { it.id == target } && incoming.assets.size == 1)
    fun id(kind: String, source: String, existing: List<String>) = if (source in existing) source else importId(target, kind, source)
    val meters = incoming.work.meters.map { it.copy(id = id("meter", it.id, current.work.meters.filter { m -> m.assetId == target }.map { m -> m.id }), assetId = target) }
    val meterIds = incoming.work.meters.zip(meters).associate { it.first.id to it.second.id }
    fun <T> combine(old: List<T>, fresh: List<T>, getId: (T) -> String): List<T> {
        val map = old.associateBy(getId).toMutableMap(); fresh.forEach { if (replace || getId(it) !in map) map[getId(it)] = it }; return map.values.toList()
    }
    return current.copy(
        // Receiver's asset identity, hierarchy and favorites stay local. Explicit merge concerns contents.
        infos = combine(current.infos, incoming.infos.map { it.copy(id = id("info", it.id, current.infos.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target) }, { it.id }),
        credentials = combine(current.credentials, incoming.credentials.map { it.copy(id = id("credential", it.id, current.credentials.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target) }, { it.id }),
        entries = combine(current.entries, incoming.entries.map { it.copy(id = id("entry", it.id, current.entries.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target, guideIds = it.guideIds.filter { g -> incoming.work.guides.any { x -> x.id == g } }.map { g -> id("guide", g, current.work.guides.filter { x -> x.assetId == target }.map { x -> x.id }) }) }, { it.id }),
        readings = combine(current.readings, incoming.readings.map { it.copy(id = id("reading", it.id, current.readings.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target, meterId = meterIds[it.meterId] ?: "") }, { it.id }),
        work = current.work.copy(meters = combine(current.work.meters, meters, { it.id }),
            guides = combine(current.work.guides, incoming.work.guides.map { g -> g.copy(id = id("guide", g.id, current.work.guides.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target, steps = g.steps.map { it.copy(id = importId(target, "step", it.id)) }) }, { it.id })))
}
val starterTemplates = listOf(
    EntryTemplate(id = "pump", name = "Pumpe / Antrieb", title = "Kontrolle Pumpe / Antrieb", body = "Beobachtung:\nGeräusch / Schwingung:\nDichtheit:\nMesswerte:\nMaßnahme:\nNächster Schritt:", trade = "Alle"),
    EntryTemplate(id = "water", name = "Wasseraufbereitung", title = "Kontrolle Wasseraufbereitung", body = "Anlagenteil:\nBetriebszustand:\nMesswerte / Einheit:\nVerbrauchsmaterial:\nAuffälligkeit:\nMaßnahme:", trade = "Wasser"),
    EntryTemplate(id = "electric", name = "Elektrisches Betriebsmittel", title = "Befund Betriebsmittel", body = "Kennzeichnung:\nFehlerbild:\nDokumentierter Befund:\nErsetztes Teil / Artikelnummer:\nPrüfprotokoll-Ablage:\nWeitere Arbeit:", trade = "Alle"))

fun removeAsset(d: Data, id: String): Data = d.copy(
    contacts = d.contacts.map { c -> c.copy(assetIds = c.assetIds - id, entryIds = c.entryIds.filterNot { e -> d.entries.any { it.id == e && it.assetId == id } }) },
    assets = d.assets.filterNot { it.id == id }.map { if (it.parentId == id) it.copy(parentId = "") else it },
    entries = d.entries.filterNot { it.assetId == id }, readings = d.readings.filterNot { it.assetId == id },
    credentials = d.credentials.filterNot { it.assetId == id }, infos = d.infos.filterNot { it.assetId == id },
    work = d.work.copy(meters = d.work.meters.filterNot { it.assetId == id },
        appointments = d.work.appointments.map { if (it.assetId == id) it.copy(assetId = "") else it },
        guides = d.work.guides.map { if (it.assetId == id) it.copy(assetId = "") else it },
        orders = d.work.orders.map { o -> o.copy(assetId = if (o.assetId == id) "" else o.assetId, entryId = if (d.entries.any { it.id == o.entryId && it.assetId == id }) "" else o.entryId, items = o.items.map { if (it.assetId == id) it.copy(assetId = "") else it }) }))

data class ImportChanges(val fresh: Int, val changed: Int, val unchanged: Int)
/** Compare localized incoming identities. Call with image fingerprints for content comparison. */
fun importChanges(current: Data, incoming: Data, target: String): ImportChanges {
    val merged = mergePackage(current, incoming, target, true)
    var fresh = 0; var changed = 0; var unchanged = 0
    fun <T> compare(old: List<T>, source: List<T>, next: List<T>, kind: String, id: (T) -> String) {
        val oldMap = old.associateBy(id); val nextMap = next.associateBy(id)
        source.forEach { item ->
            val key = if (id(item) in oldMap) id(item) else importId(target, kind, id(item))
            val before = oldMap[key]; val after = nextMap.getValue(key)
            if (before == null) fresh++ else if (before == after) unchanged++ else changed++
        }
    }
    compare(current.infos.filter { it.assetId == target }, incoming.infos, merged.infos, "info", { it.id })
    compare(current.credentials.filter { it.assetId == target }, incoming.credentials, merged.credentials, "credential", { it.id })
    compare(current.entries.filter { it.assetId == target }, incoming.entries, merged.entries, "entry", { it.id })
    compare(current.readings.filter { it.assetId == target }, incoming.readings, merged.readings, "reading", { it.id })
    compare(current.work.meters.filter { it.assetId == target }, incoming.work.meters, merged.work.meters, "meter", { it.id })
    compare(current.work.guides.filter { it.assetId == target }, incoming.work.guides, merged.work.guides, "guide", { it.id })
    return ImportChanges(fresh, changed, unchanged)
}
suspend fun fingerprintImages(data: Data, load: suspend (String) -> ByteArray): Data {
    val hashes = mutableMapOf<String, String>()
    for (image in imageValues(data).filter { it.isNotEmpty() }.distinct()) {
        val bytes = load(image)
        try { hashes[image] = "sha256:" + java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)) } finally { bytes.fill(0) }
    }
    return mapImages(data) { hashes[it] ?: it }
}
