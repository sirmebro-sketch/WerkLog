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
    if (reset) return null
    if (previous != null && value < previous.value) return "Stand kleiner als zuvor. Eingabe prüfen oder Zählerwechsel markieren."
    if (previous != null && meter.maxDelta != null && value - previous.value > meter.maxDelta) return "Differenz überschreitet deine hinterlegte Prüfgrenze (${meter.maxDelta} ${meter.unit})."
    return null
}
fun readingDelta(reading: Reading, previous: Reading?): Double? = if (previous == null || reading.reset || reading.value < previous.value) null else reading.value - previous.value
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
        entries = combine(current.entries, incoming.entries.map { it.copy(id = id("entry", it.id, current.entries.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target) }, { it.id }),
        readings = combine(current.readings, incoming.readings.map { it.copy(id = id("reading", it.id, current.readings.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target, meterId = meterIds[it.meterId] ?: "") }, { it.id }),
        work = current.work.copy(meters = combine(current.work.meters, meters, { it.id }),
            guides = combine(current.work.guides, incoming.work.guides.map { g -> g.copy(id = id("guide", g.id, current.work.guides.filter { x -> x.assetId == target }.map { x -> x.id }), assetId = target, steps = g.steps.map { it.copy(id = importId(target, "step", it.id)) }) }, { it.id })))
}
val starterTemplates = listOf(
    EntryTemplate(id = "pump", name = "Pumpe / Antrieb", title = "Kontrolle Pumpe / Antrieb", body = "Beobachtung:\nGeräusch / Schwingung:\nDichtheit:\nMesswerte:\nMaßnahme:\nNächster Schritt:", trade = "Alle"),
    EntryTemplate(id = "water", name = "Wasseraufbereitung", title = "Kontrolle Wasseraufbereitung", body = "Anlagenteil:\nBetriebszustand:\nMesswerte / Einheit:\nVerbrauchsmaterial:\nAuffälligkeit:\nMaßnahme:", trade = "Wasser"),
    EntryTemplate(id = "electric", name = "Elektrisches Betriebsmittel", title = "Befund Betriebsmittel", body = "Kennzeichnung:\nFehlerbild:\nDokumentierter Befund:\nErsetztes Teil / Artikelnummer:\nPrüfprotokoll-Ablage:\nWeitere Arbeit:", trade = "Alle"))
