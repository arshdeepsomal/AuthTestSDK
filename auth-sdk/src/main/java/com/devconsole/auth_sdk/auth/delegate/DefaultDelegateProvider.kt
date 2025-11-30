package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.data.Configuration

internal object DefaultDelegateProvider : AuthDelegateProvider {
    override fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth) -> AuthApi) =
        { context, oneConfig, twoConfig ->
            ONEAuthDelegate(context, oneConfig, twoConfig)
        }
}
