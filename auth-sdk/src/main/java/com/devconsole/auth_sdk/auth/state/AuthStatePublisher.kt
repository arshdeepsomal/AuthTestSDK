package com.devconsole.auth_sdk.auth.state

import android.content.Intent
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.session.SessionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AuthStatePublisher {

    private val _state = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun loading() {
        _state.value = AuthState.Loading
    }

    fun launch(intent: Intent) {
        _state.value = AuthState.LaunchIntent(intent)
    }

    fun success(sessionData: SessionData) {
        _state.value = AuthState.AuthSuccess(sessionData)
    }

    fun error(error: Throwable) {
        _state.value = AuthState.Error(error)
    }

    fun logout() {
        _state.value = AuthState.LogoutSuccess
    }
}




