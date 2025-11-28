package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope

internal interface AuthDelegateProvider {
    fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth, SessionManager, CoroutineScope) -> AuthApi)
}
