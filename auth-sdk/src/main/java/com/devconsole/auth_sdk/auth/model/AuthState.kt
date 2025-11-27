package com.devconsole.auth_sdk.auth.model

import android.content.Intent
import com.devconsole.auth_sdk.core.session.SessionData

sealed class AuthState {
    // Default AuthState.
    object UnInitialize : AuthState()

    // This state would be active if anything related to network is going on.
    object Loading : AuthState()

    // This state would provide the user with Exception if any.
    data class Error(val error: Throwable) : AuthState()

    // This will be called in both login and register.
    data class AuthSuccess(val sessionData: SessionData) : AuthState()

    // This state is active when logout is success.
    object LogoutSuccess : AuthState()

    // This state is active when the intent launch is requested for login & register.
    data class LaunchIntent(val intent: Intent) : AuthState()
}
