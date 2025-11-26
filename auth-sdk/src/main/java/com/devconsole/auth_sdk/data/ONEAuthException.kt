package com.devconsole.auth_sdk.data

data class ONEAuthException(
    val errorCode: Int,
    val errorDescription: String?
) : Exception()
