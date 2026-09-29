package de.werklog.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BiometricLock(private val activity: FragmentActivity) {
    private val alias = "werklog-biometric-v1"
    private val prefs = activity.getSharedPreferences("biometric", Context.MODE_PRIVATE)
    fun enabled() = prefs.contains("wrapped")
    fun disable() { prefs.edit().clear().apply(); KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) } }
    private fun cipher(encrypt: Boolean): Cipher {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (encrypt) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true).build()); generator.generateKey()
        }
        val key = store.getKey(alias, null) as SecretKey
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            if (encrypt) init(Cipher.ENCRYPT_MODE, key) else init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, Base64.getDecoder().decode(prefs.getString("iv", ""))))
        }
    }
    fun authenticate(enableKey: ByteArray? = null, result: (ByteArray?) -> Unit, error: (String) -> Unit) {
        if (BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) { enableKey?.fill(0); error("Starke Biometrie nicht verfügbar. Bitte das Tresorpasswort verwenden."); return }
        try {
            val cipher = cipher(enableKey != null)
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(auth: BiometricPrompt.AuthenticationResult) {
                    try {
                        val c = auth.cryptoObject?.cipher ?: error("Keine Schlüssel-Freigabe")
                        if (enableKey != null) {
                            val wrapped = c.doFinal(enableKey)
                            prefs.edit().putString("wrapped", Base64.getEncoder().encodeToString(wrapped)).putString("iv", Base64.getEncoder().encodeToString(c.iv)).apply(); result(null)
                        } else result(c.doFinal(Base64.getDecoder().decode(prefs.getString("wrapped", ""))))
                    } catch (_: Exception) { error("Biometrische Freigabe fehlgeschlagen. Das Tresorpasswort bleibt nutzbar.") }
                    finally { enableKey?.fill(0) }
                }
                override fun onAuthenticationError(code: Int, message: CharSequence) { enableKey?.fill(0); error(message.toString()) }
            })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(if (enableKey == null) "WerkLog entsperren" else "Biometrie aktivieren")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("Passwort verwenden").build(), BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) { enableKey?.fill(0); error("Biometrischer Schlüssel nicht verfügbar. Mit Passwort öffnen und Biometrie neu aktivieren.") }
    }
}
