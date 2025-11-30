package com.devconsole.auth_sdk.auth.client

import android.content.Context
import net.openid.appauth.AuthorizationService

internal interface AuthServiceProvider {
    fun provide(context: Context): AuthorizationService
}
