package com.example

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import com.example.actions.ScreenTargetResolver
import com.example.actions.SpatialPosition
import com.example.actions.TargetResolutionResult
import com.example.actions.UiElementNode
import com.example.memory.AppDatabase
import com.example.memory.MemoryCategory
import com.example.memory.MemoryRepository
import com.example.memory.MemoryResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches LYRA`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LYRA", appName)
    }

    @Test
    fun `screen target resolver resolves center video position`() {
        val resolver = ScreenTargetResolver()
        val elements = listOf(
            UiElementNode(
                text = "Top Documentary",
                contentDescription = "Video 1",
                className = "android.widget.FrameLayout",
                bounds = Rect(0, 100, 1080, 500),
                isClickable = true,
                isScrollable = false,
                isVisibleToUser = true,
                viewIdResourceName = "video_top"
            ),
            UiElementNode(
                text = "Robotics & AI Future",
                contentDescription = "Video 2",
                className = "android.widget.FrameLayout",
                bounds = Rect(0, 1000, 1080, 1400),
                isClickable = true,
                isScrollable = false,
                isVisibleToUser = true,
                viewIdResourceName = "video_center"
            ),
            UiElementNode(
                text = "Quantum Computing",
                contentDescription = "Video 3",
                className = "android.widget.FrameLayout",
                bounds = Rect(0, 1900, 1080, 2300),
                isClickable = true,
                isScrollable = false,
                isVisibleToUser = true,
                viewIdResourceName = "video_bottom"
            )
        )

        val result = resolver.resolveTarget("Center video open karo", elements, 1080, 2400)
        assertTrue(result is TargetResolutionResult.SingleMatch)
        val match = result as TargetResolutionResult.SingleMatch
        assertEquals("Robotics & AI Future", match.element.text)
    }

    @Test
    fun `memory repository privacy shield rejects sensitive passwords`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = MemoryRepository(db.memoryDao())

        val result = repo.saveOrUpdateMemory("bank_password", "Secret12345", MemoryCategory.FACTS)
        assertTrue(result is MemoryResult.RejectedSensitive)
    }

    @Test
    fun `memory repository updates and verifies correction correctly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val repo = MemoryRepository(db.memoryDao())
        repo.clearAll()

        // Initial save
        val save1 = repo.saveOrUpdateMemory("friend_name", "Kareem", MemoryCategory.RELATIONSHIPS)
        assertTrue(save1 is MemoryResult.Saved)

        // Correction
        val save2 = repo.saveOrUpdateMemory("friend_name", "Karima", MemoryCategory.RELATIONSHIPS)
        assertTrue(save2 is MemoryResult.Updated)
        val updateResult = save2 as MemoryResult.Updated
        assertEquals("Kareem", updateResult.previousValue)
        assertEquals("Karima", updateResult.newValue)

        val retrieved = repo.findMemory("friend_name")
        assertNotNull(retrieved)
        assertEquals("Karima", retrieved?.value)
        assertTrue(retrieved?.isVerified == true)
    }
}
