package com.example
 
import com.example.brain.GeminiApiRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiApiRepositoryTest {

    @Test
    fun testBuildConfigGeminiApiKeyIsNonNull() {
        val apiKey = BuildConfig.GEMINI_API_KEY
        assertNotNull("BuildConfig.GEMINI_API_KEY must not be null", apiKey)
    }

    @Test
    fun testGeminiApiRepositoryKeyCheck() {
        val repository = GeminiApiRepository.instance
        val resolvedKey = repository.getApiKey()
        assertNotNull("Resolved API key string should never be null", resolvedKey)
        
        // Test custom key validation
        val hasCustom = repository.isApiKeyConfigured("test-valid-api-key-12345")
        assertTrue("Repository should identify valid custom key", hasCustom)
    }

    @Test
    fun testGeminiEndpointReachabilityCheck() = runBlocking {
        val repository = GeminiApiRepository.instance
        // Perform startup check helper execution
        val result = repository.checkStartupStatus()
        assertNotNull("Status result should not be null", result)
        assertNotNull("Diagnostic message should be populated", result.message)
    }
}
