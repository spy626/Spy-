package com.example.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

data class UiElementNode(
    val text: String,
    val contentDescription: String,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isVisibleToUser: Boolean,
    val viewIdResourceName: String?,
    val nodeRef: WeakReference<AccessibilityNodeInfo>? = null
)

class AccessibilityHelperService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d("AccessibilityHelper", "LYRA Accessibility Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track latest event timestamp or window changes if needed
    }

    override fun onInterrupt() {
        Log.d("AccessibilityHelper", "LYRA Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceActive.value = false
        }
    }

    /**
     * Traverses the active window and collects visible UI elements.
     */
    fun collectVisibleElements(): List<UiElementNode> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<UiElementNode>()
        traverseNode(rootNode, elements)
        return elements
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<UiElementNode>) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString() ?: ""
        val isVisible = node.isVisibleToUser

        if (isVisible && (text.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isScrollable)) {
            list.add(
                UiElementNode(
                    text = text,
                    contentDescription = desc,
                    className = className,
                    bounds = bounds,
                    isClickable = node.isClickable,
                    isScrollable = node.isScrollable,
                    isVisibleToUser = isVisible,
                    viewIdResourceName = node.viewIdResourceName,
                    nodeRef = WeakReference(node)
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, list)
        }
    }

    /**
     * Clicks directly on a node if available or taps at its center.
     */
    suspend fun clickElement(element: UiElementNode): Boolean {
        val node = element.nodeRef?.get()
        if (node != null && node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }

        // Tap center bounds if direct action failed or node was unclickable parent
        val centerX = element.bounds.centerX().toFloat()
        val centerY = element.bounds.centerY().toFloat()
        return tapAt(centerX, centerY)
    }

    /**
     * Dispatches a tap gesture at coordinate (x, y).
     */
    suspend fun tapAt(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        return deferred.await()
    }

    /**
     * Dispatches a swipe gesture from (startX, startY) to (endX, endY).
     */
    suspend fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val deferred = CompletableDeferred<Boolean>()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                deferred.complete(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                deferred.complete(false)
            }
        }, null)

        return deferred.await()
    }

    /**
     * Performs a scroll down or up.
     */
    suspend fun scroll(directionDown: Boolean): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findScrollableNode(root)
            if (scrollable != null) {
                val action = if (directionDown) {
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                } else {
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                }
                if (scrollable.performAction(action)) {
                    return true
                }
            }
        }

        // Fallback to gesture swipe
        val displayMetrics = resources.displayMetrics
        val centerX = displayMetrics.widthPixels / 2f
        val startY = if (directionDown) displayMetrics.heightPixels * 0.75f else displayMetrics.heightPixels * 0.25f
        val endY = if (directionDown) displayMetrics.heightPixels * 0.25f else displayMetrics.heightPixels * 0.75f
        return swipe(centerX, startY, centerX, endY, 350)
    }

    private fun findScrollableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    companion object {
        var instance: AccessibilityHelperService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()
    }
}
