package de.werklog.app

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RepositoryTest {
    private val password = "Testpasswort-2026".toCharArray()
    private val jpeg = ByteArray(24 * 1024).also { java.util.Random(42).nextBytes(it); it[0] = 0xff.toByte(); it[1] = 0xd8.toByte() }
    private fun data(count: Int): Data = Data(work = WorkData(guides = (0 until count).chunked(100).map { ids -> Guide(title = "Anleitung", steps = ids.map { GuideStep(title = "Bild $it", body = "", image = Base64.getEncoder().encodeToString(jpeg)) }) }))
    @Test fun fiveHundredEncryptedImagesRoundTripWithoutInlineMetadata() {
        val root = Files.createTempDirectory("werklog").toFile(); val other = Files.createTempDirectory("restore").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val key = Vault.key(password, salt)
            val normalized = repo.save(data(500), key, salt)
            assertEquals(500, imageCount(normalized.work)); assertTrue(imageValues(normalized).all(::isImageRef))
            assertTrue(encode(normalized).size < 200000)
            val archive = File(root, "backup"); archive.outputStream().use(repo::export)
            assertTrue(archive.length() > 8L * 1024 * 1024)
            val restoredRepo = LocalRepository(other); val restored = restoredRepo.restore(archive, password)
            assertEquals(normalized, restored.third)
            imageValues(restored.third).forEach { assertArrayEquals(jpeg, restoredRepo.image(it, restored.second)) }
        } finally { root.deleteRecursively(); other.deleteRecursively() }
    }
    @Test fun assetCoverPhotoIsEncryptedAndIncludedInBackup() {
        val root = Files.createTempDirectory("cover").toFile(); val other = Files.createTempDirectory("cover-restore").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val key = Vault.key(password, salt)
            val asset = Asset(name = "Testanlage", trade = "Wasser", location = "", note = "", coverImage = Base64.getEncoder().encodeToString(jpeg))
            val stored = repo.save(Data(assets = listOf(asset)), key, salt)
            assertTrue(isImageRef(stored.assets.single().coverImage))
            val archive = File(root, "backup"); archive.outputStream().use(repo::export)
            val restoredRepo = LocalRepository(other); val restored = restoredRepo.restore(archive, password)
            assertArrayEquals(jpeg, restoredRepo.image(restored.third.assets.single().coverImage, restored.second))
        } finally { root.deleteRecursively(); other.deleteRecursively() }
    }
    @Test fun failedImageSaveRollsBackNewFilesAndLeavesExistingVaultReadable() {
        val root = Files.createTempDirectory("failed-save").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val key = Vault.key(password, salt)
            val a = Asset(name = "Test", trade = "Wasser", location = "", note = "")
            val before = repo.save(Data(assets = listOf(a)), key, salt)
            val active = File(root, File(root, "active").readText())
            File(active, "data.vault.tmp").mkdir()
            assertThrows(Exception::class.java) { repo.save(before.copy(assets = listOf(a.copy(coverImage = Base64.getEncoder().encodeToString(jpeg)))), key, salt) }
            assertEquals(0, active.listFiles()!!.count { it.extension == "image" })
            assertEquals(before, decode(Vault.decrypt(repo.envelope(), key)))
        } finally { root.deleteRecursively() }
    }
    @Test fun wrongPasswordOrMissingImageCannotReplaceExistingData() {
        val root = Files.createTempDirectory("werklog").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val key = Vault.key(password, salt); repo.save(data(1), key, salt)
            val before = repo.envelope(); val archive = File(root, "backup"); archive.outputStream().use(repo::export)
            assertThrows(Exception::class.java) { repo.restore(archive, "wrong".toCharArray()) }; assertArrayEquals(before, repo.envelope())
            ZipOutputStream(archive.outputStream()).use { it.putNextEntry(ZipEntry("data.vault")); it.write(before); it.closeEntry() }
            assertThrows(Exception::class.java) { repo.restore(archive, password) }; assertArrayEquals(before, repo.envelope())
        } finally { root.deleteRecursively() }
    }
    @Test fun oldVaultMigratesAndPasswordRotationKeepsImages() {
        val root = Files.createTempDirectory("werklog").toFile()
        try {
            val repo = LocalRepository(root); val salt = Vault.salt(); val key = Vault.key(password, salt)
            File(root, "werklog.vault").writeBytes(Vault.encrypt(encode(data(2)), key, salt))
            val normalized = repo.save(data(2), key, salt)
            val rotated = repo.rekey(key, "AnderesPasswort2026".toCharArray())
            assertEquals(normalized, rotated.third)
            assertThrows(Exception::class.java) { Vault.decrypt(repo.envelope(), key) }
            imageValues(rotated.third).forEach { assertArrayEquals(jpeg, repo.image(it, rotated.second)) }
        } finally { root.deleteRecursively() }
    }
    @Test fun imageTamperingAndSwappingAreRejected() {
        val key = ByteArray(32); val ref = "img:" + newId(); val encrypted = ImageCipher.encrypt(jpeg, key, ref)
        assertThrows(Exception::class.java) { ImageCipher.decrypt(encrypted, key, "img:" + newId()) }
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { ImageCipher.decrypt(encrypted, key, ref) }
    }
    @Test fun maliciousArchiveCannotEscapeStagingDirectory() {
        val root = Files.createTempDirectory("werklog").toFile()
        try {
            val archive = File(root, "evil"); ZipOutputStream(archive.outputStream()).use { it.putNextEntry(ZipEntry("../escape")); it.write(byteArrayOf(1)); it.closeEntry() }
            assertThrows(Exception::class.java) { LocalRepository(root).restore(archive, password) }
            assertFalse(File(root, "escape").exists())
        } finally { root.deleteRecursively() }
    }
    @Test fun largeColleagueArchiveKeepsImagesAndSeparateCode() = kotlinx.coroutines.runBlocking<Unit> {
        val root = Files.createTempDirectory("share-test").toFile()
        try {
            val repo = LocalRepository(File(root, "original")); val salt = Vault.salt(); val key = Vault.key(password, salt)
            val a = Asset(name = "Testanlage", trade = "Wasser", location = "", note = "")
            val raw = data(250); val d = repo.save(raw.copy(assets = listOf(a), work = raw.work.copy(guides = raw.work.guides.map { it.copy(assetId = a.id) })), key, salt)
            val code = Exchange.newCode(); val output = File(root, "anlage.werkshare")
            ShareArchive.create(d, code, { repo.image(it, key) }, output, root)
            ShareArchive.open(output, code.lowercase(), root).use { opened ->
                assertEquals(250, imageCount(opened.data.work))
                imageValues(opened.data).forEach { assertArrayEquals(jpeg, opened.image(it)) }
            }
            assertThrows(Exception::class.java) { ShareArchive.open(output, Exchange.newCode(), root) }
        } finally { root.deleteRecursively() }
    }

}
