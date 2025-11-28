package com.devconsole.auth_sdk.core.session

internal interface SessionStore {
    fun getSession(): SessionData?
    fun saveSession(sessionData: SessionData)
    fun clearSession()
    fun hasTokenExpired(): Boolean
}
