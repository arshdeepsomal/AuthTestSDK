package com.devconsole.auth_sdk.session

internal interface Session {
    fun getSession(): SessionData?
    fun saveSession(sessionData: SessionData)
    fun clearSession()
    fun hasTokenExpired(): Boolean
}
