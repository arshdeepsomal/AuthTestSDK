package com.devconsole.auth_sdk.core.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class SessionManager(
    val context: Context,
    sessionDelegateProvider: SessionDelegateProvider = DefaultSessionDelegateProvider
) {

    private var session: Session = sessionDelegateProvider.provide()(context)

    private val _sessionState = MutableStateFlow(!session.hasTokenExpired())
    val sessionState: StateFlow<Boolean> = _sessionState

    fun getSession(): SessionData? {
        return session.getSession()
    }

    fun hasTokenExpired(): Boolean {
        return session.hasTokenExpired()
    }

    internal fun saveSession(sessionData: SessionData) {
        session.saveSession(sessionData)
        setSessionState(true)
    }

    internal fun clearSession() {
        session.clearSession()
        setSessionState(false)
    }

    fun setSessionState(isActive: Boolean) {
        _sessionState.value = isActive
    }
}
