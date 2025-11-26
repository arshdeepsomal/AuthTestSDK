package com.devconsole.auth_sdk.network.data

import com.google.gson.annotations.SerializedName

data class Claims(
    val request: ClaimsRequest,
    @SerializedName("userinfo")
    val userInfo: ClaimsUserInfo,
    @SerializedName("account")
    val account: String = "sdsfsdf"
)

data class ClaimsRequest(
    var email: String? = null,
    var username: String? = null,
    var autoreg_token: Boolean = false,
    var nonce: String = ""
)

data class ClaimsUserInfo(
    var email: UserInfoEmail = UserInfoEmail()
)

data class ClaimsCustom(
    var account: String? = null,
    var dob: String? = null
)

data class UserInfoEmail(
    var essential: Boolean = true
)
