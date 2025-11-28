package com.devconsole.auth_sdk.core.session

import com.devconsole.auth_sdk.core.session.SessionData

internal sealed class SessionEvent {
    data class Saved(val session: SessionData) : SessionEvent()
    object Cleared : SessionEvent()
    object Expired : SessionEvent()
}
