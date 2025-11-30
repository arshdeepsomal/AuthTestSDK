package com.devconsole.auth_sdk.session

import android.content.Context
import com.devconsole.auth_sdk.session.DefaultSessionDelegateProvider
import com.devconsole.auth_sdk.session.SessionDelegateProvider

internal class SessionManager(
    private val context: Context,
    sessionDelegateProvider: SessionDelegateProvider = DefaultSessionDelegateProvider,
) {

    private var session: Session = sessionDelegateProvider.provide()(context)

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
