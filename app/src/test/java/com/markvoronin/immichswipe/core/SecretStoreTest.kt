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

    /** Fails the first N encrypt calls, then works. Records recovery attempts. */
    private class FlakyCrypto(private var failuresLeft: Int) : SecretCrypto {
        var recoveries = 0
        override fun encrypt(plain: ByteArray): ByteArray {
            if (failuresLeft > 0) { failuresLeft--; error("transient") }
            return plain.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
        override fun decrypt(data: ByteArray): ByteArray =
            data.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        override fun recoverFrom(error: Exception) { recoveries++ }
    }

    @Test
    fun `a single encrypt failure is recovered and retried`() {
        val crypto = FlakyCrypto(failuresLeft = 1)
        val store = SecretStore(crypto)
        val stored = store.encrypt("secret")
        assertTrue(stored.startsWith(SecretStore.PREFIX))
        assertEquals(1, crypto.recoveries)
        assertEquals("secret", store.decrypt(stored))
    }

    @Test
    fun `two failures fall back to plaintext after exactly one recovery`() {
        val crypto = FlakyCrypto(failuresLeft = 2)
        val stored = SecretStore(crypto).encrypt("secret")
        assertEquals("secret", stored)
        assertEquals(1, crypto.recoveries)
    }
}
