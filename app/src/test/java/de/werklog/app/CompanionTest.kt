package de.werklog.app
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
class CompanionTest {
    @Test fun monthlyRecurrenceRetainsOriginalDayAfterFebruary() {
        val e = Appointment(title = "Service", start = "31.01.2026 08:00", repeat = "Monatlich")
        assertEquals(listOf("31.01.2026 08:00", "28.02.2026 08:00", "31.03.2026 08:00"), occurrences(e, LocalDate.of(2026,1,1), LocalDate.of(2026,3,31)).map { it.start })
    }
    @Test fun resetAndNegativeDifferenceAreNotConsumption() {
        val r = Reading(assetId = "a", label = "M", value = 50.0, unit = "kWh", note = "")
        assertNull(readingDelta(r.copy(value = 5.0), r)); assertNull(readingDelta(r.copy(value = 100.0, reset = true), r))
        assertEquals(50.0, readingDelta(r.copy(value = 100.0), r)!!, .001)
        assertNotNull(meterWarning(Meter(assetId = "a", name = "M", unit = "kWh", maxDelta = 10.0), r, 100.0, false))
    }
    @Test fun repeatedMergeDoesNotDuplicateAndPreservesReceiverByDefault() {
        val a = Asset(name = "A", trade = "Wasser", location = "", note = "")
        val incoming = Data(assets = listOf(a), infos = listOf(AssetInfo(assetId = a.id, title = "Info", body = "Neu")))
        val current = Data(assets = listOf(a.copy(id = "local")))
        val first = mergePackage(current, incoming, "local", false)
        val second = mergePackage(first, incoming, "local", false)
        assertEquals(first, second); assertEquals(1, second.infos.size)
        assertEquals(ImportChanges(0, 0, 1), importChanges(first, incoming, "local"))
        val edited = first.copy(infos = first.infos.map { it.copy(body = "Lokal") })
        assertEquals("Lokal", mergePackage(edited, incoming, "local", false).infos.single().body)
        assertEquals("Neu", mergePackage(edited, incoming, "local", true).infos.single().body)
    }
    @Test fun hierarchyCyclesRejectedAndKnowledgeIsSearchable() {
        val a = Asset(id = "a", name = "Anlage", trade = "Wasser", location = "", note = "", parentId = "b")
        val b = a.copy(id = "b", parentId = "a")
        assertThrows(IllegalArgumentException::class.java) { decode(encode(Data(assets = listOf(a,b)))) }
        val d = Data(assets = listOf(a.copy(parentId = "")), infos = listOf(AssetInfo(assetId = "a", title = "Filter", body = "Artikelnummer 42")))
        assertEquals(1, searchAssets(d, "Artikelnummer").size)
    }
}
