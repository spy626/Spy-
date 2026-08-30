package com.example

import android.app.Application
import com.example.actions.ScreenTargetResolver
import com.example.actions.TaskExecutionEngine
import com.example.brain.AiClient
import com.example.brain.GeminiApiRepository
import com.example.brain.LyraBrainCoordinator
import com.example.memory.AppDatabase
import com.example.memory.MemoryRepository
import com.example.screen.ScreenVisionSession
import com.example.voice.MyraVoiceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LyraApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var memoryRepository: MemoryRepository
        private set

    lateinit var aiClient: AiClient
        private set

    lateinit var geminiApiRepository: GeminiApiRepository
        private set

    lateinit var screenVisionSession: ScreenVisionSession
        private set

    lateinit var targetResolver: ScreenTargetResolver
        private set

    lateinit var taskExecutionEngine: TaskExecutionEngine
        private set

    lateinit var brainCoordinator: LyraBrainCoordinator
        private set

    lateinit var voiceService: MyraVoiceService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        memoryRepository = MemoryRepository(database.memoryDao())

        aiClient = AiClient()
        geminiApiRepository = GeminiApiRepository.instance
        screenVisionSession = ScreenVisionSession(this)
        targetResolver = ScreenTargetResolver()
        taskExecutionEngine = TaskExecutionEngine(screenVisionSession, targetResolver)

        brainCoordinator = LyraBrainCoordinator(
            aiClient = aiClient,
            memoryRepository = memoryRepository,
            screenVisionSession = screenVisionSession,
            targetResolver = targetResolver,
            taskExecutionEngine = taskExecutionEngine
        )

        voiceService = MyraVoiceService(this, applicationScope, aiClient)

        // Launch reachability check at app launch
        applicationScope.launch(Dispatchers.IO) {
            geminiApiRepository.checkStartupStatus()
        }
    }

    companion object {
        lateinit var instance: LyraApp
            private set
    }
}
