package com.devconsole.auth_sdk.core.session

import android.app.Application
import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    @MockK(relaxed = true)
    lateinit var session: SessionStore

    private lateinit var manager: SessionManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(DefaultSessionDelegateProvider)
        every { DefaultSessionDelegateProvider.provide() } returns { session }
        every { session.hasTokenExpired() } returns false
        val context: Application = RuntimeEnvironment.getApplication()
        manager = SessionManager(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `saveSession delegates and updates state`() {
        val sessionData = SessionData("code", ONETokenData(), TWOTokenData())
        every { session.saveSession(any()) } just runs

        manager.saveSession(sessionData)

        verify { session.saveSession(sessionData) }
        assert(manager.sessionState.value)
    }

    @Test
    fun `clearSession delegates and resets state`() {
        every { session.clearSession() } just runs

        manager.clearSession()

        verify { session.clearSession() }
        assert(!manager.sessionState.value)
    }

    @Test
    fun `getSession proxies to delegate`() {
        val expected = SessionData("auth", ONETokenData(), TWOTokenData())
        every { session.getSession() } returns expected

        val result = manager.getSession()

        assert(result == expected)
    }
}
