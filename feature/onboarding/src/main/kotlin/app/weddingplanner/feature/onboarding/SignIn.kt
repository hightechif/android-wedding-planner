package app.weddingplanner.feature.onboarding

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.weddingplanner.core.auth.AuthorizationOutcome
import app.weddingplanner.core.auth.GoogleAccessTokenProvider
import app.weddingplanner.core.auth.GoogleAuthRepository
import app.weddingplanner.core.auth.SignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val consentIntent: PendingIntent? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val auth: GoogleAuthRepository,
    private val tokens: GoogleAccessTokenProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun signIn(activityContext: Context) {
        _state.update { SignInUiState(loading = true) }
        viewModelScope.launch {
            when (val result = auth.signIn(activityContext)) {
                is SignInResult.Success -> handle(auth.authorize(result.email))
                SignInResult.Cancelled -> _state.update { SignInUiState() }
                is SignInResult.Failure -> _state.update { SignInUiState(error = result.message) }
            }
        }
    }

    fun onConsentLaunched() = _state.update { it.copy(consentIntent = null) }

    fun onConsentResult(data: Intent?) = handle(auth.authorizationFromIntent(data))

    private fun handle(outcome: AuthorizationOutcome) {
        when (outcome) {
            is AuthorizationOutcome.Granted -> {
                tokens.remember(outcome.accessToken)
                _state.update { SignInUiState(signedIn = true) }
            }
            is AuthorizationOutcome.NeedsConsent -> _state.update { it.copy(consentIntent = outcome.pendingIntent) }
            is AuthorizationOutcome.Failure -> _state.update { SignInUiState(error = outcome.message) }
        }
    }
}

@Composable
internal fun SignInScreen(onSignedIn: () -> Unit, viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onConsentResult(it.data)
    }
    LaunchedEffect(state.consentIntent) {
        state.consentIntent?.let {
            consentLauncher.launch(IntentSenderRequest.Builder(it).build())
            viewModel.onConsentLaunched()
        }
    }
    LaunchedEffect(state.signedIn) { if (state.signedIn) onSignedIn() }

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Masuk", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Gunakan akun Google yang punya akses ke sheet pernikahan kalian. Data tetap di Google Sheet kalian.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.signIn(context) },
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Masuk dengan Google")
            }
        }
        state.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
