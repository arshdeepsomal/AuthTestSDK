package com.devconsole.authtest

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devconsole.auth_sdk.AuthManager
import com.devconsole.auth_sdk.data.AuthState
import com.devconsole.auth_sdk.data.Configuration
import com.devconsole.auth_sdk.session.SessionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application): AndroidViewModel(application) {

    private val oneConfig = Configuration.ONE.Auth(
        baseUrl = "One_Base",
        clientId = "CLIENT_ID",
        clientSecret = "CLIENT_SECRET",
        redirectUri = "com.example.authtest/callback",
        nounce = UUID.randomUUID().toString(),
        salt = "SALT",
        privateKeyBaseURL = "PRivate keu base url",
        privateKeyAuthorization = "Private key authorization"
    )

    @SuppressLint("HardwareIds")
    val twoConfig = Configuration.TWO.Auth(
        baseUrl = "TwoBase",
        authorization = "AUTHORIZATION",
        brand = "BRAND",
        source = "SOURCE",
        deviceId = Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    )

    private var authManager: AuthManager = AuthManager(
        context = application.applicationContext,
        ONEConfig = oneConfig,
        TWOConfig = twoConfig,
    )

    private val _authState = MutableStateFlow<AuthState>(AuthState.UnInitialize)
    val authState = _authState.asStateFlow()

    private val _sessionActive = MutableStateFlow(false)
    val sessionActive = _sessionActive.asStateFlow()

    init {
        viewModelScope.launch {
            authManager.fetchAuthState().collect { authState ->
                _authState.value = authState
            }

            authManager.fetchSessionState().collect { isSessionActive ->
                _sessionActive.value = isSessionActive
            }
        }
    }

    fun login() {
        authManager.login()
    }

    fun logout() {
        authManager.logout()
    }

    fun register() {
        authManager.register()
    }

    fun authIntent(result: ActivityResult) {
        authManager.handleONEResponse(result = result)
    }

    fun refreshSession() {
        authManager.refreshSession()
    }

    fun getCurrentSession(): SessionData? {
        return authManager.getCurrentSession()
    }
}