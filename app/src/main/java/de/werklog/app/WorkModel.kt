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
    private val file = AtomicFile(File(app.filesDir, "werklog.vault"))
    private var key: ByteArray? = null
    private var salt: ByteArray? = null
    private var generation = 0
    var data by mutableStateOf<Data?>(null); private set
    var exists by mutableStateOf(file.baseFile.exists()); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null)
    var session by mutableStateOf(0); private set
    fun lock() { generation++; key?.fill(0); key = null; salt = null; data = null; session++ }
    private fun write(bytes: ByteArray) {
        val out = file.startWrite()
        try { out.write(bytes); file.finishWrite(out) } catch (e: Exception) { file.failWrite(out); throw e }
    }
    fun unlock(password: CharArray, backup: ByteArray? = null) {
        if (busy) { password.fill('\u0000'); return }
        busy = true; error = null
        val attempt = generation
        viewModelScope.launch {
            var derived: ByteArray? = null
            try {
                val result = withContext(Dispatchers.IO) {
                    val bytes = backup ?: if (exists) file.readFully() else null
                    val s = bytes?.let(Vault::saltOf) ?: Vault.salt()
                    val k = Vault.key(password, s); derived = k
                    val d = if (bytes == null) Data() else Vault.decrypt(bytes, k).let { raw -> try { decode(raw) } finally { raw.fill(0) } }
                    Triple(s, k, d)
                }
                if (attempt == generation) {
                    if (backup != null || !exists) {
                        withContext(Dispatchers.IO) {
                            write(backup ?: Vault.encrypt(encode(result.third), result.second, result.first))
                        }
                        exists = true
                    }
                    if (attempt == generation) {
                        salt = result.first; key = result.second; derived = null
                        data = result.third; exists = true; session++
                    }
                }
            } catch (_: Exception) { error = "Entsperren fehlgeschlagen. Passwort oder Sicherungsdatei prüfen. Vorhandene Daten wurden nicht absichtlich ersetzt." }
            finally { derived?.fill(0); password.fill('\u0000'); busy = false }
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
                withContext(Dispatchers.IO) { val raw = encode(next); try { write(Vault.encrypt(raw, k, s)) } finally { raw.fill(0) } }
                if (generation == attempt) data = next
            } catch (_: Exception) { error = "Speichern fehlgeschlagen. Änderung wurde nicht übernommen." }
            finally { k.fill(0); busy = false }
        }
    }
    fun backup(): ByteArray = file.readFully()
    override fun onCleared() { lock() }
}
