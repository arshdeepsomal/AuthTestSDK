package com.devconsole.auth_sdk.delegate

import android.content.Context
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.session.SessionManager

internal object DefaultDelegateProvider : AuthDelegateProvider {
    override fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth) -> AuthApi) =
        { context, oneConfig, twoConfig ->
            ONEAuthDelegate(context, SessionManager(context), oneConfig, twoConfig)
        }
}
