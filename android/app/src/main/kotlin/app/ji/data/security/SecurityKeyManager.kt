package app.ji.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher encryption key using Android KeyStore (Rule 6).
 *
 * A 256-bit random passphrase is generated once, encrypted using an AES-256-GCM
 * key held in hardware-backed Android KeyStore, and stored in private SharedPreferences.
 * On app startup, the passphrase is decrypted in memory and supplied to SQLCipher.
 */
object SecurityKeyManager {

    private const val KEY_ALIAS = "ji_sqlcipher_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "ji_secure_vault"
    private const val PREF_ENCRYPTED_PASSPHRASE = "enc_passphrase"
    private const val PREF_IV = "iv"

    private const val GCM_TAG_LENGTH = 128
    private const val PASSPHRASE_BYTE_COUNT = 32 // 256 bits

    @Synchronized
    fun getDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encPassphraseBase64 = prefs.getString(PREF_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(PREF_IV, null)

        val secretKey = getOrCreateKey()

        if (encPassphraseBase64 != null && ivBase64 != null) {
            // Decrypt existing passphrase
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val encBytes = Base64.decode(encPassphraseBase64, Base64.NO_WRAP)
            return cipher.doFinal(encBytes)
        } else {
            // Generate a fresh random 256-bit passphrase
            val random = SecureRandom()
            val newPassphrase = ByteArray(PASSPHRASE_BYTE_COUNT)
            random.nextBytes(newPassphrase)

            // Encrypt it with KeyStore master key
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(newPassphrase)

            // Save encrypted passphrase + IV in private SharedPreferences
            prefs.edit()
                .putString(PREF_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            return newPassphrase
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Wipes the encryption keys and encrypted credentials (Rule 7).
     */
    @Synchronized
    fun wipeKeysAndCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Throwable) {
            // Ignored on best-effort wipe
        }
    }
}

