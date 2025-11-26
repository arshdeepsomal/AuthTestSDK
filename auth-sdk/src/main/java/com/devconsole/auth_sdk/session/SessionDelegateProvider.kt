package com.devconsole.auth_sdk.session

import android.content.Context

internal interface SessionDelegateProvider {
    fun provide(): ((Context) -> Session)
}
