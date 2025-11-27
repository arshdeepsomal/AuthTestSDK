package com.devconsole.auth_sdk.core.session

import com.devconsole.auth_sdk.network.data.ONETokenData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.testutil.SecurityProviderRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionPreferencesTest {

    @get:Rule
    val securityProviderRule = SecurityProviderRule()

    private lateinit var preferences: SessionPreferences

    @Before
    fun setUp() = runTest {
        preferences = SessionPreferences(RuntimeEnvironment.getApplication())
        preferences.clearSession()
    }

    @After
    fun tearDown() = runTest {
        preferences.clearSession()
    }

    @Test
    fun `updateSessionData persists session`() = runTest {
        val sessionData = SessionData("code", ONETokenData(), TWOTokenData())

        preferences.updateSessionData(sessionData)
        val stored = preferences.getSessionData().first()

        assertEquals(sessionData, stored)
    }

    @Test
    fun `clearSession removes stored data`() = runTest {
        val sessionData = SessionData("code", ONETokenData(), TWOTokenData())
        preferences.updateSessionData(sessionData)

        preferences.clearSession()
        val stored = preferences.getSessionData().first()

        assertNull(stored)
    }
}
