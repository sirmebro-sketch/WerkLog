package de.werklog.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AssetDataTest {
    private val asset = Asset(name = "Prüfanlage", trade = "Kälte", location = "Test", note = "", manufacturer = "Beispielhersteller", model = "Testtyp",
        serial = "TEST-123", contact = "Testkontakt", spareParts = "Testfilter", nextService = "30.09.2026")
    @Test fun credentialsAndAssetInformationSurviveEncryptedBackup() {
        val d = Data(assets = listOf(asset), credentials = listOf(Credential(assetId = asset.id, title = "Panel", username = "test-user", password = " Nur-Test-Geheimnis! ", address = "test-address", note = "Testnotiz")),
            infos = listOf(AssetInfo(assetId = asset.id, title = "Plan", body = "Testablage")))
        val salt = Vault.salt(); val key = Vault.key("Beispielpasswort!".toCharArray(), salt)
        val encrypted = Vault.encrypt(encode(d), key, salt)
        assertFalse(String(encrypted).contains("Nur-Test-Geheimnis"))
        assertEquals(d, decode(Vault.decrypt(encrypted, key)))
    }
    @Test fun legacyVersionOneOpensWithoutLosingHistory() {
        val oldAsset = Asset(id = "legacy", name = "Altanlage", trade = "Wasser", location = "Raum", note = "Bestandsnotiz")
        val d = Data(assets = listOf(oldAsset), entries = listOf(Entry(assetId = oldAsset.id, title = "Alter Vorgang", note = "Historie")))
        val old = JSONObject(String(encode(d))).put("schema", 1)
        old.remove("credentials"); old.remove("infos")
        val row = old.getJSONArray("assets").getJSONObject(0)
        listOf("manufacturer", "model", "serial", "contact", "spareParts", "nextService").forEach { row.remove(it) }
        assertEquals(d, decode(old.toString().toByteArray()))
    }
    @Test fun credentialsMustBelongToAnAsset() {
        val d = Data(credentials = listOf(Credential(assetId = "missing", title = "Panel", username = "test", password = "test")))
        assertThrows(IllegalArgumentException::class.java) { decode(encode(d)) }
    }
    @Test fun schemaTwoCannotSilentlyLoseCredentialCollection() {
        val j = JSONObject(String(encode(Data()))); j.remove("credentials")
        assertThrows(Exception::class.java) { decode(j.toString().toByteArray()) }
    }
    @Test fun emailDoesNotContainCredentialsOrPrivateKnowledge() {
        val d = Data(assets = listOf(asset), entries = listOf(Entry(assetId = asset.id, title = "Sichtkontrolle", note = "Befund")),
            credentials = listOf(Credential(assetId = asset.id, title = "SECRET_TITLE", username = "SECRET_USER", password = "SECRET_PASSWORD", address = "SECRET_ADDRESS", note = "SECRET_NOTE")),
            infos = listOf(AssetInfo(assetId = asset.id, title = "SECRET_INFO", body = "SECRET_BODY")))
        val mail = handover(d)
        assertTrue(mail.contains("Sichtkontrolle")); assertFalse(mail.contains("SECRET_"))
        assertFalse(mail.contains("Testkontakt")); assertFalse(mail.contains("Testfilter"))
    }
    @Test fun serviceDatesAreStrictAndDueBoundariesCorrect() {
        assertNull(parseServiceDate("31.02.2026")); assertNull(parseServiceDate("29.02.2025"))
        assertNotNull(parseServiceDate("29.02.2028"))
        val today = LocalDate.of(2026, 9, 29)
        assertEquals("Überfällig", serviceState("28.09.2026", today))
        assertEquals("Heute fällig", serviceState("29.09.2026", today))
        assertEquals("In den nächsten 30 Tagen", serviceState("29.10.2026", today))
        assertEquals("Geplant", serviceState("30.10.2026", today))
    }
    @Test fun generatedPasswordsHaveRequiredCharacterGroups() {
        val passwords = (1..100).map { PasswordGenerator.generate() }
        assertEquals(100, passwords.toSet().size)
        passwords.forEach { p -> assertEquals(20, p.length); assertTrue(p.any { it.isUpperCase() }); assertTrue(p.any { it.isLowerCase() }); assertTrue(p.any { it.isDigit() }); assertTrue(p.any { !it.isLetterOrDigit() }) }
        assertThrows(IllegalArgumentException::class.java) { PasswordGenerator.generate(4) }
    }
}
