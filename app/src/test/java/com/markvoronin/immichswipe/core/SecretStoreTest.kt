package com.markvoronin.immichswipe.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretStoreTest {

    /** Reversible stand-in for the Keystore cipher. */
    private class XorCrypto(var broken: Boolean = false) : SecretCrypto {
        override fun encrypt(plain: ByteArray): ByteArray {
            if (broken) error("keystore unavailable")
            return plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
        override fun decrypt(data: ByteArray): ByteArray {
            if (broken) error("key invalidated")
            return data.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
    }

    @Test
    fun `round trip returns the original and hides the plaintext`() {
        val store = SecretStore(XorCrypto())
        val stored = store.encrypt("my-api-key")
        assertTrue(stored.startsWith(SecretStore.PREFIX))
        assertFalse(stored.contains("my-api-key"))
        assertEquals("my-api-key", store.decrypt(stored))
    }

    @Test
    fun `legacy plaintext is returned unchanged and flagged for migration`() {
        val store = SecretStore(XorCrypto())
        assertEquals("legacy-key", store.decrypt("legacy-key"))
        assertTrue(store.needsEncryption("legacy-key"))
        assertFalse(store.needsEncryption(store.encrypt("legacy-key")))
        assertFalse(store.needsEncryption(""))
    }

    @Test
    fun `undecryptable value yields null instead of crashing`() {
        val crypto = XorCrypto()
        val store = SecretStore(crypto)
        val stored = store.encrypt("secret")
        crypto.broken = true
        assertNull(store.decrypt(stored))
    }

    @Test
    fun `encryption failure keeps the credential usable`() {
        val store = SecretStore(XorCrypto(broken = true))
        val stored = store.encrypt("secret")
        assertEquals("secret", stored)
        assertNotEquals(SecretStore.PREFIX, stored.take(SecretStore.PREFIX.length))
        assertEquals("secret", store.decrypt(stored))
    }
}
