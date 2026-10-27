package app.weddingplanner.core.auth

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import app.weddingplanner.core.datastore.UserPreferencesRepository
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** OAuth Web client ID (`google.webClientId` in wedding-profile.json), provided by :app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WebClientId

const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"

sealed interface SignInResult {
    data class Success(val email: String) : SignInResult
    data object Cancelled : SignInResult
    data class Failure(val message: String) : SignInResult
}

sealed interface AuthorizationOutcome {
    data class Granted(val accessToken: String) : AuthorizationOutcome
    data class NeedsConsent(val pendingIntent: PendingIntent) : AuthorizationOutcome
    data class Failure(val message: String) : AuthorizationOutcome
}

/**
 * Sign in with Credential Manager, then authorise the `spreadsheets` scope with Play services'
 * AuthorizationClient (TRD §3.1). No client secret or refresh token is ever held by the app.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @WebClientId private val webClientId: String,
    private val preferences: UserPreferencesRepository,
) {
    private val credentialManager by lazy { CredentialManager.create(context) }
    private val authorizationClient by lazy { Identity.getAuthorizationClient(context) }

    /** [activityContext] must be an Activity: Credential Manager shows its bottom sheet over it. */
    suspend fun signIn(activityContext: Context): SignInResult {
        if (webClientId.isBlank()) {
            return SignInResult.Failure("Google Web client ID belum diisi. Isi google.webClientId di wedding-profile.json lalu build ulang.")
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
        return try {
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val email = GoogleIdTokenCredential.createFrom(credential.data).id
                preferences.setAccount(email)
                SignInResult.Success(email.lowercase())
            } else {
                SignInResult.Failure("Jenis kredensial tidak dikenali.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failure("Tidak ada akun Google di perangkat ini.")
        } catch (e: GetCredentialException) {
            SignInResult.Failure(e.message ?: "Gagal masuk dengan Google.")
        }
    }

    suspend fun authorize(email: String): AuthorizationOutcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SHEETS_SCOPE)))
            .setAccount(Account(email, "com.google"))
            .build()
        return try {
            authorizationClient.authorize(request).await().toOutcome()
        } catch (e: ApiException) {
            AuthorizationOutcome.Failure(e.message ?: "Otorisasi gagal (${e.statusCode}).")
        }
    }

    /** Result of the consent screen launched from [AuthorizationOutcome.NeedsConsent]. */
    fun authorizationFromIntent(data: Intent?): AuthorizationOutcome = try {
        authorizationClient.getAuthorizationResultFromIntent(data).toOutcome()
    } catch (e: ApiException) {
        AuthorizationOutcome.Failure("Akses Google Sheets diperlukan.")
    }

    suspend fun signOut() {
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        preferences.clear()
    }

    private fun AuthorizationResult.toOutcome(): AuthorizationOutcome {
        val intent = pendingIntent
        val token = accessToken
        return when {
            hasResolution() && intent != null -> AuthorizationOutcome.NeedsConsent(intent)
            token != null -> AuthorizationOutcome.Granted(token)
            else -> AuthorizationOutcome.Failure("Token akses tidak tersedia.")
        }
    }
}

internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
