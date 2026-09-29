package de.werklog.app

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun stamp(time: Long) = DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(time))
internal fun assetName(d: Data, id: String) = d.assets.find { it.id == id }?.name ?: "Anlage"

internal fun handover(d: Data) = buildString {
    append("SCHICHTÜBERGABE · WERKLOG\n${stamp(System.currentTimeMillis())}\n\nOFFENE VORGÄNGE\n")
    val open = d.entries.filter { it.status != "Erledigt" }.sortedByDescending { priorities.indexOf(it.priority) }
    if (open.isEmpty()) append("Keine offenen Vorgänge.\n")
    open.forEach { append("\n[${it.priority} · ${it.status}] ${it.title}\n${assetName(d, it.assetId)}\n${it.note}\nAktualisiert: ${stamp(it.updated)}\nAufwand: ${it.minutes} min\n") }
    val today = java.time.LocalDate.now()
    val done = d.entries.filter { it.status == "Erledigt" && Instant.ofEpochMilli(it.updated).atZone(ZoneId.systemDefault()).toLocalDate() == today }
    append("\nHEUTE ERLEDIGT\n"); if (done.isEmpty()) append("Keine Einträge.\n")
    done.forEach { append("• ${assetName(d, it.assetId)}: ${it.title}\n${it.note}\n") }
}
