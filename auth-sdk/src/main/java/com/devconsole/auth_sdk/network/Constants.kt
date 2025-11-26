package com.devconsole.auth_sdk.network

import com.devconsole.auth_sdk.utils.ExcludeFromCoverage

@ExcludeFromCoverage("Constants")
internal object Constants {
    internal const val PATH_TOKEN = "login/direct/one/token"
    internal const val PATH_AUTHORIZE = "login/direct/one/authorize"
    internal const val LOGIN_SCOPES = "openid offline_access"
    internal const val QUERY = "query"
    internal const val GRANT_TYPE = "authorization_code"
    internal const val REGISTER_SCOPES = "openid login offline_access"
    internal const val CODE = "code"
    internal const val REGISTER_STATE = "register"
    internal const val PROMPT = "create"
    internal const val MAX_AGE = 86400
}
