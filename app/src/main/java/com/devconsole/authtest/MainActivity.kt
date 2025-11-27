package com.devconsole.authtest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devconsole.auth_sdk.auth.model.AuthState
import com.devconsole.authtest.ui.theme.AuthTestTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.refreshSession()

        setContent {
            AuthTestTheme {
                val authState = viewModel.authState.collectAsStateWithLifecycle()
                val sessionState = viewModel.sessionActive.collectAsStateWithLifecycle()

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        when {
                            authState.value is AuthState.Loading -> { Loading() }
                            (authState.value is AuthState.UnInitialize ||
                                    authState.value is AuthState.Error ||
                                    authState.value is AuthState.LogoutSuccess ||
                                    authState.value is AuthState.LaunchIntent)
                                    && !sessionState.value -> {
                                LoginOptions(
                                    onLoginClick = viewModel::login,
                                    onRegisterClick = viewModel::register
                                )
                                if (authState.value is AuthState.LaunchIntent) {
                                    handleONEIntent((authState.value as AuthState.LaunchIntent).intent)
                                }
                            }
                            authState.value is AuthState.AuthSuccess
                                    || sessionState.value -> {
                                LogoutOption(onLogoutClick = viewModel::logout)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleONEIntent(intent: Intent) {
        resultLauncher.launch(intent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.authIntent(result)
        }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun LoginOptions(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    Text(text = "Welcome", textAlign = TextAlign.Center)
    Button(
        modifier = Modifier
            .wrapContentSize()
            .padding(5.dp),
        onClick = onLoginClick
    ) {
        Text(text = "Login")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        modifier = Modifier
            .wrapContentSize()
            .padding(5.dp),
        onClick = onRegisterClick
    ) {
        Text(text = "Register")
    }
}

@Composable
fun LogoutOption(
    onLogoutClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .wrapContentSize()
            .padding(5.dp),
        onClick = onLogoutClick
    ) {
        Text(text = "Logout")
    }
}

@Composable
fun ColumnScope.Loading() {
    CircularProgressIndicator(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        strokeWidth = 4.dp
    )
}
