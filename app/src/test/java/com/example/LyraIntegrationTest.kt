package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.actions.AccessibilityHelperService
import com.example.actions.ScreenTargetResolver
import com.example.actions.TaskExecutionEngine
import com.example.brain.AiClient
import com.example.brain.LyraBrainCoordinator
import com.example.memory.AppDatabase
import com.example.memory.MemoryCategory
import com.example.memory.MemoryRepository
import com.example.screen.ScreenVisionSession
import com.example.voice.MyraVoiceService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyraIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var memoryRepo: MemoryRepository
    private lateinit var aiClient: AiClient
    private lateinit var screenVision: ScreenVisionSession
    private lateinit var targetResolver: ScreenTargetResolver
    private lateinit var taskEngine: TaskExecutionEngine
    private lateinit var brain: LyraBrainCoordinator

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.getInstance(context)
        memoryRepo = MemoryRepository(db.memoryDao())
        memoryRepo.clearAll()
        memoryRepo.clearChat()
        aiClient = AiClient()
        screenVision = ScreenVisionSession(context)
        targetResolver = ScreenTargetResolver()
        taskEngine = TaskExecutionEngine(screenVision, targetResolver)
        brain = LyraBrainCoordinator(
            aiClient = aiClient,
            memoryRepository = memoryRepo,
            screenVisionSession = screenVision,
            targetResolver = targetResolver,
            taskExecutionEngine = taskEngine
        )
    }

    @Test
    fun test1_TextChatProducesActualResponseAndPersists() = runBlocking {
        // Step 1: User sends "Hello LYRA"
        val response = brain.processUserMessage("Hello LYRA")
        assertNotNull(response)
        assertTrue("Response should not be blank", response.isNotBlank())
        assertTrue("Response should mention LYRA or assistant capability", response.contains("LYRA") || response.contains("assistant") || response.contains("help"))

        // Step 2: Verify chat messages persisted in Room Database
        val chatHistory = memoryRepo.allChatMessages.first()
        assertTrue("Chat history should contain at least user and assistant turns", chatHistory.size >= 2)
        assertEquals("user", chatHistory[chatHistory.size - 2].role)
        assertEquals("Hello LYRA", chatHistory[chatHistory.size - 2].content)
        assertEquals("assistant", chatHistory[chatHistory.size - 1].role)
    }

    @Test
    fun test2_VoiceIntegrationPipeline() = runBlocking {
        var speechResultReceived = ""
        val lyraApp = context as LyraApp
        val voiceService = MyraVoiceService(context, lyraApp.applicationScope, lyraApp.aiClient)
        voiceService.setOnSpeechResultListener { utterance ->
            speechResultReceived = utterance
        }

        // Simulate voice recognition returning "Hello LYRA"
        val userVoiceInput = "Hello LYRA"
        val aiResponse = brain.processUserMessage(userVoiceInput)
        assertTrue(aiResponse.isNotBlank())

        // Verify audio speech trigger and immediate barge-in interruption
        voiceService.speak(aiResponse)
        voiceService.stopSpeaking() // Verify immediate cancellation without crash
        assertEquals(com.example.voice.VoiceState.IDLE, voiceService.voiceState.value)
    }

    @Test
    fun test3_MemoryVaultStoreAndRecall() = runBlocking {
        // Store memory via Brain
        val storeResponse = brain.processUserMessage("My friend's name is Kareem")
        assertTrue(storeResponse.contains("Kareem") || storeResponse.contains("remember") || storeResponse.contains("Saved"))

        // Recall memory via Brain
        val recallResponse = brain.processUserMessage("What is my friend's name?")
        assertTrue("Expected recall to return Kareem, got: $recallResponse", recallResponse.contains("Kareem"))

        // Update memory via Brain correction
        val updateResponse = brain.processUserMessage("No, Karima")
        assertTrue(updateResponse.contains("Karima") || updateResponse.contains("updated") || updateResponse.contains("corrected"))

        // Verify updated in Room database
        val updatedMemory = memoryRepo.findMemory("friend_name")
        assertNotNull(updatedMemory)
        assertEquals("Karima", updatedMemory?.value)
    }

    @Test
    fun test4_ScreenVisionFrameProcessing() = runBlocking {
        // Frame generation
        val frame = screenVision.captureLatestFrame()
        assertNotNull(frame)
        assertTrue("Frame width should be valid", frame.width > 0)
        assertTrue("Frame height should be valid", frame.height > 0)

        val base64 = screenVision.getLatestFrameBase64()
        assertTrue("Base64 frame string should be valid", base64.isNotBlank())

        // Screen analysis query
        val visionResponse = brain.processUserMessage("What is on my screen?")
        assertTrue("Vision response should describe screen", visionResponse.isNotBlank())
    }

    @Test
    fun test5_AccessibilityServiceDeclaration() {
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SERVICES)
        val services = packageInfo.services ?: emptyArray()

        val accessibilityService = services.firstOrNull { it.name.contains("AccessibilityHelperService") }
        assertNotNull("AccessibilityHelperService must be declared in AndroidManifest.xml", accessibilityService)
        assertEquals(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            accessibilityService?.permission
        )
    }
}
