package com.devconsole.auth_sdk.auth.api

import androidx.activity.result.ActivityResult
import com.devconsole.auth_sdk.auth.model.AuthState
import kotlinx.coroutines.flow.StateFlow

internal interface AuthApi {
    fun login()
    fun register()
    fun handleIntentResult(result: ActivityResult)
    fun logout()
    suspend fun refreshToken(): Boolean
    fun loginWithGoogleReceipt(purchaseToken: String)
    fun submitGoogleReceiptAndLinkAccount(
        purchaseToken: String,
        sku: String,
        username: String? = "",
        password: String? = "",
        packageName: String? = "",
        accountToken: String? = ""
    )
    fun submitGoogleReceipt(
        currentPurchaseToken: String?,
        previousPurchaseToken: String?,
        sku: String,
        packageName: String? = ""
    )

    val state: StateFlow<AuthState>
    val sessionState: StateFlow<Boolean>
}
