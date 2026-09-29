package de.werklog.app

import org.junit.Assert.*
import org.junit.Test

class VaultTest {
    @Test fun roundTripAndWrongPassword() {
        val salt = Vault.salt()
        val key = Vault.key("Langes Testpasswort!".toCharArray(), salt)
        val encrypted = Vault.encrypt("Sensibler Anlagenbericht".toByteArray(), key, salt)
        assertEquals("Sensibler Anlagenbericht", String(Vault.decrypt(encrypted, key)))
        assertFalse(String(encrypted).contains("Sensibler Anlagenbericht"))
        val wrong = Vault.key("Falsches Testpasswort".toCharArray(), salt)
        assertThrows(Exception::class.java) { Vault.decrypt(encrypted, wrong) }
    }
    @Test fun tamperingIsRejected() {
        val salt = Vault.salt(); val key = Vault.key("Testpasswort lang genug".toCharArray(), salt)
        val encrypted = Vault.encrypt("Daten".toByteArray(), key, salt)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { Vault.decrypt(encrypted, key) }
    }
    @Test fun everySaveUsesFreshNonce() {
        val salt = Vault.salt(); val key = Vault.key("Testpasswort lang genug".toCharArray(), salt)
        assertFalse(Vault.encrypt(byteArrayOf(1), key, salt).contentEquals(Vault.encrypt(byteArrayOf(1), key, salt)))
    }
    @Test fun malformedEnvelopeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Vault.saltOf(ByteArray(50)) }
        assertThrows(IllegalArgumentException::class.java) { Vault.saltOf(ByteArray(Vault.MAX_BYTES + 1)) }
    }
}
