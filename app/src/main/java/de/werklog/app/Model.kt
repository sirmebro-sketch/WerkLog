package de.werklog.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun newId() = UUID.randomUUID().toString()
val trades = listOf("Dampf", "Kälte", "Heizung", "BHKW", "Notstrom", "Wasser", "GLT", "Sonstiges")
val statuses = listOf("Offen", "In Arbeit", "Erledigt")
val priorities = listOf("Normal", "Wichtig", "Dringend")
data class Asset(val id: String = newId(), val name: String, val trade: String, val location: String, val note: String)
data class Entry(val id: String = newId(), val assetId: String, val title: String, val note: String,
    val priority: String = "Normal", val status: String = "Offen", val created: Long = System.currentTimeMillis(),
    val updated: Long = created, val minutes: Int = 0)
data class Reading(val id: String = newId(), val assetId: String, val label: String, val value: Double,
    val unit: String, val note: String, val created: Long = System.currentTimeMillis())
data class Round(val id: String = newId(), val title: String, val checks: List<String>)
data class RoundRun(val id: String = newId(), val title: String, val results: List<String>, val note: String,
    val created: Long = System.currentTimeMillis())
data class Data(val assets: List<Asset> = emptyList(), val entries: List<Entry> = emptyList(),
    val readings: List<Reading> = emptyList(), val rounds: List<Round> = emptyList(), val runs: List<RoundRun> = emptyList())
fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun <T> List<T>.json(map: (T) -> JSONObject) = JSONArray().also { a -> forEach { a.put(map(it)) } }
private fun obj(vararg pairs: Pair<String, Any>) = JSONObject().also { j -> pairs.forEach { j.put(it.first, it.second) } }
fun encode(d: Data): ByteArray = obj("schema" to 1,
    "assets" to d.assets.json { obj("id" to it.id, "name" to it.name, "trade" to it.trade, "location" to it.location, "note" to it.note) },
    "entries" to d.entries.json { obj("id" to it.id, "assetId" to it.assetId, "title" to it.title, "note" to it.note, "priority" to it.priority, "status" to it.status, "created" to it.created, "updated" to it.updated, "minutes" to it.minutes) },
    "readings" to d.readings.json { obj("id" to it.id, "assetId" to it.assetId, "label" to it.label, "value" to it.value, "unit" to it.unit, "note" to it.note, "created" to it.created) },
    "rounds" to d.rounds.json { obj("id" to it.id, "title" to it.title, "checks" to JSONArray(it.checks)) },
    "runs" to d.runs.json { obj("id" to it.id, "title" to it.title, "results" to JSONArray(it.results), "note" to it.note, "created" to it.created) }
).toString().toByteArray(Charsets.UTF_8)
private fun <T> JSONObject.list(key: String, map: (JSONObject) -> T): List<T> = getJSONArray(key).let { a ->
    require(a.length() <= 20000) { "Zu viele Datensätze" }; (0 until a.length()).map { map(a.getJSONObject(it)) }
}
private fun JSONObject.strings(key: String): List<String> = getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
fun decode(bytes: ByteArray): Data {
    val j = JSONObject(bytes.toString(Charsets.UTF_8)); require(j.getInt("schema") == 1) { "Unbekannte Datenversion" }
    val d = Data(j.list("assets") { Asset(it.getString("id"), it.getString("name"), it.getString("trade"), it.getString("location"), it.getString("note")) },
        j.list("entries") { Entry(it.getString("id"), it.getString("assetId"), it.getString("title"), it.getString("note"), it.getString("priority"), it.getString("status"), it.getLong("created"), it.getLong("updated"), it.getInt("minutes")) },
        j.list("readings") { Reading(it.getString("id"), it.getString("assetId"), it.getString("label"), it.getDouble("value"), it.getString("unit"), it.getString("note"), it.getLong("created")) },
        j.list("rounds") { Round(it.getString("id"), it.getString("title"), it.strings("checks")) },
        j.list("runs") { RoundRun(it.getString("id"), it.getString("title"), it.strings("results"), it.getString("note"), it.getLong("created")) })
    val ids = d.assets.map { it.id }.toSet()
    require(ids.size == d.assets.size && d.assets.all { it.name.isNotBlank() })
    require(d.entries.all { it.assetId in ids && it.status in statuses && it.priority in priorities && it.minutes >= 0 })
    require(d.readings.all { it.assetId in ids && it.value.isFinite() })
    require(d.rounds.all { it.checks.isNotEmpty() && it.checks.size <= 100 })
    return d
}
