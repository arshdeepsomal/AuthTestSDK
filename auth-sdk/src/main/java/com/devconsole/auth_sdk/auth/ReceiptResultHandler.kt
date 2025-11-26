package com.devconsole.auth_sdk.auth

import com.devconsole.auth_sdk.network.data.SubmitReceiptData
import com.devconsole.auth_sdk.network.data.TWOTokenData
import com.devconsole.auth_sdk.network.data.ONETokenData

internal class ReceiptResultHandler(
    private val onSessionSaved: (TWOTokenData, ONETokenData) -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    suspend fun handle(result: Result<SubmitReceiptData>) {
        result
            .mapCatching { receipt ->
                if (receipt.success == true) receipt.toTwoTokenData() else throw Exception(receipt.message)
            }
            .onSuccess { onSessionSaved(it, ONETokenData()) }
            .onFailure { onError(it) }
    }

    private fun SubmitReceiptData.toTwoTokenData(): TWOTokenData {
        return TWOTokenData(
            success = success,
            status = status,
            sessionToken = sessionToken,
            sessionTokenExpiry = sessionTokenExpiry,
            supportToken = supportToken,
            encodedJwt = encodedJwt,
            username = username,
            processingTime = processingTime,
        )
    }
}
