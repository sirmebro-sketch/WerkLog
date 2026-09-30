package de.werklog.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import org.junit.Assert.*
import org.junit.Test

class DraftStateTest {
    @Test fun encryptedRoundTripPreservesFormValuesAndSelectedRecord() {
        val asset = Asset(name = "Testanlage", trade = "Dampf", location = "Ort", note = "")
        val original = mapOf("text" to listOf(mutableStateOf("Unfertige Notiz")), "record" to listOf(mutableStateOf(asset)), "scroll" to listOf(320))
        val raw = DraftState.encode(original, 4, "Anleitungen")
        val salt = Vault.salt(); val key = ByteArray(32) { it.toByte() }
        val encrypted = Vault.encrypt(raw, key, salt)
        assertFalse(encrypted.toString(Charsets.UTF_8).contains("Unfertige Notiz"))
        val restored = DraftState.decode(Vault.decrypt(encrypted, key))
        assertEquals(4, restored.first); assertEquals("Anleitungen", restored.second)
        assertEquals("Unfertige Notiz", (restored.third.getValue("text").single() as MutableState<*>).value)
        assertEquals(asset, (restored.third.getValue("record").single() as MutableState<*>).value)
        assertEquals(320, restored.third.getValue("scroll").single())
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { Vault.decrypt(encrypted, key) }
        raw.fill(0); key.fill(0)
    }
}
