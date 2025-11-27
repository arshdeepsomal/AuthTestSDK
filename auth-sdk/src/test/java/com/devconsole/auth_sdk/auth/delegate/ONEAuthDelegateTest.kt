package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import android.content.Intent
import com.devconsole.auth_sdk.auth.client.OneAuthClient
import com.devconsole.auth_sdk.auth.client.TwoAuthClient
import com.devconsole.auth_sdk.auth.model.AuthState
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.core.session.SessionData
import com.devconsole.auth_sdk.core.session.SessionManager
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.TWORenewTokenData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class ONEAuthDelegateTest {

    private val context = mockk<Context>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val sessionState = MutableStateFlow(true)
    private val oneAuthClient = mockk<OneAuthClient>(relaxed = true)
    private val twoAuthClient = mockk<TwoAuthClient>(relaxed = true)
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

    @Before
    fun setup() {
        every { sessionManager.sessionState } returns sessionState
        every { sessionManager.hasTokenExpired() } returns false
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `login publishes launch intent`() = runBlocking {
        every { oneAuthClient.buildLoginIntent() } returns Intent("login")

        val delegate = ONEAuthDelegate(context, sessionManager, oneConfig, twoConfig, oneAuthClient, twoAuthClient)
        delegate.login()

        withTimeoutState(delegate) { it is AuthState.LaunchIntent }
    }

    @Test
    fun `logout clears session and emits success`() = runBlocking {
        val sessionData = SessionData("code", ONETokenData(), TWOTokenData())
        every { sessionManager.getSession() } returns sessionData
        coEvery { twoAuthClient.logout(any()) } returns Result.success(Unit)

        val delegate = ONEAuthDelegate(context, sessionManager, oneConfig, twoConfig, oneAuthClient, twoAuthClient)
        delegate.logout()

        withTimeoutState(delegate) { state ->
            coVerify { sessionManager.clearSession() }
            state is AuthState.LogoutSuccess
        }
    }

    @Test
    fun `refreshToken saves renewed session`() = runBlocking {
        val sessionData = SessionData(
            "code",
            ONETokenData(accessToken = "one", refreshToken = "refresh"),
            TWOTokenData(encodedJwt = "jwt", sessionToken = "session", sessionTokenExpiry = 1L)
        )
        every { sessionManager.getSession() } returns sessionData
        coEvery { twoAuthClient.renewToken(any()) } returns Result.success(
            TWORenewTokenData(encodedJwt = "newJwt", sessionToken = "newSession", sessionTokenExpiry = 2L)
        )

        val delegate = ONEAuthDelegate(context, sessionManager, oneConfig, twoConfig, oneAuthClient, twoAuthClient)

        val refreshed = delegate.refreshToken()

        assertTrue(refreshed)
        coVerify { sessionManager.saveSession(match { it.TWOTokenData.encodedJwt == "newJwt" }) }
        assertTrue(delegate.state.value is AuthState.AuthSuccess)
    }

    private suspend fun withTimeoutState(delegate: ONEAuthDelegate, predicate: (AuthState) -> Boolean) {
        repeat(50) {
            val current = delegate.state.value
            if (predicate(current)) return
            delay(20)
        }
        assertEquals(true, predicate(delegate.state.value))
    }
}
