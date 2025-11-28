package com.devconsole.auth_sdk.core.session

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class SessionManager(
    private val session: SessionStore,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _sessionState = MutableStateFlow(!session.hasTokenExpired())
    val sessionState: StateFlow<Boolean> = _sessionState

    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents

    constructor(
        context: Context,
        sessionDelegateProvider: SessionDelegateProvider = DefaultSessionDelegateProvider,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ) : this(sessionDelegateProvider.provide()(context), coroutineScope)

    fun getSession(): SessionData? {
        return session.getSession()
    }

    fun hasTokenExpired(): Boolean {
        val expired = session.hasTokenExpired()
        if (expired) {
            coroutineScope.launch { _sessionEvents.emit(SessionEvent.Expired) }
            setSessionState(false)
        }
        return expired
    }

    internal fun saveSession(sessionData: SessionData) {
        session.saveSession(sessionData)
        setSessionState(true)
        coroutineScope.launch { _sessionEvents.emit(SessionEvent.Saved(sessionData)) }
    }

    internal fun clearSession() {
        session.clearSession()
        setSessionState(false)
        coroutineScope.launch { _sessionEvents.emit(SessionEvent.Cleared) }
    }

    fun setSessionState(isActive: Boolean) {
        _sessionState.value = isActive
    }
}
