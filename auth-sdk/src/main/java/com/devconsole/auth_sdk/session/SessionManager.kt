package com.devconsole.auth_sdk.session

import android.content.Context

internal class SessionManager(val context: Context) {

    private var session: Session = DefaultSessionDelegateProvider.provide()(context)

    fun getSession(): SessionData? {
        return session.getSession()
    }

    fun hasTokenExpired(): Boolean {
        return session.hasTokenExpired()
    }

    internal fun saveSession(sessionData: SessionData) {
        session.saveSession(sessionData)
    }

    internal fun clearSession() {
        session.clearSession()
    }
}
