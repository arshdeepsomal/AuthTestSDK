package com.devconsole.auth_sdk.auth.model

data class ONEAuthException(
    val errorCode: Int,
    val errorDescription: String?
) : Exception()
