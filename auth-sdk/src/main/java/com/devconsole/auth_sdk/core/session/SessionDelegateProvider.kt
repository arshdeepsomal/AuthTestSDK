package com.devconsole.auth_sdk.core.session

import android.content.Context

internal interface SessionDelegateProvider {
    fun provide(): ((Context) -> Session)
}
