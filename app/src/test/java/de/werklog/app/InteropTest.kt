package de.werklog.app
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64
class InteropTest {
    @Test fun sharedAppleVectorAndUnicodePassword() {
        val j = JSONObject(javaClass.getResourceAsStream("/interop.json")!!.bufferedReader().use { it.readText() })
        fun bytes(name: String) = Base64.getDecoder().decode(j.getString(name))
        val key = Vault.key(j.getString("password").toCharArray(), bytes("salt"))
        assertArrayEquals(bytes("plaintext"), Vault.decrypt(bytes("metadata"), key))
        assertArrayEquals(bytes("image"), ImageCipher.decrypt(bytes("encryptedImage"), key, j.getString("reference")))
    }
}
