package com.devconsole.auth_sdk.auth.delegate

import android.content.Context
import com.devconsole.auth_sdk.auth.api.AuthApi
import com.devconsole.auth_sdk.auth.model.Configuration

internal interface AuthDelegateProvider {
    fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth) -> AuthApi)
}
