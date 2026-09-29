package de.werklog.app

import org.junit.Assert.*
import org.junit.Test

class ExchangeTest {
    private val a = Asset(name = "Testanlage", trade = "Wasser", location = "PRIVATE_LOCATION", note = "PRIVATE_NOTE")
    private val info = AssetInfo(assetId = a.id, title = "Testinfo", body = "Testwissen")
    private val data = Data(assets = listOf(a), infos = listOf(info),
        credentials = listOf(Credential(assetId = a.id, title = "SECRET", username = "TESTUSER", password = "TESTPASSWORD")),
        entries = listOf(Entry(assetId = a.id, title = "Vorgang", note = "TESTHISTORY")))
    @Test fun selectionExcludesSecretsUnlessExplicitlyIncluded() {
        val normal = assetPackage(data, a.id, credentials = false, history = false)
        assertTrue(normal.credentials.isEmpty()); assertTrue(normal.entries.isEmpty()); assertEquals(1, normal.infos.size)
        assertEquals(1, assetPackage(data, a.id, credentials = true, history = true).credentials.size)
    }
    @Test fun individualKnowledgeSharesOnlyThatEntryAndBasicIdentity() {
        val pkg = assetPackage(data, a.id, credentials = true, history = true, infoId = info.id)
        assertTrue(pkg.credentials.isEmpty()); assertTrue(pkg.entries.isEmpty())
        assertEquals("", pkg.assets.single().location); assertEquals("", pkg.assets.single().note)
        assertEquals(listOf(info), pkg.infos)
    }
    @Test fun sharingRoundTripRejectsWrongCodeAndTampering() {
        val code = Exchange.newCode(); val bytes = Exchange.encrypt(data, code)
        assertEquals(data, Exchange.decrypt(bytes, code.lowercase().replace("-", " ")))
        assertFalse(String(bytes).contains("TESTPASSWORD"))
        assertThrows(Exception::class.java) { Exchange.decrypt(bytes, Exchange.newCode()) }
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { Exchange.decrypt(bytes, code) }
    }
    @Test fun backupCannotBeMistakenForShare() {
        val salt = Vault.salt(); val key = Vault.key("Beispielpasswort".toCharArray(), salt)
        val bytes = Vault.encrypt(encode(data), key, salt)
        assertThrows(IllegalArgumentException::class.java) { Exchange.checkEnvelope(bytes) }
        val share = Exchange.encrypt(data, Exchange.newCode())
        assertThrows(IllegalArgumentException::class.java) { Vault.saltOf(share) }
    }
    @Test fun importRemapsAllIdsWithoutOverwriting() {
        val merged = importPackage(data, data)
        assertEquals(2, merged.assets.size); assertEquals(data.assets.first(), merged.assets.first())
        val copied = merged.assets.last(); assertNotEquals(a.id, copied.id)
        assertEquals(copied.id, merged.credentials.last().assetId); assertNotEquals(data.credentials.first().id, merged.credentials.last().id)
        assertEquals(copied.id, merged.infos.last().assetId); assertEquals(copied.id, merged.entries.last().assetId)
        assertEquals(merged, decode(encode(merged)))
    }
}
