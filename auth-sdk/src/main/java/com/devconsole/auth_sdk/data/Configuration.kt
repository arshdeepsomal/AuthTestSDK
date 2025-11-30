package com.devconsole.auth_sdk.data

/**
 * This sealed class is for the configuration of one and two auth.
 * Sending all the parameters */
sealed class Configuration {

    sealed class ONE : Configuration() {
        data class Auth(
            val baseUrl: String,
            val clientId: String,
            val clientSecret: String,
            val redirectUri: String,
            val nounce: String,
            val salt: String = "",
            val privateKeyBaseURL: String,
            val privateKeyAuthorization: String
        ) : ONE()
    }

    sealed class TWO : Configuration() {
        data class Auth(
            val baseUrl: String,
            val authorization: String,
            val brand: String,
            val source: String,
            val deviceId: String
        ) : TWO()
    }
}
