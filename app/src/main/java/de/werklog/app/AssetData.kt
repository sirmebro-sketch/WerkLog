package de.werklog.app

import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** These records never participate in mail or journal search. */
data class Credential(val id: String = newId(), val assetId: String, val title: String,
    val username: String, val password: String, val address: String = "", val note: String = "",
    val updated: Long = System.currentTimeMillis())
data class AssetInfo(val id: String = newId(), val assetId: String, val title: String, val body: String,
    val updated: Long = System.currentTimeMillis())

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
fun parseServiceDate(text: String): LocalDate? = runCatching { LocalDate.parse(text.trim(), dateFormat) }.getOrNull()
fun serviceState(date: String, today: LocalDate = LocalDate.now()): String? = parseServiceDate(date)?.let {
    when {
        it.isBefore(today) -> "Überfällig"
        it == today -> "Heute fällig"
        !it.isAfter(today.plusDays(30)) -> "In den nächsten 30 Tagen"
        else -> "Geplant"
    }
}
object PasswordGenerator {
    private val groups = listOf("ABCDEFGHJKLMNPQRSTUVWXYZ", "abcdefghijkmnopqrstuvwxyz", "23456789", "!#%+,-.:=?@_")
    fun generate(length: Int = 20): String {
        require(length in 12..64)
        val random = SecureRandom()
        val pool = groups.joinToString("")
        val chars = groups.map { it[random.nextInt(it.length)] }.toMutableList()
        repeat(length - groups.size) { chars.add(pool[random.nextInt(pool.length)]) }
        for (i in chars.lastIndex downTo 1) { val j = random.nextInt(i + 1); val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp }
        return chars.joinToString("")
    }
}
