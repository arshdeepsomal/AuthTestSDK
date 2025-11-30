package com.devconsole.auth_sdk.auth.handler

import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptResultHandlerTest {

    @Test
    fun `handle success saves session data`() = runTest {
        var savedTwo: TWOTokenData? = null
        var savedOne: ONETokenData? = null
        var error: Throwable? = null

        val handler = ReceiptResultHandler(
            onSessionSaved = { two, one ->
                savedTwo = two
                savedOne = one
            },
            onError = { error = it }
        )

        val receipt = SubmitReceiptData(
            success = true,
            status = "ok",
            sessionToken = "session",
            sessionTokenExpiry = 10L,
            encodedJwt = "jwt",
            username = "user"
        )

        handler.handle(Result.success(receipt))

        assertTrue(error == null)
        assertEquals("session", savedTwo?.sessionToken)
        assertEquals("jwt", savedTwo?.encodedJwt)
        assertSame(ONETokenData::class, savedOne!!::class)
    }

    @Test
    fun `handle failure forwards error`() = runTest {
        var capturedError: Throwable? = null
        val handler = ReceiptResultHandler(
            onSessionSaved = { _, _ -> },
            onError = { capturedError = it }
        )

        val receipt = SubmitReceiptData(success = false, message = "failed")

        handler.handle(Result.success(receipt))

        assertEquals("failed", capturedError?.message)
    }
}
