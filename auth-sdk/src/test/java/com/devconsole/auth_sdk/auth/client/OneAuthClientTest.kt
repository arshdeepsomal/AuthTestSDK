package com.devconsole.auth_sdk.auth.client

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.devconsole.auth_sdk.auth.delegate.AuthServiceProvider
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.network.api.ONEAuthService
import com.devconsole.auth_sdk.network.api.RetrofitManager
import com.devconsole.auth_sdk.network.data.ONETokenData
import io.mockk.anyConstructed
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.TokenRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit

class OneAuthClientTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val config = Configuration.ONE.Auth(
        baseUrl = "https://example.com/",
        clientId = "client-id",
        clientSecret = "client-secret",
        redirectUri = "app://redirect",
        nounce = "nonce",
        salt = "salt"
    )

    @Before
    fun setup() {
        mockkStatic(AuthorizationException::class)
        mockkStatic(AuthorizationResponse::class)
        mockkObject(RetrofitManager)
        mockkConstructor(com.devconsole.auth_sdk.network.security.JWTEncryption::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `buildLoginIntent returns intent from authorization service`() {
        val authService = mockk<AuthorizationService>(relaxed = true) {
            every { createCustomTabsIntentBuilder() } returns mockk(relaxed = true)
            every { getAuthorizationRequestIntent(any<AuthorizationRequest>(), any()) } returns Intent("login")
        }
        val provider = mockk<AuthServiceProvider> {
            every { provide(any()) } returns authService
        }

        val client = OneAuthClient(context, config, provider)

        client.buildLoginIntent()

        io.mockk.verify { authService.getAuthorizationRequestIntent(any(), any()) }
    }

    @Test
    fun `exchangeToken returns success when response and token are valid`() = kotlinx.coroutines.runBlocking {
        val intent = Intent()
        val tokenData = ONETokenData(accessToken = "access", refreshToken = "refresh")

        val service = mockk<ONEAuthService> {
            every { getToken(any()) } returns Response.success(tokenData)
        }
        val retrofit = mockk<Retrofit> {
            every { create(ONEAuthService::class.java) } returns service
        }
        every { RetrofitManager.getInstance(any()) } returns retrofit

        every { AuthorizationException.fromIntent(intent) } returns null

        val tokenRequest = mockk<TokenRequest> {
            every { codeVerifier } returns "code-verifier"
        }
        val response = mockk<AuthorizationResponse> {
            every { createTokenExchangeRequest() } returns tokenRequest
            every { authorizationCode } returns "auth-code"
        }
        every { AuthorizationResponse.fromIntent(intent) } returns response

        val client = OneAuthClient(context, config)

        val result = client.exchangeToken(intent)

        assert(result.isSuccess)
        assert(result.getOrNull() == tokenData)
    }

    @Test
    fun `buildRegisterIntent signs jwt and returns intent`() = kotlinx.coroutines.runBlocking {
        val authService = mockk<AuthorizationService>(relaxed = true) {
            every { createCustomTabsIntentBuilder() } returns mockk(relaxed = true)
            every { getAuthorizationRequestIntent(any<AuthorizationRequest>(), any()) } returns Intent("register")
        }
        val provider = mockk<AuthServiceProvider> {
            every { provide(any()) } returns authService
        }

        every { anyConstructed<com.devconsole.auth_sdk.network.security.JWTEncryption>().createJWT(any(), any(), any(), any(), any(), any()) } returns "signed-jwt"

        val client = OneAuthClient(context, config, provider)

        val intent = client.buildRegisterIntent(privateKey = "private-key")

        assert(intent.action == "register")
    }
}
