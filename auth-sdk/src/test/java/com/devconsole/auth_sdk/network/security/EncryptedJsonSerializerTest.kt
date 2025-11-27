package com.devconsole.auth_sdk.network.security

import com.devconsole.auth_sdk.testutil.SecurityProviderRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EncryptedJsonSerializerTest {

    @get:Rule
    val securityProviderRule = SecurityProviderRule()

    @MockK
    lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `writeTo encrypts json payload before persisting`() = runBlocking {
        val serializer = encryptedGsonSerializer<String?>(
            defaultValue = null,
            cryptoManager = cryptoManager,
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined
        )
        val captured = slot<ByteArray>()
        every { cryptoManager.encrypt(capture(captured)) } returns "cipher".toByteArray()

        val output = ByteArrayOutputStream()
        serializer.writeTo("value", output)

        assertEquals("cipher", output.toByteArray().decodeToString())
        assertEquals("\"value\"", captured.captured.decodeToString())
    }

    @Test
    fun `readFrom decrypts payload and returns parsed type`() = runBlocking {
        val serializer = encryptedGsonSerializer<Map<String, String>?>(
            defaultValue = null,
            cryptoManager = cryptoManager,
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined
        )
        val json = "{""key"":""value""}"
        every { cryptoManager.decrypt(any()) } returns json.toByteArray()

        val result = serializer.readFrom(ByteArrayInputStream("encrypted".toByteArray()))

        assertEquals("value", result?.get("key"))
    }

    @Test
    fun `readFrom returns default when stream is empty`() = runBlocking {
        val serializer = encryptedGsonSerializer<String?>(
            defaultValue = null,
            cryptoManager = cryptoManager,
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = serializer.readFrom(ByteArrayInputStream(ByteArray(0)))

        assertNull(result)
        verify(exactly = 0) { cryptoManager.decrypt(any()) }
    }

    @Test
    fun `readFrom returns null when decrypted json is null`() = runBlocking {
        val serializer = encryptedGsonSerializer<String?>(
            defaultValue = null,
            cryptoManager = cryptoManager,
            gson = Gson(),
            ioDispatcher = Dispatchers.Unconfined
        )
        every { cryptoManager.decrypt(any()) } returns "null".toByteArray()

        val result = serializer.readFrom(ByteArrayInputStream("ignored".toByteArray()))

        assertTrue(result == null)
    }
}
