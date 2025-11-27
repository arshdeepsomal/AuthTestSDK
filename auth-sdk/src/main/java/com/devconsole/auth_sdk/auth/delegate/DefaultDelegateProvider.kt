package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.model.Configuration
import com.devconsole.auth_sdk.auth.delegate.ONEAuthDelegate
import com.devconsole.auth_sdk.core.session.SessionManager

internal object DefaultDelegateProvider : AuthDelegateProvider {
    override fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth) -> AuthApi) =
        { context, oneConfig, twoConfig ->
            ONEAuthDelegate(context, SessionManager(context), oneConfig, twoConfig)
        }
}
