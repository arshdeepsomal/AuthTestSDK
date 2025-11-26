package com.devconsole.auth_sdk.auth

import com.devconsole.auth_sdk.session.SessionData
import com.devconsole.auth_sdk.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SessionController(
    private val sessionManager: SessionManager,
) {

    private val _sessionState = MutableStateFlow(!sessionManager.hasTokenExpired())
    val sessionState: StateFlow<Boolean> = _sessionState

    fun currentSession(): SessionData? = sessionManager.getSession()

    fun save(sessionData: SessionData) {
        sessionManager.saveSession(sessionData)
        setSessionState(true)
    }

    fun clear() {
        sessionManager.clearSession()
        setSessionState(false)
    }

    fun setSessionState(isActive: Boolean) {
        _sessionState.value = isActive
    }
}
