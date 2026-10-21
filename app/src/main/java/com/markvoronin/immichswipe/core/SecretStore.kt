package com.markvoronin.immichswipe.core

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Low-level symmetric primitive, injectable so the storage format can be tested without Android. */
interface SecretCrypto {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}

/**
 * Protects secrets (the Immich API key) at rest. Values are stored as `enc1:<base64>`.
 * Values without that prefix are legacy plaintext and are returned unchanged so existing
 * installs keep working until they are re-encrypted by a migration pass.
 */
class SecretStore(private val crypto: SecretCrypto) {

    fun encrypt(plain: String): String {
        if (plain.isEmpty()) return plain
        return try {
            PREFIX + java.util.Base64.getEncoder().encodeToString(crypto.encrypt(plain.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            // Never lose the credential because the Keystore is unavailable on a broken device.
            AppLogger.e("SecretStore", "Encryption failed, storing value unprotected: ${e.message}")
            plain
        }
    }

    /** Returns the plaintext, or null when an encrypted value can no longer be decrypted. */
    fun decrypt(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            String(crypto.decrypt(java.util.Base64.getDecoder().decode(stored.removePrefix(PREFIX))), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.e("SecretStore", "Could not decrypt stored secret: ${e.message}")
            null
        }
    }

    fun needsEncryption(stored: String): Boolean = stored.isNotEmpty() && !stored.startsWith(PREFIX)

    companion object {
        const val PREFIX = "enc1:"

        /** Shared instance backed by the Android Keystore. */
        val default: SecretStore by lazy { SecretStore(KeystoreCrypto()) }
    }
}

/** AES-256-GCM with a non-exportable key held in the Android Keystore. */
class KeystoreCrypto : SecretCrypto {

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        // Layout: [iv length][iv][ciphertext+tag]
        return byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        val ivLength = data[0].toInt()
        val iv = data.copyOfRange(1, 1 + ivLength)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return cipher.doFinal(data, 1 + ivLength, data.size - 1 - ivLength)
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "immich_swipe_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
