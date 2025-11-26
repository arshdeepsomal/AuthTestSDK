package com.devconsole.auth_sdk.delegate

import android.content.Context
import net.openid.appauth.AuthorizationService

internal interface AuthServiceProvider {
    fun provide(context: Context): AuthorizationService
}
