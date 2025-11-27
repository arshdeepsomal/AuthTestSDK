package com.devconsole.auth_sdk.core.session

import android.content.Context

internal object DefaultSessionDelegateProvider : SessionDelegateProvider {
    override fun provide(): (Context) -> Session = { context ->
        SessionManagerDelegate(context)
    }
}
