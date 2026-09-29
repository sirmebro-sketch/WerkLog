package de.werklog.app

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** File: magic (8), salt (16), IV (12), AES-GCM ciphertext with tag. */
object Vault {
    const val MAX_BYTES = 32 * 1024 * 1024
    private val magic = "WRKLOG01".toByteArray(Charsets.US_ASCII)
    private fun random(n: Int) = ByteArray(n).also { SecureRandom().nextBytes(it) }
    fun salt() = random(16)
    fun key(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, 310_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
    }
    fun encrypt(data: ByteArray, key: ByteArray, salt: ByteArray): ByteArray {
        require(data.size < MAX_BYTES - 64) { "Speichergrenze erreicht. Alte Daten extern sichern." }
        val iv = random(12)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        c.updateAAD(magic)
        return magic + salt + iv + c.doFinal(data)
    }
    fun saltOf(bytes: ByteArray): ByteArray {
        require(bytes.size in 52..MAX_BYTES && bytes.copyOfRange(0, 8).contentEquals(magic)) { "Keine gültige WerkLog-Sicherung" }
        return bytes.copyOfRange(8, 24)
    }
    fun decrypt(bytes: ByteArray, key: ByteArray): ByteArray {
        saltOf(bytes)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, bytes.copyOfRange(24, 36)))
        c.updateAAD(magic)
        return c.doFinal(bytes, 36, bytes.size - 36)
    }
}
