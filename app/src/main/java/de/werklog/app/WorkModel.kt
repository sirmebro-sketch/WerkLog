package de.werklog.app

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WorkModel(app: Application) : AndroidViewModel(app) {
    private val repository = LocalRepository(app.filesDir)
    private var key: ByteArray? = null
    private var salt: ByteArray? = null
    private var generation = 0
    var data by mutableStateOf<Data?>(null); private set
    var exists by mutableStateOf(repository.exists()); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    val workspacePage = androidx.compose.runtime.mutableIntStateOf(0)
    val workspaceTool = mutableStateOf<String?>(null)
    var session by mutableStateOf(0); private set
    var offerBiometric by mutableStateOf(false); private set
    fun dismissBiometricOffer() { offerBiometric = false }
    private var lastDraftHash: ByteArray? = null
    internal var draftRegistry: androidx.compose.runtime.saveable.SaveableStateRegistry? = null
    private val draftFile get() = File(getApplication<Application>().filesDir, "ui-draft.vault")
    internal fun checkpointDraft(state: Map<String, List<Any?>>? = null) {
        val k = key ?: return; val s = salt ?: return
        var raw: ByteArray? = null
        try {
            val values = state ?: draftRegistry?.performSave() ?: return
            raw = DraftState.encode(values, workspacePage.intValue, workspaceTool.value)
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(s + raw)
            if (draftFile.exists() && lastDraftHash?.contentEquals(digest) == true) return
            val encrypted = Vault.encrypt(raw, k, s)
            val atomic = AtomicFile(draftFile); val stream = atomic.startWrite()
            try { stream.write(encrypted); atomic.finishWrite(stream); lastDraftHash = digest } catch (e: Exception) { atomic.failWrite(stream); throw e }
        } catch (_: Exception) { error = "Der aktuelle Entwurf konnte nicht zwischengespeichert werden. Bitte freien Speicher prüfen." }
        finally { raw?.fill(0) }
    }
    internal fun restoreDraftState(): Map<String, List<Any?>>? {
        val k = key ?: return null
        if (!draftFile.exists()) return null
        var raw: ByteArray? = null
        return try {
            raw = Vault.decrypt(draftFile.readBounded(Vault.MAX_BYTES), k)
            val restored = DraftState.decode(raw)
            workspacePage.intValue = restored.first; workspaceTool.value = restored.second
            restored.third
        } catch (_: Exception) {
            // Preserve the encrypted original for recovery rather than overwriting it with an empty form.
            draftFile.renameTo(File(draftFile.parentFile, "ui-draft-recovery-${System.currentTimeMillis()}.vault"))
            error = "Der letzte Entwurf konnte nicht wiederhergestellt werden. Die verschlüsselte Entwurfsdatei wurde zur Wiederherstellung aufbewahrt."
            null
        } finally { raw?.fill(0) }
    }
    fun lock() { checkpointDraft(); draftRegistry = null; lastDraftHash = null; generation++; key?.fill(0); key = null; salt = null; data = null; session++ }
    fun unlock(password: CharArray, backup: File? = null) {
        if (busy) { password.fill('\u0000'); return }
        busy = true; error = null
        val attempt = generation
        val creating = !exists && backup == null
        viewModelScope.launch {
            var derived: ByteArray? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    if (backup != null) return@withContext repository.restore(backup, password).also { derived = it.second; draftFile.delete(); workspacePage.intValue = 0; workspaceTool.value = null }
                    val bytes = if (exists) repository.envelope() else null
                    val s = bytes?.let(Vault::saltOf) ?: Vault.salt()
                    val k = Vault.key(password, s); derived = k
                    val d = if (bytes == null) Data() else Vault.decrypt(bytes, k).let { raw -> try { decode(raw) } finally { raw.fill(0) } }
                    Triple(s, k, d)
                }
                if (attempt == generation) {
                    if (backup == null) {
                        withContext(Dispatchers.IO) {
                            repository.save(result.third, result.second, result.first)
                        }
                        exists = true
                    }
                    if (attempt == generation) {
                        salt = result.first; key = result.second; derived = null
                        val raw = Vault.decrypt(repository.envelope(), result.second)
                        data = try { decode(raw) } finally { raw.fill(0) }; exists = true; session++
                        if (creating) offerBiometric = true
                        data?.let { runCatching { Reminders.update(getApplication(), it) } }
                    }
                }
            } catch (_: Exception) { error = "Entsperren fehlgeschlagen. Passwort oder Sicherungsdatei prüfen. Vorhandene Daten wurden nicht absichtlich ersetzt." }
            finally { exists = repository.exists(); if (data == null) { key?.fill(0); key = null; salt = null }; backup?.delete(); derived?.fill(0); password.fill('\u0000'); busy = false }
        }
    }
    fun update(next: Data) {
        if (busy) { error = "Ein Speichervorgang läuft bereits. Bitte danach erneut bearbeiten."; return }
        val k = key?.copyOf() ?: return
        val s = salt?.copyOf() ?: return
        val attempt = generation
        busy = true
        viewModelScope.launch {
            try {
                val normalized = withContext(Dispatchers.IO) { repository.save(next, k, s) }
                if (generation == attempt) data = normalized
                runCatching { Reminders.update(getApplication(), normalized) }
            } catch (_: Exception) { error = "Speichern fehlgeschlagen. Änderung wurde nicht übernommen. Daten- und Bildlimit prüfen." }
            finally { k.fill(0); busy = false }
        }
    }
    suspend fun backup(target: File) = withContext(Dispatchers.IO) { target.outputStream().use { repository.export(it) } }
    suspend fun image(value: String): ByteArray {
        val k = key?.copyOf() ?: error("Tresor gesperrt")
        return try { withContext(Dispatchers.IO) { repository.image(value, k) } } finally { k.fill(0) }
    }
    fun importShare(opened: OpenShare, target: String?, replace: Boolean) {
        val current = data; val k = key?.copyOf(); val s = salt?.copyOf()
        if (busy || current == null || k == null || s == null) { k?.fill(0); opened.close(); return }
        busy = true; val attempt = generation
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val candidate = if (target == null) importPackage(current, opened.data) else mergePackage(current, opened.data, target, replace)
                    validateWork(candidate.work, candidate.assets.map { it.id }.toSet())
                    val replacements = mutableMapOf<String, String>()
                    for (image in imageValues(opened.data).filter { it.isNotEmpty() }.distinct()) {
                        val bytes = opened.image(image)
                        try { replacements[image] = repository.addImage(bytes, k) } finally { bytes.fill(0) }
                    }
                    val localized = mapImages(opened.data) { replacements[it] ?: it }
                    val next = if (target == null) {
                        // Reuse the single generated copy identity from the validated preview.
                        val id = candidate.assets.last().id
                        mergePackage(current.copy(assets = candidate.assets), localized, id, false)
                    } else mergePackage(current, localized, target, replace)
                    repository.save(next, k, s)
                }
                if (attempt == generation) data = result
            } catch (_: Exception) { error = "Import fehlgeschlagen. Bildlimit oder freien Speicher prüfen. Bestehende Einträge bleiben erhalten." }
            finally { k.fill(0); opened.close(); busy = false }
        }
    }
    fun changePassword(oldPassword: CharArray, nextPassword: CharArray) {
        val currentKey = key?.copyOf()
        if (busy || currentKey == null) { currentKey?.fill(0); oldPassword.fill('\u0000'); nextPassword.fill('\u0000'); return }
        busy = true; val attempt = generation
        viewModelScope.launch {
            var resultKey: ByteArray? = null
            try {
                val changed = withContext(Dispatchers.IO) {
                    require(nextPassword.size >= 10)
                    val checkKey = Vault.key(oldPassword, Vault.saltOf(repository.envelope()))
                    try { require(java.security.MessageDigest.isEqual(checkKey, currentKey)) } finally { checkKey.fill(0) }
                    repository.rekey(currentKey, nextPassword).also { resultKey = it.second }
                }
                getApplication<Application>().getSharedPreferences("backup", 0).edit().putLong("lastBackup", 0).apply()
                getApplication<Application>().getSharedPreferences("biometric", 0).edit().clear().apply()
                java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("werklog-biometric-v1") }
                if (attempt == generation) { key?.fill(0); key = changed.second; resultKey = null; salt = changed.first; data = changed.third; checkpointDraft() }
                else if (draftFile.exists()) {
                    val rawDraft = Vault.decrypt(draftFile.readBounded(Vault.MAX_BYTES), currentKey)
                    try {
                        val atomic = AtomicFile(draftFile); val stream = atomic.startWrite()
                        try { stream.write(Vault.encrypt(rawDraft, changed.second, changed.first)); atomic.finishWrite(stream) }
                        catch (e: Exception) { atomic.failWrite(stream); throw e }
                    } finally { rawDraft.fill(0) }
                }
                error = "Passwort geändert. Bitte neue Sicherung erstellen; ältere Sicherungen behalten ihr bisheriges Passwort."
            } catch (_: Exception) { error = "Passwortwechsel fehlgeschlagen. Aktuelles Passwort und freien Speicher prüfen." }
            finally { resultKey?.fill(0); currentKey.fill(0); oldPassword.fill('\u0000'); nextPassword.fill('\u0000'); busy = false }
        }
    }
    fun biometricKey(): ByteArray? = key?.copyOf()
    fun unlockKey(k: ByteArray) {
        if (busy) { k.fill(0); return }; busy = true; val attempt = generation
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { val bytes = repository.envelope(); val raw = Vault.decrypt(bytes, k); try { Vault.saltOf(bytes) to decode(raw) } finally { raw.fill(0) } }
                if (attempt == generation) { key = k.copyOf(); salt = result.first; data = result.second; session++; Reminders.update(getApplication(), result.second) }
            } catch (_: Exception) { error = "Biometrie passt nicht zu diesem Tresor. Bitte mit Passwort öffnen und neu aktivieren." }
            finally { k.fill(0); busy = false }
        }
    }
    fun storageBytes() = repository.bytesUsed()
    override fun onCleared() { lock() }
}
