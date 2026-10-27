package app.weddingplanner.core.sheets.api

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Supplies OAuth access tokens for the `spreadsheets` scope (implemented in :core:auth). */
interface AccessTokenProvider {
    suspend fun accessToken(): String

    /** Drops a cached token after the API rejected it with 401. */
    fun invalidate(token: String)
}

object SheetsClientFactory {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** [logBasic] logs request lines only, never bodies (guest names are personal data). */
    fun create(tokenProvider: AccessTokenProvider, logBasic: Boolean = false): SheetsApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .authenticator(TokenRefreshAuthenticator(tokenProvider))
            .apply {
                if (logBasic) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(SheetsApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SheetsApi::class.java)
    }
}

private class AuthInterceptor(private val tokens: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokens.accessToken() }
        return chain.proceed(chain.request().withBearer(token))
    }
}

/** Retries once with a fresh token when the API answers 401. */
private class TokenRefreshAuthenticator(private val tokens: AccessTokenProvider) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (failed != null) tokens.invalidate(failed)
        val fresh = runBlocking { tokens.accessToken() }
        return response.request.withBearer(fresh)
    }
}

private fun Request.withBearer(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
