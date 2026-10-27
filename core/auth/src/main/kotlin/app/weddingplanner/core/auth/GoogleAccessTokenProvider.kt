package app.weddingplanner.core.auth

import app.weddingplanner.core.datastore.UserPreferencesRepository
import app.weddingplanner.core.sheets.api.AccessTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The user must sign in or grant consent again. An IOException so OkHttp surfaces it as a failed call. */
class AuthRequiredException(message: String) : IOException(message)

/**
 * Fetches tokens silently through AuthorizationClient and keeps one in memory only.
 * Tokens are never persisted (TRD §7).
 */
@Singleton
class GoogleAccessTokenProvider @Inject constructor(
    private val auth: GoogleAuthRepository,
    private val preferences: UserPreferencesRepository,
) : AccessTokenProvider {
    @Volatile private var cached: CachedToken? = null

    override suspend fun accessToken(): String {
        cached?.takeIf { it.expiresAtMillis > System.currentTimeMillis() }?.let { return it.token }
        val email = preferences.current().accountEmail
            ?: throw AuthRequiredException("Belum masuk dengan Google.")
        return when (val outcome = auth.authorize(email)) {
            is AuthorizationOutcome.Granted -> outcome.accessToken.also(::remember)
            is AuthorizationOutcome.NeedsConsent -> throw AuthRequiredException("Masuk ulang untuk sinkronisasi.")
            is AuthorizationOutcome.Failure -> throw AuthRequiredException(outcome.message)
        }
    }

    /** Stores a token obtained interactively (after the consent screen). */
    fun remember(token: String) {
        cached = CachedToken(token, System.currentTimeMillis() + TOKEN_TTL_MILLIS)
    }

    override fun invalidate(token: String) {
        if (cached?.token == token) cached = null
    }

    private data class CachedToken(val token: String, val expiresAtMillis: Long)

    private companion object {
        // Google access tokens last ~60 min; refresh a little early.
        const val TOKEN_TTL_MILLIS = 45 * 60 * 1000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindAccessTokenProvider(impl: GoogleAccessTokenProvider): AccessTokenProvider
}
