package com.devconsole.auth_sdk.network.security

import com.devconsole.auth_sdk.testutil.SecurityProviderRule
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class CryptoManagerTest {

    @get:Rule
    val securityProviderRule = SecurityProviderRule()

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

}
