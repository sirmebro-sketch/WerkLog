package de.werklog.app

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

const val MAX_ARCHIVE_BYTES = 384L * 1024 * 1024
fun isImageRef(s: String) = Regex("img:[a-f0-9-]{36}").matches(s)
fun imageValues(d: Data) = (d.assets.map { it.coverImage } + d.work.guides.flatMap { it.steps.map { s -> s.image } } + d.work.orders.flatMap { it.items.map { x -> x.image } }).filter { it.isNotEmpty() }
fun mapImages(d: Data, transform: (String) -> String): Data = d.copy(assets = d.assets.map { it.copy(coverImage = transform(it.coverImage)) }, work = d.work.copy(
    guides = d.work.guides.map { g -> g.copy(steps = g.steps.map { it.copy(image = transform(it.image)) }) },
    orders = d.work.orders.map { o -> o.copy(items = o.items.map { it.copy(image = transform(it.image)) }) }))
fun InputStream.copyBounded(out: OutputStream, limit: Long): Long {
    val buffer = ByteArray(32768); var count = 0L
    while (true) { val n = read(buffer); if (n < 0) return count; count += n; require(count <= limit) { "Datei zu groß" }; out.write(buffer, 0, n) }
}
fun File.readBounded(limit: Int): ByteArray = inputStream().use { i -> ByteArrayOutputStream().also { i.copyBounded(it, limit.toLong()) }.toByteArray() }
private fun atomicWrite(file: File, bytes: ByteArray) {
    val tmp = File(file.parentFile, file.name + ".tmp")
    FileOutputStream(tmp).use { it.write(bytes); it.fd.sync() }
    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
}
object ImageCipher {
    fun encrypt(bytes: ByteArray, key: ByteArray, ref: String): ByteArray {
        require(isImageRef(ref) && bytes.size <= MAX_IMAGE_BYTES)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        c.updateAAD(("WRKIMG01:" + ref).toByteArray()); return iv + c.doFinal(bytes)
    }
    fun decrypt(bytes: ByteArray, key: ByteArray, ref: String): ByteArray {
        require(isImageRef(ref) && bytes.size in 28..MAX_IMAGE_BYTES + 28)
        val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        c.updateAAD(("WRKIMG01:" + ref).toByteArray()); return c.doFinal(bytes, 12, bytes.size - 12)
    }
}
/** Immutable encrypted images; metadata and generation pointer committed atomically. No plaintext archive. */
class LocalRepository(private val root: File) {
    private val pointer = File(root, "active")
    private val legacy = File(root, "werklog.vault")
    init { root.mkdirs() }
    private fun active(): File? = if (pointer.exists()) {
        val name = pointer.readText(); require(Regex("store-[a-f0-9-]{36}").matches(name)); File(root, name)
    } else null
    fun exists() = active()?.let { File(it, "data.vault").exists() } ?: legacy.exists()
    @Synchronized fun envelope(): ByteArray = (active()?.let { File(it, "data.vault") } ?: legacy).readBounded(Vault.MAX_BYTES)
    private fun imageFile(dir: File, ref: String): File { require(isImageRef(ref)); return File(dir, ref.substring(4) + ".image") }
    @Synchronized fun image(ref: String, key: ByteArray): ByteArray = if (!isImageRef(ref)) Base64.getDecoder().decode(ref) else {
        ImageCipher.decrypt(imageFile(active() ?: error("Bildablage fehlt"), ref).readBounded(MAX_IMAGE_BYTES + 28), key, ref)
    }
    private fun persist(dir: File, data: Data, key: ByteArray, salt: ByteArray): Data {
        validateImages(data)
        validateWork(data.work, data.assets.map { it.id }.toSet())
        val normalized = mapImages(data) { image ->
            when { image.isEmpty() -> ""; isImageRef(image) -> { require(imageFile(dir, image).isFile); image }; else -> {
                val ref = "img:" + newId(); val raw = Base64.getDecoder().decode(image)
                try { atomicWrite(imageFile(dir, ref), ImageCipher.encrypt(raw, key, ref)) } finally { raw.fill(0) }; ref
            } }
        }
        val raw = encode(normalized)
        try { decode(raw); atomicWrite(File(dir, "data.vault"), Vault.encrypt(raw, key, salt)) } finally { raw.fill(0) }
        val used = imageValues(normalized).filter(::isImageRef).map { it.substring(4) + ".image" }.toSet()
        dir.listFiles()?.filter { it.extension == "image" && it.name !in used }?.forEach { it.delete() }
        return normalized
    }
    @Synchronized fun save(data: Data, key: ByteArray, salt: ByteArray): Data {
        val current = active()
        if (current != null) return persist(current, data, key, salt)
        val dir = File(root, "store-" + newId()).also { check(it.mkdir()) }
        try { val result = persist(dir, data, key, salt); activate(dir); return result }
        catch (e: Exception) { dir.deleteRecursively(); throw e }
    }
    private fun activate(dir: File) {
        atomicWrite(pointer, dir.name.toByteArray())
        // Old generations are only deleted after the new pointer is durable.
        root.listFiles()?.filter { it.isDirectory && it.name.startsWith("store-") && it != dir }?.forEach { it.deleteRecursively() }
        legacy.delete()
    }
    @Synchronized fun export(out: OutputStream) {
        val dir = active()
        ZipOutputStream(out).use { zip ->
            if (dir == null) { zip.putNextEntry(ZipEntry("data.vault")); legacy.inputStream().use { it.copyTo(zip) }; zip.closeEntry() }
            else dir.listFiles()!!.filter { it.name == "data.vault" || it.extension == "image" }.sortedBy { it.name }.forEach { f ->
                zip.putNextEntry(ZipEntry(f.name)); f.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }
    @Synchronized fun restore(source: File, password: CharArray): Triple<ByteArray, ByteArray, Data> {
        val dir = File(root, "store-" + newId()).also { check(it.mkdir()) }; var key: ByteArray? = null
        try {
            val signature = source.inputStream().use { it.readNBytesCompat(8) }
            if (signature.contentEquals("WRKLOG01".toByteArray())) source.inputStream().use { i -> File(dir, "data.vault").outputStream().use { i.copyBounded(it, Vault.MAX_BYTES.toLong()) } }
            else ZipInputStream(source.inputStream().buffered()).use { zip ->
                val names = mutableSetOf<String>(); var total = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break; val name = entry.name
                    require(names.add(name) && names.size <= MAX_IMAGES + 1 && !entry.isDirectory)
                    require(name == "data.vault" || Regex("[a-f0-9-]{36}\\.image").matches(name))
                    total += File(dir, name).outputStream().use { zip.copyBounded(it, if (name == "data.vault") Vault.MAX_BYTES.toLong() else (MAX_IMAGE_BYTES + 28).toLong()) }
                    require(total <= MAX_ARCHIVE_BYTES); zip.closeEntry()
                }
            }
            val bytes = File(dir, "data.vault").readBounded(Vault.MAX_BYTES); val salt = Vault.saltOf(bytes)
            val k = Vault.key(password, salt); key = k
            val raw = Vault.decrypt(bytes, k); val data = try { decode(raw) } finally { raw.fill(0) }
            imageValues(data).filter(::isImageRef).distinct().forEach { ref ->
                val plain = ImageCipher.decrypt(imageFile(dir, ref).readBounded(MAX_IMAGE_BYTES + 28), k, ref)
                try { validateImageBytes(plain) } finally { plain.fill(0) }
            }
            val normalized = persist(dir, data, k, salt); activate(dir); key = null
            return Triple(salt, k, normalized)
        } catch (e: Exception) { dir.deleteRecursively(); throw e } finally { key?.fill(0) }
    }
    @Synchronized fun addImage(bytes: ByteArray, key: ByteArray): String {
        validateImageBytes(bytes); val dir = active() ?: error("Tresor fehlt")
        val ref = "img:" + newId(); atomicWrite(imageFile(dir, ref), ImageCipher.encrypt(bytes, key, ref)); return ref
    }
    @Synchronized fun rekey(oldKey: ByteArray, password: CharArray): Triple<ByteArray, ByteArray, Data> {
        val bytes = envelope(); val plain = Vault.decrypt(bytes, oldKey)
        val data = try { decode(plain) } finally { plain.fill(0) }
        val salt = Vault.salt(); val key = Vault.key(password, salt)
        val dir = File(root, "store-" + newId()).also { check(it.mkdir()) }
        try {
            imageValues(data).filter(::isImageRef).distinct().forEach { ref ->
                val raw = image(ref, oldKey)
                try { atomicWrite(imageFile(dir, ref), ImageCipher.encrypt(raw, key, ref)) } finally { raw.fill(0) }
            }
            val result = persist(dir, data, key, salt); activate(dir); return Triple(salt, key, result)
        } catch (e: Exception) { dir.deleteRecursively(); key.fill(0); throw e }
    }
    @Synchronized fun bytesUsed(): Long = active()?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: legacy.length()
}
private fun InputStream.readNBytesCompat(n: Int): ByteArray { val out = ByteArray(n); var count = 0; while (count < n) { val r = read(out, count, n - count); if (r < 0) break; count += r }; return out.copyOf(count) }

/** Forward-only image-by-image share package; no decrypted archive on disk. */
class OpenShare(val data: Data, private val repository: LocalRepository?, private val key: ByteArray?, private val folder: File?) : Closeable {
    fun image(value: String): ByteArray = if (isImageRef(value)) repository!!.image(value, key!!) else Base64.getDecoder().decode(value)
    override fun close() { key?.fill(0); folder?.deleteRecursively() }
}
object ShareArchive {
    private val header = "WRKSHR02".toByteArray()
    private fun normalized(code: String) = code.filterNot { it.isWhitespace() || it == '-' }.uppercase(java.util.Locale.ROOT).also { require(it.length == 20 && it.all { c -> c in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" }) }.toCharArray()
    suspend fun create(data: Data, code: String, load: suspend (String) -> ByteArray, output: File, cache: File) {
        require(data.assets.size == 1)
        val folder = File(cache, "share-build-" + newId()); val repo = LocalRepository(folder)
        val salt = Vault.salt(); val chars = normalized(code); val key = try { Vault.key(chars, salt) } finally { chars.fill('\u0000') }
        try {
            repo.save(mapImages(data) { "" }, key, salt)
            val refs = mutableMapOf<String, String>()
            for (value in imageValues(data).filter { it.isNotEmpty() }.distinct()) {
                val raw = if (isImageRef(value)) load(value) else Base64.getDecoder().decode(value)
                try { refs[value] = repo.addImage(raw, key) } finally { raw.fill(0) }
            }
            repo.save(mapImages(data) { refs[it] ?: "" }, key, salt)
            output.outputStream().use { out -> out.write(header); repo.export(out) }
        } finally { key.fill(0); folder.deleteRecursively() }
    }
    fun open(source: File, code: String, cache: File): OpenShare {
        val folder = File(cache, "share-read-" + newId()).also { it.mkdirs() }
        try {
            val signature = source.inputStream().use { it.readNBytesCompat(8) }
            if (!signature.contentEquals(header)) {
                val d = Exchange.decrypt(source.readBounded(Vault.MAX_BYTES), code)
                folder.deleteRecursively(); return OpenShare(d, null, null, null)
            }
            val archive = File(folder, "payload")
            source.inputStream().use { input -> require(input.skip(8) == 8L); archive.outputStream().use { input.copyBounded(it, MAX_ARCHIVE_BYTES) } }
            val repo = LocalRepository(File(folder, "content")); val chars = normalized(code)
            val loaded = try { repo.restore(archive, chars) } finally { chars.fill('\u0000'); archive.delete() }
            try {
                val d = loaded.third
                require(d.assets.size == 1 && d.rounds.isEmpty() && d.runs.isEmpty() && d.work.orders.isEmpty() && d.work.appointments.isEmpty() && d.work.templates.isEmpty())
                require(d.work.guides.all { it.assetId == d.assets.single().id })
                return OpenShare(d, repo, loaded.second, folder)
            } catch (e: Exception) { loaded.second.fill(0); throw e }
        } catch (e: Exception) { folder.deleteRecursively(); throw e }
    }
}
