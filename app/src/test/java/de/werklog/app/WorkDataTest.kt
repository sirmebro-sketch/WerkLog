package de.werklog.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WorkDataTest {
    private val asset = Asset(id = "asset", name = "Test", trade = "Wasser", location = "", note = "")
    @Test fun workModulesRoundTripAndMeterAssignment() {
        val meter = Meter(assetId = asset.id, name = "Testzähler", unit = "m³")
        val w = WorkData(meters = listOf(meter), appointments = listOf(Appointment(title = "Service", start = "30.09.2026 08:30", company = "Testfirma", responsible = "Testperson")),
            guides = listOf(Guide(title = "Allgemeine Anleitung", steps = listOf(GuideStep(title = "Schritt", body = "Beschreibung")))),
            orders = listOf(PartsOrder(title = "Testbedarf", items = listOf(OrderItem(name = "Filter", quantity = "2", assetId = asset.id)))))
        val d = Data(assets = listOf(asset), readings = listOf(Reading(assetId = asset.id, label = meter.name, value = 1.2, unit = "m³", note = "", meterId = meter.id)), work = w)
        assertEquals(d, decode(encode(d)))
    }
    @Test fun schemaTwoMigrationPreservesCredentials() {
        val d = Data(assets = listOf(asset), credentials = listOf(Credential(assetId = asset.id, title = "Panel", username = "u", password = "p")))
        val j = JSONObject(String(encode(d))).put("schema", 2); j.remove("work")
        assertEquals(d, decode(j.toString().toByteArray()))
    }
    @Test fun appointmentInvalidDatesAndDurationsAreRejected() {
        assertNull(appointmentTime("31.02.2026 10:00")); assertNull(appointmentTime("30.09.2026 25:00"))
        assertNotNull(appointmentTime("30.09.2026 08:30"))
        val w = WorkData(appointments = listOf(Appointment(title = "Test", start = "30.09.2026 08:30", minutes = 0)))
        assertThrows(IllegalArgumentException::class.java) { validateWork(w, emptySet()) }
    }
    @Test fun meterMustBelongToSameAssetAsReading() {
        val meter = Meter(assetId = asset.id, name = "Zähler", unit = "kWh")
        val other = asset.copy(id = "other")
        val d = Data(assets = listOf(asset, other), work = WorkData(meters = listOf(meter)), readings = listOf(Reading(assetId = other.id, label = "Messung", value = 1.0, unit = "kWh", note = "", meterId = meter.id)))
        assertThrows(IllegalArgumentException::class.java) { decode(encode(d)) }
    }
    @Test fun orderReportDoesNotLeakLocalAssetIdOrCredentials() {
        val order = PartsOrder(title = "Bedarf", items = listOf(OrderItem(name = "Filter", quantity = "2", reason = "Ersatz", assetId = "SECRET_ASSET_ID")))
        assertTrue(orderText(order).contains("2 Stück · Filter")); assertFalse(orderText(order).contains("SECRET_ASSET_ID"))
    }
    @Test fun ocrCandidatesStaySuggestionsWithoutGuessingDecimalPosition() {
        assertEquals(listOf("001234", "56,7", "12.5"), meterCandidates("001234 kWh\n56,7\n12.5\n001234"))
        assertTrue(meterCandidates("Unlesbar").isEmpty())
    }
    @Test fun assetExchangeExcludesAppointmentsAndOrdersButIncludesLinkedGuides() {
        val meter = Meter(assetId = asset.id, name = "M", unit = "kWh")
        val data = Data(assets = listOf(asset), work = WorkData(meters = listOf(meter), guides = listOf(Guide(title = "Anlagenwissen", assetId = asset.id, steps = listOf(GuideStep(title = "S", body = "B")))),
            appointments = listOf(Appointment(title = "Privater Termin", start = "30.09.2026 08:00")), orders = listOf(PartsOrder(title = "Private Bestellung"))),
            readings = listOf(Reading(assetId = asset.id, label = "M", value = 10.0, unit = "kWh", note = "", meterId = meter.id)))
        val pkg = assetPackage(data, asset.id, credentials = false, history = true)
        assertTrue(pkg.work.orders.isEmpty()); assertTrue(pkg.work.appointments.isEmpty()); assertEquals(1, pkg.work.guides.size)
        val imported = importPackage(data, pkg)
        assertEquals(1, imported.work.orders.size); assertEquals(1, imported.work.appointments.size)
        assertEquals(imported.work.meters.last().id, imported.readings.last().meterId)
        assertEquals(imported, decode(encode(imported)))
    }
    @Test fun oversizedStoredImageRejected() {
        val tooLarge = "A".repeat(MAX_IMAGE_BYTES * 2)
        val w = WorkData(guides = listOf(Guide(title = "Test", steps = listOf(GuideStep(title = "S", body = "", image = tooLarge)))))
        assertThrows(IllegalArgumentException::class.java) { validateWork(w, emptySet()) }
    }
    @Test fun totalImageBudgetRejectsOversizedImports() {
        val jpegHeader = java.util.Base64.getEncoder().encodeToString(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte()))
        val w = WorkData(guides = listOf(Guide(title = "Test", steps = (1..31).map { GuideStep(title = "Schritt $it", body = "", image = jpegHeader) })))
        assertThrows(IllegalArgumentException::class.java) { validateWork(w, emptySet()) }
    }

}
