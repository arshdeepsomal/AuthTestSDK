package com.devconsole.auth_sdk.core.session

import android.app.Application
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.security.JWTEncryption
import io.mockk.MockKAnnotations
import io.mockk.anyConstructed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionManagerDelegateTest {

    private lateinit var context: Application

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkConstructor(SessionPreferences::class)
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getSession returns stored value`() {
        val expected = SessionData("auth", ONETokenData(), TWOTokenData())
        coEvery { anyConstructed<SessionPreferences>().getSessionData() } returns flowOf(expected)

        val delegate = SessionManagerDelegate(context)
        val session = delegate.getSession()

        kotlin.test.assertEquals(expected, session)
    }

    @Test
    fun `hasTokenExpired returns true when authorization is empty`() {
        val emptySession = SessionData("", ONETokenData(), TWOTokenData())
        coEvery { anyConstructed<SessionPreferences>().getSessionData() } returns flowOf(emptySession)

        val delegate = SessionManagerDelegate(context)

        assertTrue(delegate.hasTokenExpired())
    }

    @Test
    fun `hasTokenExpired parses jwt expiration`() {
        val tokenData = TWOTokenData(encodedJwt = "jwt")
        val session = SessionData("code", ONETokenData(), tokenData)
        coEvery { anyConstructed<SessionPreferences>().getSessionData() } returns flowOf(session)
        mockkConstructor(JWTEncryption::class)
        val future = (System.currentTimeMillis() / 1000) + 10
        every { anyConstructed<JWTEncryption>().decodeJWT("jwt") } returns "{\"exp\":$future}"

        val delegate = SessionManagerDelegate(context)

        assertFalse(delegate.hasTokenExpired())
    }

    @Test
    fun `hasTokenExpired returns true on expired token`() {
        val tokenData = TWOTokenData(encodedJwt = "jwt")
        val session = SessionData("code", ONETokenData(), tokenData)
        coEvery { anyConstructed<SessionPreferences>().getSessionData() } returns flowOf(session)
        mockkConstructor(JWTEncryption::class)
        val past = (System.currentTimeMillis() / 1000) - 10
        every { anyConstructed<JWTEncryption>().decodeJWT("jwt") } returns "{\"exp\":$past}"

        val delegate = SessionManagerDelegate(context)

        assertTrue(delegate.hasTokenExpired())
    }
}
