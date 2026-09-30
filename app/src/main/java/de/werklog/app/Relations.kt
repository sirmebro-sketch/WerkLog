package de.werklog.app

import org.json.JSONArray
import org.json.JSONObject

/** One source of truth for contact links; both directions are derived from these IDs. */
data class Contact(val id: String = newId(), val name: String, val company: String = "", val role: String = "",
    val phone: String = "", val email: String = "", val note: String = "",
    val assetIds: List<String> = emptyList(), val entryIds: List<String> = emptyList()) : java.io.Serializable
fun contactsJson(contacts: List<Contact>) = JSONArray().also { a -> contacts.forEach { c -> a.put(JSONObject()
    .put("id", c.id).put("name", c.name).put("company", c.company).put("role", c.role).put("phone", c.phone)
    .put("email", c.email).put("note", c.note).put("assetIds", JSONArray(c.assetIds)).put("entryIds", JSONArray(c.entryIds))) } }
private fun JSONObject.ids(key: String): List<String> = optJSONArray(key)?.let { a ->
    require(a.length() <= 20000); (0 until a.length()).map { a.getString(it) }.distinct()
} ?: emptyList()
fun readContacts(a: JSONArray?): List<Contact> {
    if (a == null) return emptyList()
    require(a.length() <= 20000)
    return (0 until a.length()).map { val j = a.getJSONObject(it); Contact(j.getString("id"), j.getString("name"), j.optString("company"),
        j.optString("role"), j.optString("phone"), j.optString("email"), j.optString("note"), j.ids("assetIds"), j.ids("entryIds")) }
}
fun availableTrades(d: Data) = (d.tradeNames + d.assets.map { it.trade }).filter { it.isNotBlank() }.distinct().sorted()
fun renameTrade(d: Data, old: String, next: String): Data {
    require(next.isNotBlank())
    return d.copy(tradeNames = (availableTrades(d).filterNot { it == old } + next.trim()).distinct(),
        assets = d.assets.map { if (it.trade == old) it.copy(trade = next.trim()) else it },
        work = d.work.copy(templates = d.work.templates.map { if (it.trade == old) it.copy(trade = next.trim()) else it }))
}
fun orderFromEntry(e: Entry) = PartsOrder(title = e.title, entryId = e.id, assetId = e.assetId, context = e.note)
fun removeEntry(d: Data, id: String) = d.copy(entries = d.entries.filterNot { it.id == id },
    contacts = d.contacts.map { it.copy(entryIds = it.entryIds - id) },
    work = d.work.copy(orders = d.work.orders.map { if (it.entryId == id) it.copy(entryId = "") else it }))
fun validateRelations(d: Data) {
    require(d.tradeNames.size <= 1000 && d.tradeNames.all { it.isNotBlank() } && d.tradeNames.distinct().size == d.tradeNames.size)
    val assets = d.assets.map { it.id }.toSet(); val entries = d.entries.map { it.id }.toSet()
    require(d.contacts.map { it.id }.distinct().size == d.contacts.size)
    require(d.contacts.all { it.name.isNotBlank() && it.assetIds.all { id -> id in assets } && it.entryIds.all { id -> id in entries } })
}
fun validateImages(d: Data) {
    val images = imageValues(d).filter { it.isNotEmpty() }
    require(images.size <= MAX_IMAGES) { "Höchstens $MAX_IMAGES Bilder insgesamt" }
    images.filterNot(::isImageRef).forEach {
        require(it.length <= (MAX_IMAGE_BYTES + 2) / 3 * 4)
        validateImageBytes(java.util.Base64.getDecoder().decode(it))
    }
}
