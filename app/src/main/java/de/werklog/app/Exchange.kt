package de.werklog.app

import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** A distinct envelope keeps colleague packages separate from complete device backups. */
object Exchange {
    private val magic = "WRKSHR01".toByteArray(Charsets.US_ASCII)
    private const val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    fun newCode(): String {
        val random = SecureRandom()
        return (1..20).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("").chunked(4).joinToString("-")
    }
    private fun normalize(code: String): CharArray {
        val text = code.filterNot { it.isWhitespace() || it == '-' }.uppercase(java.util.Locale.ROOT)
        require(text.length == 20 && text.all { it in alphabet }) { "Ungültiger Freigabecode" }
        return text.toCharArray()
    }
    fun checkEnvelope(bytes: ByteArray) {
        require(bytes.size in 52..Vault.MAX_BYTES && bytes.copyOfRange(0, 8).contentEquals(magic)) { "Keine WerkLog-Anlagenfreigabe" }
    }
    fun encrypt(data: Data, code: String): ByteArray {
        require(data.assets.size == 1)
        val salt = Vault.salt(); val chars = normalize(code)
        val key = try { Vault.key(chars, salt) } finally { chars.fill('\u0000') }
        val raw = JSONObject().put("format", "WerkLog-Anlagenpaket").put("version", 1)
            .put("created", System.currentTimeMillis()).put("data", JSONObject(String(encode(data), Charsets.UTF_8))).toString().toByteArray(Charsets.UTF_8)
        try {
            require(raw.size < Vault.MAX_BYTES - 64) { "Paket zu groß" }
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv)); cipher.updateAAD(magic)
            return magic + salt + iv + cipher.doFinal(raw)
        } finally { key.fill(0); raw.fill(0) }
    }
    fun decrypt(bytes: ByteArray, code: String): Data {
        checkEnvelope(bytes)
        val chars = normalize(code)
        val key = try { Vault.key(chars, bytes.copyOfRange(8, 24)) } finally { chars.fill('\u0000') }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, bytes.copyOfRange(24, 36))); cipher.updateAAD(magic)
            val raw = cipher.doFinal(bytes, 36, bytes.size - 36)
            try {
                val j = JSONObject(String(raw, Charsets.UTF_8))
                require(j.getString("format") == "WerkLog-Anlagenpaket" && j.getInt("version") == 1)
                return decode(j.getJSONObject("data").toString().toByteArray(Charsets.UTF_8)).also {
                    require(imageValues(it).none(::isImageRef))
                    require(it.assets.size == 1 && it.rounds.isEmpty() && it.runs.isEmpty() && it.work.orders.isEmpty() && it.work.appointments.isEmpty())
                    require(it.work.guides.all { g -> g.assetId == it.assets.single().id })
                }
            } finally { raw.fill(0) }
        } finally { key.fill(0) }
    }
}
fun assetPackage(data: Data, id: String, credentials: Boolean, history: Boolean, infoId: String? = null): Data {
    val asset = data.assets.single { it.id == id }
    return Data(assets = listOf(if (infoId == null) asset.copy(parentId = "", favorite = false, lastOpened = 0) else Asset(id = asset.id, name = asset.name, trade = asset.trade, location = "", note = "")),
        entries = if (history && infoId == null) data.entries.filter { it.assetId == id } else emptyList(),
        readings = if (history && infoId == null) data.readings.filter { it.assetId == id } else emptyList(),
        credentials = if (credentials && infoId == null) data.credentials.filter { it.assetId == id } else emptyList(),
        infos = data.infos.filter { it.assetId == id && (infoId == null || it.id == infoId) },
        work = if (infoId != null) WorkData() else WorkData(
            meters = if (history) data.work.meters.filter { it.assetId == id } else emptyList(),
            guides = data.work.guides.filter { it.assetId == id }))
}
/** Import as a new local copy; never replace the receiver's existing asset or records. */
fun importPackage(current: Data, incoming: Data): Data {
    require(incoming.assets.size == 1)
    val old = incoming.assets.single(); val id = newId()
    return mergePackage(current.copy(assets = current.assets + old.copy(id = id, name = "${old.name} (Import)", parentId = "", favorite = false, lastOpened = 0)), incoming, id, false)
}
