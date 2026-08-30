package com.example.brain

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

data class GeminiEndpointCheckResult(
    val isApiKeyPresent: Boolean,
    val isEndpointReachable: Boolean,
    val httpStatusCode: Int? = null,
    val message: String
)

class GeminiApiRepository(
    private val retrofitClient: GeminiRetrofitClient = GeminiRetrofitClient
) {
    /**
     * Checks whether BuildConfig.GEMINI_API_KEY is non-null and not blank.
     */
    fun isApiKeyConfigured(customKey: String? = null): Boolean {
        val key = retrofitClient.getApiKey(customKey)
        return key.isNotBlank() && key != "YOUR_GEMINI_API_KEY"
    }

    /**
     * Retrieves the current active Gemini API Key from BuildConfig or custom user override.
     */
    fun getApiKey(customKey: String? = null): String {
        return retrofitClient.getApiKey(customKey)
    }

    /**
     * Verifies network reachability of the Google Generative Language API endpoint.
     * Executes a fast HTTP HEAD check against the API server.
     * Returns true if a response code is received from the server (network is reachable).
     */
    suspend fun verifyEndpointReachability(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/")
                .head()
                .build()

            retrofitClient.okHttpClient.newCall(request).execute().use { response ->
                // Receiving any HTTP status response (e.g. 200, 400, 404) indicates network reachability
                val reachable = response.code in 200..499
                Log.d("GeminiApiRepository", "Gemini endpoint reachable with HTTP ${response.code}")
                Result.success(reachable)
            }
        } catch (e: IOException) {
            Log.w("GeminiApiRepository", "Gemini endpoint is not reachable via network: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GeminiApiRepository", "Unexpected error verifying Gemini reachability", e)
            Result.failure(e)
        }
    }

    /**
     * Comprehensive startup diagnostic checking both key presence and endpoint reachability.
     */
    suspend fun checkStartupStatus(customKey: String? = null): GeminiEndpointCheckResult = withContext(Dispatchers.IO) {
        val hasKey = isApiKeyConfigured(customKey)
        val reachabilityResult = verifyEndpointReachability()
        val isReachable = reachabilityResult.getOrDefault(false)

        val message = when {
            hasKey && isReachable -> "Gemini API key is configured and endpoint is reachable."
            !hasKey && isReachable -> "Gemini endpoint is reachable, but GEMINI_API_KEY is not configured."
            hasKey && !isReachable -> "Gemini API key is configured, but endpoint is unreachable (Check network connection)."
            else -> "Gemini API key is missing and endpoint is unreachable."
        }

        GeminiEndpointCheckResult(
            isApiKeyPresent = hasKey,
            isEndpointReachable = isReachable,
            message = message
        )
    }

    companion object {
        val instance: GeminiApiRepository by lazy { GeminiApiRepository() }
    }
}
