package de.werklog.app

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import java.time.LocalDate

fun entryDue(e: Entry, today: LocalDate = LocalDate.now()) = e.status != "Erledigt" && parseServiceDate(e.dueDate)?.let { !it.isAfter(today) } == true
fun entryOverdue(e: Entry, today: LocalDate = LocalDate.now()) = e.status != "Erledigt" && parseServiceDate(e.dueDate)?.isBefore(today) == true
fun dueEntries(d: Data, through: LocalDate) = d.entries.filter { it.status != "Erledigt" && parseServiceDate(it.dueDate)?.let { date -> !date.isAfter(through) } == true }
    .sortedWith(compareBy<Entry> { parseServiceDate(it.dueDate) }.thenByDescending { priorities.indexOf(it.priority) })
fun dueOrders(d: Data, through: LocalDate) = d.work.orders.filter { it.status in listOf("Angefragt", "Bestellt", "Teilgeliefert") && parseServiceDate(it.delivery)?.let { date -> !date.isAfter(through) } == true }.sortedBy { parseServiceDate(it.delivery) }
fun validParents(assets: List<Asset>, child: String?): List<Asset> {
    val byId = assets.associateBy { it.id }
    return assets.filter { candidate ->
    var parent = candidate.id; val visited = mutableSetOf<String>(); var valid = true
    while (parent.isNotBlank() && visited.add(parent)) {
        if (parent == child) { valid = false; break }
        parent = byId[parent]?.parentId.orEmpty()
    }
    valid
    }
}

/** The search still covers all records; only visible composition is bounded. */
@Composable fun <T> PagedRecords(records: List<T>, filter: String, id: (T) -> String, row: @Composable (T) -> Unit) {
    var limit by rememberSaveable(filter) { mutableIntStateOf(40) }
    if (records.isEmpty()) Text("Keine passenden Einträge.", color = Muted)
    records.take(limit).forEach { record -> key(id(record)) { row(record) } }
    if (records.size > limit) OutlinedButton(onClick = { limit += 40 }) { Text("Weitere anzeigen · ${records.size - limit} verbleibend") }
}
