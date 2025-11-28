package com.devconsole.auth_sdk.core.session

import android.content.Context

internal object DefaultSessionDelegateProvider : SessionDelegateProvider {
    override fun provide(): (Context) -> SessionStore = { context ->
        SessionManagerDelegate(context)
    }
}
