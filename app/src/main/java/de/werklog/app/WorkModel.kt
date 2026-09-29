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
    var session by mutableStateOf(0); private set
    fun lock() { generation++; key?.fill(0); key = null; salt = null; data = null; session++ }
    fun unlock(password: CharArray, backup: File? = null) {
        if (busy) { password.fill('\u0000'); return }
        busy = true; error = null
        val attempt = generation
        viewModelScope.launch {
            var derived: ByteArray? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    if (backup != null) return@withContext repository.restore(backup, password).also { derived = it.second }
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
                        data?.let { runCatching { Reminders.update(getApplication(), it) } }
                    }
                }
            } catch (_: Exception) { error = "Entsperren fehlgeschlagen. Passwort oder Sicherungsdatei prüfen. Vorhandene Daten wurden nicht absichtlich ersetzt." }
            finally { backup?.delete(); derived?.fill(0); password.fill('\u0000'); busy = false }
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
                getApplication<Application>().getSharedPreferences("biometric", 0).edit().clear().apply()
                java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("werklog-biometric-v1") }
                if (attempt == generation) { key?.fill(0); key = changed.second; resultKey = null; salt = changed.first; data = changed.third }
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
