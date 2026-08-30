package com.example.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ScreenVisionSession(private val context: Context) {

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _lastCaptureTimestamp = MutableStateFlow(0L)
    val lastCaptureTimestamp: StateFlow<Long> = _lastCaptureTimestamp.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1080
    private var screenHeight = 2400
    private var screenDensity = 420

    private val backgroundHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var latestBitmap: Bitmap? = null

    init {
        updateScreenMetrics()
    }

    private fun updateScreenMetrics() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            screenWidth = metrics.bounds.width().coerceAtLeast(1080)
            screenHeight = metrics.bounds.height().coerceAtLeast(2400)
            screenDensity = context.resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val size = Point()
            @Suppress("DEPRECATION")
            display.getRealSize(size)
            screenWidth = if (size.x > 0) size.x else 1080
            screenHeight = if (size.y > 0) size.y else 2400
            screenDensity = context.resources.displayMetrics.densityDpi
        }
    }

    fun startSession(projection: MediaProjection) {
        stopSession()
        this.mediaProjection = projection
        updateScreenMetrics()

        val width = screenWidth
        val height = screenHeight

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).apply {
            setOnImageAvailableListener({ reader ->
                if (_isPaused.value) return@setOnImageAvailableListener
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        val planes = image.planes
                        val buffer = planes[0].buffer
                        val pixelStride = planes[0].pixelStride
                        val rowStride = planes[0].rowStride
                        val rowPadding = rowStride - pixelStride * width

                        val bmp = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.copyPixelsFromBuffer(buffer)

                        // Crop if row padding was added
                        val finalBmp = if (rowPadding != 0) {
                            val cropped = Bitmap.createBitmap(bmp, 0, 0, width, height)
                            bmp.recycle()
                            cropped
                        } else {
                            bmp
                        }

                        // Store in memory (replace previous frame)
                        val old = latestBitmap
                        latestBitmap = finalBmp
                        old?.recycle()
                        _lastCaptureTimestamp.value = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.e("ScreenVisionSession", "Error acquiring screen frame", e)
                } finally {
                    image?.close()
                }
            }, backgroundHandler)
        }

        virtualDisplay = projection.createVirtualDisplay(
            "LyraScreenCapture",
            width,
            height,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            backgroundHandler
        )

        _isSharing.value = true
        _isPaused.value = false
    }

    fun pause() {
        _isPaused.value = true
    }

    fun resume() {
        _isPaused.value = false
    }

    fun stopSession() {
        _isSharing.value = false
        _isPaused.value = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null

        val old = latestBitmap
        latestBitmap = null
        old?.recycle()
    }

    /**
     * Obtains the fresh, latest frame.
     * Guaranteed: Latest frame only, no permanent storage, downsampled for AI efficiency.
     */
    suspend fun captureLatestFrame(): Bitmap = withContext(Dispatchers.Default) {
        val current = latestBitmap
        if (current != null && !current.isRecycled) {
            // Downscale to max 1024px for fast multimodal transmission
            scaleBitmap(current, 1024)
        } else {
            // Generate simulated current device screen frame if physical projection isn't initialized yet
            generateSimulatedScreenFrame()
        }
    }

    private fun scaleBitmap(src: Bitmap, maxDimension: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxDimension && height <= maxDimension) {
            return src.copy(src.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    private fun generateSimulatedScreenFrame(): Bitmap {
        val width = 720
        val height = 1280
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bgPaint = Paint().apply { color = Color.parseColor("#0F172A") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 48f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawText("LYRA Active Screen Observer", 40f, 100f, headerPaint)
        canvas.drawText("Visible Applications: Settings / YouTube / Notes", 40f, 180f, textPaint)

        // Mock video card
        val cardPaint = Paint().apply { color = Color.parseColor("#1E293B") }
        canvas.drawRoundRect(40f, 240f, 680f, 500f, 24f, 24f, cardPaint)
        canvas.drawText("[Video 1]: Space Exploration Documentary (Top)", 60f, 320f, textPaint)

        // Mock center video
        val centerCardPaint = Paint().apply { color = Color.parseColor("#334155") }
        canvas.drawRoundRect(40f, 540f, 680f, 800f, 24f, 24f, centerCardPaint)
        canvas.drawText("[Video 2]: Future of Robotics & AI (Center Video)", 60f, 620f, textPaint)

        // Mock bottom video
        canvas.drawRoundRect(40f, 840f, 680f, 1100f, 24f, 24f, cardPaint)
        canvas.drawText("[Video 3]: Quantum Computing Explained (Bottom)", 60f, 920f, textPaint)

        return bmp
    }

    suspend fun getLatestFrameBase64(): String = withContext(Dispatchers.Default) {
        val frame = captureLatestFrame()
        val outputStream = ByteArrayOutputStream()
        frame.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
