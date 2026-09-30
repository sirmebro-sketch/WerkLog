package de.werklog.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AuditTest {
    private val a = Asset(name = "Testanlage", trade = "Wasser", location = "", note = "")
    @Test fun dueWorkAndDeliveriesIgnoreCompletedRecordsAndSortByDate() {
        val today = LocalDate.of(2026, 9, 30)
        val old = Entry(assetId = a.id, title = "Alt", note = "", dueDate = "29.09.2026")
        val later = old.copy(id = newId(), title = "Später", dueDate = "02.10.2026")
        val completed = old.copy(id = newId(), status = "Erledigt")
        val data = Data(assets = listOf(a), entries = listOf(later, completed, old), work = WorkData(orders = listOf(
            PartsOrder(title = "Aktive Lieferung", status = "Bestellt", delivery = "29.09.2026"),
            PartsOrder(title = "Geliefert", status = "Geliefert", delivery = "29.09.2026"))))
        assertEquals(listOf(old, later), dueEntries(data, today.plusDays(7)))
        assertTrue(entryDue(old, today)); assertFalse(entryDue(later, today)); assertFalse(entryDue(completed, today))
        assertEquals("Aktive Lieferung", dueOrders(data, today).single().title)
        assertEquals(data, decode(encode(data)))
    }
    @Test fun parentsExcludeDescendantsAndLongQrCodesRemainBounded() {
        val child = a.copy(id = newId(), parentId = a.id)
        val grandchild = a.copy(id = newId(), parentId = child.id)
        val other = a.copy(id = newId())
        assertEquals(listOf(other), validParents(listOf(a, child, grandchild, other), a.id))
        assertEquals("werklog:asset:${a.id}", assetCode(a.copy(tag = "x".repeat(5000))))
    }
    @Test fun oldRecurringDatesStillProduceCurrentOccurrences() {
        val event = Appointment(title = "Lang laufend", start = "01.01.1900 08:00", repeat = "Täglich")
        val dates = occurrences(event, LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1))
        assertEquals(listOf("30.09.2026 08:00", "01.10.2026 08:00"), dates.map { it.start })
        val month = event.copy(start = "31.01.2026 08:00", repeat = "Monatlich")
        assertEquals("28.02.2026 08:00", occurrences(month, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)).single().start)
    }
    @Test fun invalidDeadlineAndDuplicateIdsAreRejected() {
        val entry = Entry(assetId = a.id, title = "Test", note = "")
        assertThrows(IllegalArgumentException::class.java) { validateData(Data(assets = listOf(a), entries = listOf(entry, entry))) }
        assertThrows(IllegalArgumentException::class.java) { validateData(Data(assets = listOf(a), entries = listOf(entry.copy(dueDate = "31.02.2026")))) }
    }
}
