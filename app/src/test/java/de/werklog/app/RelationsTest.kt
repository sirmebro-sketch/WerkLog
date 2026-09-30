package de.werklog.app

import org.junit.Assert.*
import org.junit.Test

class RelationsTest {
    private val asset = Asset(name = "Testpumpe", trade = "Wasser", location = "Test", note = "")
    private val entry = Entry(assetId = asset.id, title = "Leck", note = "Interner Hintergrund")
    @Test fun incidentCreatesEmptyOrderWithSeparatePrivateContext() {
        val order = orderFromEntry(entry)
        assertTrue(order.items.isEmpty()); assertEquals(entry.id, order.entryId); assertEquals(asset.id, order.assetId)
        assertEquals(entry.note, order.context)
        assertFalse(orderText(order).contains(entry.note))
        val data = Data(assets = listOf(asset), entries = listOf(entry), work = WorkData(orders = listOf(order)))
        assertEquals(data, decode(encode(data)))
    }
    @Test fun contactsAndTradesSurviveBackupAndDeletionPrunesLinks() {
        val contact = Contact(name = "Testkontakt", assetIds = listOf(asset.id), entryIds = listOf(entry.id))
        val data = Data(assets = listOf(asset), entries = listOf(entry), contacts = listOf(contact), work = WorkData(orders = listOf(orderFromEntry(entry))))
        assertEquals(data, decode(encode(data)))
        val renamed = renameTrade(data, "Wasser", "Wasseraufbereitung")
        assertEquals("Wasseraufbereitung", renamed.assets.single().trade); assertFalse("Wasser" in renamed.tradeNames)
        val removed = removeAsset(data, asset.id)
        assertTrue(removed.contacts.single().assetIds.isEmpty()); assertTrue(removed.contacts.single().entryIds.isEmpty())
        assertEquals("", removed.work.orders.single().assetId); assertEquals("", removed.work.orders.single().entryId)
        assertEquals(removed, decode(encode(removed)))
        assertTrue(removeEntry(data, entry.id).contacts.single().entryIds.isEmpty())
    }
    @Test fun assetImagesParticipateInEncryptedImageMappingAndLimit() {
        val image = java.util.Base64.getEncoder().encodeToString(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0, 0))
        val data = attachImage(Data(assets = listOf(asset)), PhotoTarget("cover", asset.id), image)
        assertEquals(listOf(image), imageValues(data))
        assertEquals(data, decode(encode(data)))
        assertEquals("", mapImages(data) { "" }.assets.single().coverImage)
        assertThrows(IllegalArgumentException::class.java) { validateImages(data.copy(assets = List(501) { asset.copy(id = newId(), coverImage = image) })) }
    }
}
