package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.auth.delegate.ONEAuthDelegate
import com.devconsole.auth_sdk.core.session.SessionManager
import kotlinx.coroutines.CoroutineScope

internal object DefaultDelegateProvider : AuthDelegateProvider {
    override fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth, SessionManager, CoroutineScope) -> AuthApi) =
        { context, oneConfig, twoConfig, sessionManager, coroutineScope ->
            ONEAuthDelegate(
                context = context,
                sessionManager = sessionManager,
                oneConfig = oneConfig,
                twoConfig = twoConfig,
                coroutineScope = coroutineScope
            )
        }
}
