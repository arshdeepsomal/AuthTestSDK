package com.devconsole.auth_sdk.delegate

import android.content.Context
import com.devconsole.auth_sdk.AuthApi
import com.devconsole.auth_sdk.data.Configuration

internal interface AuthDelegateProvider {
    fun provide(): ((Context, Configuration.ONE.Auth, Configuration.TWO.Auth) -> AuthApi)
}
