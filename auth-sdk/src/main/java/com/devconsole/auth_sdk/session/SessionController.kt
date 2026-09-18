package com.devconsole.auth_sdk.session

import com.devconsole.auth_sdk.core.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class SessionController(
    private val sessionManager: SessionManager,
    private val dispatcherProvider: DispatcherProvider,
    private val scope: CoroutineScope
) {

    private val _sessionState = MutableStateFlow(false)
    val sessionState: StateFlow<Boolean> = _sessionState.asStateFlow()

    private val _sessionData = MutableStateFlow<SessionData?>(null)

    init {
        scope.launch(dispatcherProvider.io) {
            sessionManager.sessionStream()
                .catch {
                    _sessionData.value = null
                    _sessionState.value = false
                }
                .collect { session ->
                    _sessionData.value = session
                    _sessionState.value = session?.let { !SessionExpirationEvaluator.isExpired(it) } ?: false
                }
        }
    }

    fun currentSession(): SessionData? = _sessionData.value

    fun onSessionEstablished(sessionData: SessionData) {
        _sessionData.value = sessionData
        _sessionState.value = !SessionExpirationEvaluator.isExpired(sessionData)
        sessionManager.saveSession(sessionData)
    }

    fun clearSession() {
        _sessionData.value = null
        _sessionState.value = false
        sessionManager.clearSession()
    }

    fun markSessionInactive() {
        _sessionState.value = false
    }
}
