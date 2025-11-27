package com.devconsole.auth_sdk.network.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun `encrypt and decrypt round trip restores data`() {
        val cryptoManager = CryptoManager()
        val plain = "hello-world".toByteArray()

        val encrypted = cryptoManager.encrypt(plain)
        val decrypted = cryptoManager.decrypt(encrypted)

        assertNotEquals("Encryption should change the payload", plain.toList(), encrypted.toList())
        assertArrayEquals(plain, decrypted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decrypt throws when payload is smaller than IV`() {
        val cryptoManager = CryptoManager()

        cryptoManager.decrypt(ByteArray(4))
    }

    @Test
    fun `uses fallback key when keystore is unavailable`() {
        val cryptoManager = CryptoManager(keystoreProvider = "MissingProvider")
        val plain = "fallback-key".toByteArray()

        val decrypted = cryptoManager.decrypt(cryptoManager.encrypt(plain))

        assertArrayEquals(plain, decrypted)
    }
}
