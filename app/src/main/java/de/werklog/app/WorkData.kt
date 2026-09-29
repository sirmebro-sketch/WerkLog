package de.werklog.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Base64

data class Meter(val id: String = newId(), val assetId: String, val name: String, val unit: String, val note: String = "")
data class Appointment(val id: String = newId(), val title: String, val start: String, val minutes: Int = 60,
    val company: String = "", val contact: String = "", val responsible: String = "", val assetId: String = "", val note: String = "", val status: String = "Geplant")
data class GuideStep(val id: String = newId(), val title: String, val body: String, val image: String = "")
data class Guide(val id: String = newId(), val title: String, val assetId: String = "", val steps: List<GuideStep> = emptyList())
data class OrderItem(val id: String = newId(), val name: String, val quantity: String, val unit: String = "Stück", val reason: String = "", val assetId: String = "", val image: String = "")
data class PartsOrder(val id: String = newId(), val title: String, val recipient: String = "", val items: List<OrderItem> = emptyList(), val completed: Boolean = false)
data class WorkData(val meters: List<Meter> = emptyList(), val appointments: List<Appointment> = emptyList(), val guides: List<Guide> = emptyList(), val orders: List<PartsOrder> = emptyList())
data class PhotoTarget(val kind: String, val id: String)
const val MAX_IMAGE_BYTES = 160 * 1024
const val MAX_IMAGES = 30
private val appointmentFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)
fun appointmentTime(text: String): LocalDateTime? = runCatching { LocalDateTime.parse(text.trim(), appointmentFormat) }.getOrNull()
fun formatAppointment(time: LocalDateTime): String = appointmentFormat.format(time)
fun meterCandidates(text: String): List<String> = Regex("(?<![A-Za-z0-9])[0-9]+(?:[.,][0-9]+)?(?![A-Za-z0-9])").findAll(text).map { it.value }.distinct().take(12).toList()
fun imageCount(w: WorkData) = w.guides.sumOf { g -> g.steps.count { it.image.isNotEmpty() } } + w.orders.sumOf { o -> o.items.count { it.image.isNotEmpty() } }
fun orderText(order: PartsOrder): String = buildString {
    append("BESTELLANFRAGE · ${order.title}\n\n")
    order.items.forEachIndexed { index, item ->
        append("${index + 1}. ${item.quantity} ${item.unit} · ${item.name}\n")
        if (item.reason.isNotBlank()) append("   Zweck / Angaben: ${item.reason}\n")
        if (item.image.isNotEmpty()) append("   Bild: Position-${index + 1}.jpg\n")
    }
    append("\nBitte um Prüfung und Bestellung.\n")
}
private fun json(vararg pairs: Pair<String, Any>) = JSONObject().also { j -> pairs.forEach { j.put(it.first, it.second) } }
private fun <T> arr(items: List<T>, fn: (T) -> JSONObject) = JSONArray().also { j -> items.forEach { j.put(fn(it)) } }
private fun <T> read(j: JSONObject, key: String, fn: (JSONObject) -> T): List<T> = j.getJSONArray(key).let { a -> require(a.length() <= 20000); (0 until a.length()).map { fn(a.getJSONObject(it)) } }
fun workJson(w: WorkData): JSONObject = json(
    "meters" to arr(w.meters) { json("id" to it.id, "assetId" to it.assetId, "name" to it.name, "unit" to it.unit, "note" to it.note) },
    "appointments" to arr(w.appointments) { json("id" to it.id, "title" to it.title, "start" to it.start, "minutes" to it.minutes, "company" to it.company, "contact" to it.contact, "responsible" to it.responsible, "assetId" to it.assetId, "note" to it.note, "status" to it.status) },
    "guides" to arr(w.guides) { json("id" to it.id, "title" to it.title, "assetId" to it.assetId, "steps" to arr(it.steps) { s -> json("id" to s.id, "title" to s.title, "body" to s.body, "image" to s.image) }) },
    "orders" to arr(w.orders) { json("id" to it.id, "title" to it.title, "recipient" to it.recipient, "completed" to it.completed, "items" to arr(it.items) { x -> json("id" to x.id, "name" to x.name, "quantity" to x.quantity, "unit" to x.unit, "reason" to x.reason, "assetId" to x.assetId, "image" to x.image) }) }
)
fun readWork(j: JSONObject) = WorkData(
    read(j, "meters") { Meter(it.getString("id"), it.getString("assetId"), it.getString("name"), it.getString("unit"), it.getString("note")) },
    read(j, "appointments") { Appointment(it.getString("id"), it.getString("title"), it.getString("start"), it.getInt("minutes"), it.getString("company"), it.getString("contact"), it.getString("responsible"), it.getString("assetId"), it.getString("note"), it.getString("status")) },
    read(j, "guides") { Guide(it.getString("id"), it.getString("title"), it.getString("assetId"), read(it, "steps") { s -> GuideStep(s.getString("id"), s.getString("title"), s.getString("body"), s.getString("image")) }) },
    read(j, "orders") { PartsOrder(it.getString("id"), it.getString("title"), it.getString("recipient"), read(it, "items") { x -> OrderItem(x.getString("id"), x.getString("name"), x.getString("quantity"), x.getString("unit"), x.getString("reason"), x.getString("assetId"), x.getString("image")) }, it.getBoolean("completed")) }
)
fun validateWork(w: WorkData, assetIds: Set<String>) {
    require(w.meters.all { it.assetId in assetIds && it.name.isNotBlank() && it.unit.isNotBlank() })
    require(w.appointments.all { it.title.isNotBlank() && appointmentTime(it.start) != null && it.minutes in 1..10080 && (it.assetId.isEmpty() || it.assetId in assetIds) && it.status in listOf("Geplant", "Erledigt", "Abgesagt") })
    require(w.guides.all { it.title.isNotBlank() && (it.assetId.isEmpty() || it.assetId in assetIds) && it.steps.size <= 100 && it.steps.all { s -> s.title.isNotBlank() } })
    require(w.orders.all { it.title.isNotBlank() && it.items.all { x -> x.name.isNotBlank() && (number(x.quantity)?.let { n -> n > 0 } == true) && (x.assetId.isEmpty() || x.assetId in assetIds) } })
    val images = w.guides.flatMap { it.steps.map { s -> s.image } } + w.orders.flatMap { it.items.map { x -> x.image } }
    require(images.count { it.isNotEmpty() } <= MAX_IMAGES)
    images.filter { it.isNotEmpty() }.forEach { value ->
        require(value.length <= (MAX_IMAGE_BYTES + 2) / 3 * 4)
        val bytes = Base64.getDecoder().decode(value)
        require(bytes.size <= MAX_IMAGE_BYTES && bytes.size > 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte())
    }
    for (ids in listOf(w.meters.map { it.id }, w.appointments.map { it.id }, w.guides.map { it.id }, w.orders.map { it.id }, w.guides.flatMap { it.steps.map { s -> s.id } }, w.orders.flatMap { it.items.map { x -> x.id } })) require(ids.size == ids.distinct().size)
}
fun attachImage(data: Data, target: PhotoTarget, image: String): Data {
    val w = data.work
    val next = when (target.kind) {
        "guide" -> { require(w.guides.any { it.steps.any { s -> s.id == target.id } }); w.copy(guides = w.guides.map { g -> g.copy(steps = g.steps.map { if (it.id == target.id) it.copy(image = image) else it }) }) }
        "order" -> { require(w.orders.any { it.items.any { x -> x.id == target.id } }); w.copy(orders = w.orders.map { o -> o.copy(items = o.items.map { if (it.id == target.id) it.copy(image = image) else it }) }) }
        else -> error("Unbekanntes Bildziel")
    }
    validateWork(next, data.assets.map { it.id }.toSet())
    return data.copy(work = next)
}
