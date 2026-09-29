package de.werklog.app

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.nio.file.Files
import java.io.File
import java.util.Base64

class LocalProfileTest {
    private val photo = Base64.getEncoder().encodeToString(byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0, 0))
    private val profile = LocalProfile("Alex", "Servicetechnik", "Kraftwerk", "Testbetrieb", "123", "alex@example.invalid", photo)
    @Test fun legacyVaultGetsEmptyProfileAndNewProfileRoundTrips() {
        val legacy = JSONObject(String(encode(Data()))).put("schema", 4).apply { remove("profile") }
        assertEquals(LocalProfile(), decode(legacy.toString().toByteArray()).profile)
        assertEquals(profile, decode(encode(Data(profile = profile))).profile)
    }
    @Test fun sharesExcludeIdentityAndImportPreservesReceiverProfile() {
        val a = Asset(name = "Test", trade = "Dampf", location = "", note = "")
        val pack = assetPackage(Data(assets = listOf(a), profile = profile), a.id, true, true)
        assertEquals(LocalProfile(), pack.profile)
        assertFalse(String(encode(pack)).contains("alex@example.invalid"))
        assertEquals(profile, importPackage(Data(profile = profile), pack.copy(profile = LocalProfile("Fremd"))).profile)
    }
    @Test fun oversizedProfilePictureIsRejected() {
        val bytes = ByteArray(MAX_PROFILE_IMAGE_BYTES + 1).also { it[0] = 0xff.toByte(); it[1] = 0xd8.toByte() }
        assertThrows(IllegalArgumentException::class.java) { decode(encode(Data(profile = profile.copy(image = Base64.getEncoder().encodeToString(bytes))))) }
    }
    @Test fun encryptedBackupPreservesProfile() {
        val root = Files.createTempDirectory("profile-vault").toFile()
        val restore = Files.createTempDirectory("profile-restore").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val password = "Testpasswort2026".toCharArray(); val key = Vault.key(password, salt)
            repo.save(Data(profile = profile), key, salt)
            val backup = File(root, "backup"); backup.outputStream().use(repo::export)
            assertEquals(profile, LocalRepository(restore).restore(backup, password).third.profile)
            key.fill(0); password.fill('\u0000')
        } finally { root.deleteRecursively(); restore.deleteRecursively() }
    }
}
