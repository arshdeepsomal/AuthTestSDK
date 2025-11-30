package com.devconsole.auth_sdk.auth.client

import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.api.TWOAuthService
import com.devconsole.auth_sdk.network.data.SubmitGoogleData
import com.devconsole.auth_sdk.network.data.SubmitGoogleReceiptDataLinkAccount
import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOGetTokenForPKData
import com.devconsole.auth_sdk.network.data.TWOGoogleReceiptLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLoginRequest
import com.devconsole.auth_sdk.network.data.TWOLogoutRequest
import com.devconsole.auth_sdk.network.data.TWOPrivateKeyData
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenRequest
import com.devconsole.auth_sdk.network.data.TWOTokenData
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoAuthClientTest {

    private val config = Configuration.TWO.Auth(
        baseUrl = "https://example.com/",
        authorization = "Bearer token",
        brand = "brand",
        source = "mobile",
        deviceId = "device"
    )

    private lateinit var service: TWOAuthService
    private lateinit var retrofit: Retrofit
    private lateinit var client: TwoAuthClient

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        service = mockk(relaxed = true)
        retrofit = mockk(relaxed = true)
        mockkObject(RetrofitManager)
        coEvery { retrofit.create(TWOAuthService::class.java) } returns service
        coEvery { RetrofitManager.getInstance(any()) } returns retrofit
        client = TwoAuthClient(config)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loginWithOneToken returns success`() = runTest {
        coEvery { service.login(any(), any<TWOLoginRequest>()) } returns Response.success(TWOTokenData(success = true))

        val result = client.loginWithOneToken("one")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `loginWithOneToken returns failure on error response`() = runTest {
        val error = Response.error<TWOTokenData>(400, "bad".toResponseBody("text/plain".toMediaType()))
        coEvery { service.login(any(), any<TWOLoginRequest>()) } returns error

        val result = client.loginWithOneToken("one")

        assertTrue(result.isFailure)
    }

    @Test
    fun `renewToken updates token data`() = runTest {
        val renewData = TWORenewTokenData(success = true, encodedJwt = "new", sessionToken = "session", sessionTokenExpiry = 5L)
        coEvery { service.renewToken(any(), any<TWORenewTokenRequest>()) } returns Response.success(renewData)

        val result = client.renewToken("token").getOrThrow()

        assertEquals("new", result.encodedJwt)
        assertEquals("session", result.sessionToken)
    }

    @Test
    fun `renewToken returns failure on error`() = runTest {
        val error = Response.error<TWORenewTokenData>(500, "error".toResponseBody("text/plain".toMediaType()))
        coEvery { service.renewToken(any(), any<TWORenewTokenRequest>()) } returns error

        val result = client.renewToken("token")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchPrivateKeyToken returns token when successful`() = runTest {
        val body = TWOGetTokenForPKData(accessToken = "token", expiresIn = 1, grantType = "", success = true, error = null)
        coEvery { service.getTokenForPrivateKey(any()) } returns Response.success(body)

        val result = client.fetchPrivateKeyToken("secret").getOrThrow()

        assertEquals("token", result)
    }

    @Test
    fun `fetchPrivateKeyToken fails when backend signals error`() = runTest {
        val body = TWOGetTokenForPKData(accessToken = null, expiresIn = null, grantType = null, success = false, error = "bad")
        coEvery { service.getTokenForPrivateKey(any()) } returns Response.success(body)

        val result = client.fetchPrivateKeyToken("secret")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchPrivateKey returns private key when successful`() = runTest {
        val body = TWOPrivateKeyData(privateKey = "pk", success = true, error = null)
        coEvery { service.getPrivateKey(any(), any()) } returns Response.success(body)

        val result = client.fetchPrivateKey("bearer").getOrThrow()

        assertEquals("pk", result)
    }

    @Test
    fun `fetchPrivateKey fails when backend signals error`() = runTest {
        val body = TWOPrivateKeyData(privateKey = null, success = false, error = "bad")
        coEvery { service.getPrivateKey(any(), any()) } returns Response.success(body)

        val result = client.fetchPrivateKey("bearer")

        assertTrue(result.isFailure)
    }

    @Test
    fun `receipt calls return parsed data`() = runTest {
        val receipt = SubmitReceiptData(success = true, sessionToken = "s", encodedJwt = "jwt")
        coEvery { service.loginWithGoogleReceipt(any(), any<TWOGoogleReceiptLoginRequest>()) } returns Response.success(receipt)

        val loginResult = client.loginWithGoogleReceipt("token").getOrThrow()

        assertEquals("s", loginResult.sessionToken)
    }

    @Test
    fun `receipt calls return failure on error`() = runTest {
        val error = Response.error<SubmitReceiptData>(500, "err".toResponseBody("text/plain".toMediaType()))
        coEvery { service.loginWithGoogleReceipt(any(), any<TWOGoogleReceiptLoginRequest>()) } returns error

        val result = client.loginWithGoogleReceipt("token")

        assertTrue(result.isFailure)
    }

    @Test
    fun `submit receipt and link account delegates to service`() = runTest {
        val receipt = SubmitReceiptData(success = true, sessionToken = "s", encodedJwt = "jwt")
        coEvery { service.submitGoogleReceiptAndLinkAccount(any(), any<SubmitGoogleReceiptDataLinkAccount>()) } returns Response.success(receipt)

        val result = client.submitGoogleReceiptAndLinkAccount("pt", "sku", "user", "pass", "pkg", "acct").getOrThrow()

        assertEquals("jwt", result.encodedJwt)
    }

    @Test
    fun `submit receipt delegates to service`() = runTest {
        val receipt = SubmitReceiptData(success = true, sessionToken = "s", encodedJwt = "jwt")
        coEvery { service.submitGoogleReceipt(any(), any<SubmitGoogleData>()) } returns Response.success(receipt)

        val result = client.submitGoogleReceipt("current", "previous", "sku", "pkg").getOrThrow()

        assertEquals("s", result.sessionToken)
    }

    @Test
    fun `logout completes successfully`() = runTest {
        coEvery { service.logout(any(), any()) } returns Response.success(Any())

        val result = client.logout(TWOLogoutRequest(idToken = "id", flatToken = "flat"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `logout failure is captured`() = runTest {
        coEvery { service.logout(any(), any()) } throws IllegalStateException("boom")

        val result = client.logout(TWOLogoutRequest(idToken = "id", flatToken = "flat"))

        assertTrue(result.isFailure)
    }
}
