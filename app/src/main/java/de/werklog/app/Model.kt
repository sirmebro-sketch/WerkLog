package de.werklog.app

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

fun newId() = UUID.randomUUID().toString()
val trades = listOf("Dampf", "Kälte", "Heizung", "BHKW", "Notstrom", "Wasser", "GLT", "Sonstiges")
val statuses = listOf("Offen", "In Arbeit", "Erledigt")
val priorities = listOf("Normal", "Wichtig", "Dringend")
data class Asset(val id: String = newId(), val name: String, val trade: String, val location: String, val note: String,
    val manufacturer: String = "", val model: String = "", val serial: String = "",
    val contact: String = "", val spareParts: String = "", val nextService: String = "", val tag: String = "", val parentId: String = "", val favorite: Boolean = false, val lastOpened: Long = 0) : java.io.Serializable
data class Entry(val id: String = newId(), val assetId: String, val title: String, val note: String,
    val priority: String = "Normal", val status: String = "Offen", val created: Long = System.currentTimeMillis(),
    val updated: Long = created, val minutes: Int = 0) : java.io.Serializable
data class Reading(val id: String = newId(), val assetId: String, val label: String, val value: Double,
    val unit: String, val note: String, val created: Long = System.currentTimeMillis(), val meterId: String = "", val reset: Boolean = false) : java.io.Serializable
data class Round(val id: String = newId(), val title: String, val checks: List<String>) : java.io.Serializable
data class RoundRun(val id: String = newId(), val title: String, val results: List<String>, val note: String,
    val created: Long = System.currentTimeMillis()) : java.io.Serializable
data class Data(val assets: List<Asset> = emptyList(), val entries: List<Entry> = emptyList(),
    val readings: List<Reading> = emptyList(), val rounds: List<Round> = emptyList(), val runs: List<RoundRun> = emptyList(),
    val credentials: List<Credential> = emptyList(), val infos: List<AssetInfo> = emptyList(), val work: WorkData = WorkData(), val profile: LocalProfile = LocalProfile()) : java.io.Serializable
fun number(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
private fun <T> List<T>.json(map: (T) -> JSONObject) = JSONArray().also { a -> forEach { a.put(map(it)) } }
private fun obj(vararg pairs: Pair<String, Any>) = JSONObject().also { j -> pairs.forEach { j.put(it.first, it.second) } }
fun encode(d: Data): ByteArray = obj("schema" to 5,
    "profile" to profileJson(d.profile),
    "assets" to d.assets.json { obj("id" to it.id, "name" to it.name, "trade" to it.trade, "location" to it.location, "note" to it.note, "manufacturer" to it.manufacturer, "model" to it.model, "serial" to it.serial, "contact" to it.contact, "spareParts" to it.spareParts, "nextService" to it.nextService, "tag" to it.tag, "parentId" to it.parentId, "favorite" to it.favorite, "lastOpened" to it.lastOpened) },
    "entries" to d.entries.json { obj("id" to it.id, "assetId" to it.assetId, "title" to it.title, "note" to it.note, "priority" to it.priority, "status" to it.status, "created" to it.created, "updated" to it.updated, "minutes" to it.minutes) },
    "readings" to d.readings.json { obj("id" to it.id, "assetId" to it.assetId, "label" to it.label, "value" to it.value, "unit" to it.unit, "note" to it.note, "created" to it.created, "meterId" to it.meterId, "reset" to it.reset) },
    "rounds" to d.rounds.json { obj("id" to it.id, "title" to it.title, "checks" to JSONArray(it.checks)) },
    "credentials" to d.credentials.json { obj("id" to it.id, "assetId" to it.assetId, "title" to it.title, "username" to it.username, "password" to it.password, "address" to it.address, "note" to it.note, "updated" to it.updated) },
    "infos" to d.infos.json { obj("id" to it.id, "assetId" to it.assetId, "title" to it.title, "body" to it.body, "updated" to it.updated) },
    "runs" to d.runs.json { obj("id" to it.id, "title" to it.title, "results" to JSONArray(it.results), "note" to it.note, "created" to it.created) }
).put("work", workJson(d.work)).toString().toByteArray(Charsets.UTF_8)
private fun <T> JSONObject.list(key: String, map: (JSONObject) -> T): List<T> = getJSONArray(key).let { a ->
    require(a.length() <= 20000) { "Zu viele Datensätze" }; (0 until a.length()).map { map(a.getJSONObject(it)) }
}
private fun JSONObject.strings(key: String): List<String> = getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
fun decode(bytes: ByteArray): Data {
    val j = JSONObject(bytes.toString(Charsets.UTF_8)); val schema = j.getInt("schema"); require(schema in 1..5) { "Unbekannte Datenversion" }
    val d = Data(j.list("assets") { Asset(it.getString("id"), it.getString("name"), it.getString("trade"), it.getString("location"), it.getString("note"), it.optString("manufacturer"), it.optString("model"), it.optString("serial"), it.optString("contact"), it.optString("spareParts"), it.optString("nextService"), it.optString("tag"), it.optString("parentId"), it.optBoolean("favorite"), it.optLong("lastOpened")) },
        j.list("entries") { Entry(it.getString("id"), it.getString("assetId"), it.getString("title"), it.getString("note"), it.getString("priority"), it.getString("status"), it.getLong("created"), it.getLong("updated"), it.getInt("minutes")) },
        j.list("readings") { Reading(it.getString("id"), it.getString("assetId"), it.getString("label"), it.getDouble("value"), it.getString("unit"), it.getString("note"), it.getLong("created"), it.optString("meterId"), it.optBoolean("reset")) },
        j.list("rounds") { Round(it.getString("id"), it.getString("title"), it.strings("checks")) },
        j.list("runs") { RoundRun(it.getString("id"), it.getString("title"), it.strings("results"), it.getString("note"), it.getLong("created")) },
        if (schema == 1) emptyList() else j.list("credentials") { Credential(it.getString("id"), it.getString("assetId"), it.getString("title"), it.getString("username"), it.getString("password"), it.getString("address"), it.getString("note"), it.getLong("updated")) },
        if (schema == 1) emptyList() else j.list("infos") { AssetInfo(it.getString("id"), it.getString("assetId"), it.getString("title"), it.getString("body"), it.getLong("updated")) },
        if (schema < 3) WorkData() else readWork(j.getJSONObject("work")), readProfile(j.optJSONObject("profile")))
    val ids = d.assets.map { it.id }.toSet()
    require(ids.size == d.assets.size && d.assets.all { it.name.isNotBlank() })
    require(d.entries.all { it.assetId in ids && it.status in statuses && it.priority in priorities && it.minutes >= 0 })
    require(d.readings.all { it.assetId in ids && it.value.isFinite() })
    require(d.rounds.all { it.checks.isNotEmpty() && it.checks.size <= 100 })
    require(d.assets.all { it.nextService.isBlank() || parseServiceDate(it.nextService) != null })
    require(d.credentials.all { it.assetId in ids && it.title.isNotBlank() && it.password.isNotEmpty() })
    require(d.infos.all { it.assetId in ids && it.title.isNotBlank() && it.body.isNotBlank() })
    require(d.credentials.map { it.id }.distinct().size == d.credentials.size)
    require(d.infos.map { it.id }.distinct().size == d.infos.size)
    d.assets.forEach { asset ->
        val visited = mutableSetOf(asset.id); var parent = asset.parentId
        while (parent.isNotEmpty()) { require(parent in ids && visited.add(parent)) { "Ungültige Anlagenhierarchie" }; parent = d.assets.single { it.id == parent }.parentId }
    }
    validateProfile(d.profile)
    validateWork(d.work, ids)
    require(d.readings.all { r -> r.meterId.isEmpty() || d.work.meters.any { it.id == r.meterId && it.assetId == r.assetId } })
    return d
}
