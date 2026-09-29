package de.werklog.app

import org.junit.Assert.*
import org.junit.Test

class ModelTest {
    @Test fun germanDecimalsAndInvalidMeasurements() {
        assertEquals(1.25, number("1,25")!!, 0.0)
        assertEquals(-3.0, number("-3")!!, 0.0)
        listOf("NaN", "Infinity", "", "1,2,3", "1e999").forEach { assertNull(number(it)) }
    }
    @Test fun dataRoundTripPreservesAllRecords() {
        val asset = Asset(name = "Testanlage", trade = "Wasser", location = "Testraum", note = "äöü")
        val data = Data(assets = listOf(asset), entries = listOf(Entry(assetId = asset.id, title = "Test", note = "Notiz", minutes = 30)),
            readings = listOf(Reading(assetId = asset.id, label = "Druck", value = 2.3, unit = "bar", note = "")),
            rounds = listOf(Round(title = "Kontrolle", checks = listOf("Sichtkontrolle"))),
            runs = listOf(RoundRun(title = "Kontrolle", results = listOf("Ungeprüft · Sichtkontrolle"), note = "")))
        assertEquals(data, decode(encode(data)))
    }
    @Test fun orphanedEntriesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { decode(encode(Data(entries = listOf(Entry(assetId = "missing", title = "Test", note = ""))))) }
    }
    @Test fun unknownSchemaIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { decode("{\"schema\":999}".toByteArray()) }
    }
}
