package com.devconsole.auth_sdk.core.session

import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.ONETokenData

data class SessionData(
    val authorizationCode: String,
    val ONETokenData: ONETokenData,
    val TWOTokenData: TWOTokenData
)
