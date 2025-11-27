package com.devconsole.auth_sdk.auth

import android.content.Context
import androidx.activity.result.ActivityResult
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.delegate.DefaultDelegateProvider
import com.devconsole.auth_sdk.auth.model.AuthState
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.core.session.SessionData
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthManagerTest {

    @MockK(relaxed = true)
    lateinit var context: Context

    private val oneConfig = Configuration.ONE.Auth(
        baseUrl = "https://one.example/",
        clientId = "client",
        clientSecret = "secret",
        redirectUri = "app://redirect",
        nounce = "nonce",
        salt = "salt"
    )
    private val twoConfig = Configuration.TWO.Auth(
        baseUrl = "https://two.example/",
        authorization = "auth",
        brand = "brand",
        source = "android",
        deviceId = "device"
    )

    private val authState = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    private val sessionState = MutableStateFlow(false)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `manager delegates auth actions to api`() {
        val api = mockk<AuthApi>(relaxed = true) {
            every { state } returns authState
            every { sessionState } returns sessionState
        }
        mockkObject(DefaultDelegateProvider)
        every { DefaultDelegateProvider.provide() } returns { _, _, _ -> api }
        val manager = AuthManager(context, oneConfig, twoConfig, api, mockk(relaxed = true))
        val result = mockk<ActivityResult>()

        manager.login()
        manager.register()
        manager.logout()
        manager.handleONEResponse(result)
        manager.loginWithGoogleReceipt("token")
        manager.submitGoogleReceiptAndLinkAccount("p", "sku", "u", "p", "pkg", "acct")
        manager.submitGoogleReceipt("curr", "prev", "sku", "pkg")
        manager.refreshSession()

        verify { api.login() }
        verify { api.register() }
        verify { api.logout() }
        verify { api.handleIntentResult(result) }
        verify { api.loginWithGoogleReceipt("token") }
        verify { api.submitGoogleReceiptAndLinkAccount("p", "sku", "u", "p", "pkg", "acct") }
        verify { api.submitGoogleReceipt("curr", "prev", "sku", "pkg") }
        verify { api.refreshToken() }
    }

    @Test
    fun `manager exposes auth and session state`() {
        val api = mockk<AuthApi>(relaxed = true) {
            every { state } returns authState
            every { sessionState } returns sessionState
        }
        val sessionData = SessionData("code", ONETokenData(), TWOTokenData())
        mockkObject(DefaultDelegateProvider)
        every { DefaultDelegateProvider.provide() } returns { _, _, _ -> api }
        val sessionManager = mockk<com.devconsole.auth_sdk.core.session.SessionManager>()
        every { sessionManager.getSession() } returns sessionData

        val manager = AuthManager(context, oneConfig, twoConfig, api, sessionManager)

        assert(manager.fetchAuthState() === authState)
        assert(manager.fetchSessionState() === sessionState)
        assert(manager.getCurrentSession() == sessionData)
    }
}
