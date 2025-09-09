package com.videowidgetplayer.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.videowidgetplayer.R
import com.videowidgetplayer.utils.MemoryLeakDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple and reliable video service for widget playback
 * Focuses on working video display rather than complex optimizations
 */
class SimpleWidgetVideoService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val activeAnimations = ConcurrentHashMap<Int, VideoAnimation>()
    private val videoFrameCache = ConcurrentHashMap<String, List<Bitmap>>()
    private var serviceTrackingKey: String? = null
    
    // Service-scoped coroutine scope for proper cleanup
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        const val ACTION_START_VIDEO = "START_VIDEO"
        const val ACTION_STOP_VIDEO = "STOP_VIDEO"
        const val ACTION_PAUSE_VIDEO = "PAUSE_VIDEO"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_VIDEO_URI = "video_uri"
        
        private const val TAG = "SimpleWidgetVideoService"
        private const val FRAME_DELAY_MS = 100L // 10 FPS for reliable playback
        private const val MAX_FRAMES = 50 // Reasonable frame count
        private const val TARGET_SIZE = 200 // Reasonable widget size
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Track this service instance for leak detection
        serviceTrackingKey = "SimpleVideoService_${System.currentTimeMillis()}"
        MemoryLeakDetector.trackObject(serviceTrackingKey!!, this)
        Log.d(TAG, "Service created and tracked: $serviceTrackingKey")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, -1) ?: -1
        if (widgetId == -1) {
            Log.e(TAG, "Invalid widget ID")
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START_VIDEO -> {
                val videoUriString = intent.getStringExtra(EXTRA_VIDEO_URI)
                if (videoUriString != null) {
                    val videoUri = Uri.parse(videoUriString)
                    startSimpleVideoPlayback(widgetId, videoUri)
                } else {
                    Log.e(TAG, "No video URI provided")
                }
            }
            ACTION_PAUSE_VIDEO -> {
                pauseVideoPlayback(widgetId)
            }
            ACTION_STOP_VIDEO -> {
                stopVideoPlayback(widgetId)
            }
        }
        
        return START_NOT_STICKY // Don't restart service automatically
    }

    private fun startSimpleVideoPlayback(widgetId: Int, videoUri: Uri) {
        Log.d(TAG, "Starting simple video playback for widget $widgetId")
        
        // Stop any existing animation
        stopVideoPlayback(widgetId)
        
        serviceScope.launch(Dispatchers.IO) {
            try {
                // Check cache first
                val cacheKey = videoUri.toString()
                var frames = videoFrameCache[cacheKey]
                
                if (frames == null) {
                    Log.d(TAG, "Extracting frames for $cacheKey")
                    frames = extractSimpleFrames(videoUri)
                    if (frames.isNotEmpty()) {
                        videoFrameCache[cacheKey] = frames
                        Log.d(TAG, "Cached ${frames.size} frames")
                    }
                } else {
                    Log.d(TAG, "Using cached frames (${frames.size} frames)")
                }
                
                if (frames.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val animation = VideoAnimation(widgetId, frames)
                        activeAnimations[widgetId] = animation
                        animation.start()
                        Log.d(TAG, "Started animation for widget $widgetId")
                    }
                } else {
                    Log.w(TAG, "No frames extracted, showing static thumbnail")
                    showStaticThumbnail(widgetId, videoUri)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting video playback", e)
                showStaticThumbnail(widgetId, videoUri)
            }
        }
    }

    private suspend fun extractSimpleFrames(videoUri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this@SimpleWidgetVideoService, videoUri)
            
            // Get basic video info
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            if (duration <= 0) {
                Log.w(TAG, "Invalid video duration: $duration")
                return@withContext frames
            }
            
            // Extract limited frames for performance
            val maxDuration = minOf(duration, 10000L) // Max 10 seconds
            val frameCount = minOf(MAX_FRAMES, (maxDuration / 200L).toInt().coerceAtLeast(5))
            val frameInterval = maxDuration / frameCount
            
            Log.d(TAG, "Extracting $frameCount frames from ${maxDuration}ms video")
            
            for (i in 0 until frameCount) {
                try {
                    val timeMs = (i * frameInterval).coerceAtMost(maxDuration - 100)
                    val timeUs = timeMs * 1000L
                    
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        // Simple scaling for widget
                        val scaledBitmap = createSimpleScaledBitmap(bitmap)
                        if (scaledBitmap != null) {
                            frames.add(scaledBitmap)
                        }
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract frame $i", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in frame extraction", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing retriever", e)
            }
        }
        
        Log.d(TAG, "Successfully extracted ${frames.size} frames")
        frames
    }

    private fun createSimpleScaledBitmap(originalBitmap: Bitmap): Bitmap? {
        return try {
            val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val (targetWidth, targetHeight) = if (aspectRatio > 1) {
                Pair(TARGET_SIZE, (TARGET_SIZE / aspectRatio).toInt())
            } else {
                Pair((TARGET_SIZE * aspectRatio).toInt(), TARGET_SIZE)
            }
            
            Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "Error scaling bitmap", e)
            null
        }
    }

    private fun showStaticThumbnail(widgetId: Int, videoUri: Uri) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val thumbnail = extractThumbnail(videoUri)
                withContext(Dispatchers.Main) {
                    val appWidgetManager = AppWidgetManager.getInstance(this@SimpleWidgetVideoService)
                    val views = RemoteViews(packageName, R.layout.video_widget_layout)
                    
                    if (thumbnail != null) {
                        views.setImageViewBitmap(R.id.videoDisplay, thumbnail)
                        views.setViewVisibility(R.id.videoDisplay, android.view.View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.videoDisplay, android.view.View.GONE)
                    }
                    
                    views.setViewVisibility(R.id.videoThumbnail, android.view.View.VISIBLE)
                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error showing thumbnail", e)
            }
        }
    }

    private fun extractThumbnail(videoUri: Uri): Bitmap? {
        var retriever: MediaMetadataRetriever? = null
        var originalBitmap: Bitmap? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, videoUri)
            originalBitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            
            val scaledBitmap = originalBitmap?.let { createSimpleScaledBitmap(it) }
            
            // Important: Recycle original bitmap if it's different from scaled
            if (originalBitmap != null && originalBitmap != scaledBitmap) {
                originalBitmap.recycle()
            }
            
            scaledBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting thumbnail", e)
            // Make sure to recycle on error
            originalBitmap?.recycle()
            null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing thumbnail retriever", e)
            }
        }
    }

    private fun pauseVideoPlayback(widgetId: Int) {
        activeAnimations[widgetId]?.pause()
        Log.d(TAG, "Paused video for widget $widgetId")
    }

    private fun stopVideoPlayback(widgetId: Int) {
        activeAnimations[widgetId]?.stop()
        activeAnimations.remove(widgetId)
        
        Log.d(TAG, "Stopped video for widget $widgetId")
        
        // Stop service if no active widgets remain
        if (activeAnimations.isEmpty()) {
            Log.d(TAG, "No active widgets remaining, stopping service")
            stopSelf()
        }
    }

    /**
     * Simple video animation class
     */
    private inner class VideoAnimation(
        private val widgetId: Int,
        private val frames: List<Bitmap>
    ) {
        private var currentFrame = 0
        private var isRunning = false
        private var isPaused = false
        private var animationJob: Job? = null

        fun start() {
            if (frames.isEmpty()) {
                Log.w(TAG, "No frames to animate")
                return
            }
            
            isRunning = true
            isPaused = false
            currentFrame = 0
            
            Log.d(TAG, "Starting animation with ${frames.size} frames")
            
            animationJob = serviceScope.launch(Dispatchers.Main) {
                try {
                    while (isRunning && !isPaused) {
                        // Display current frame
                        displayFrame(frames[currentFrame])
                        
                        // Move to next frame
                        currentFrame = (currentFrame + 1) % frames.size
                        
                        // Wait before next frame
                        delay(FRAME_DELAY_MS)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in animation loop", e)
                }
            }
        }

        fun pause() {
            isPaused = true
        }

        fun stop() {
            isRunning = false
            isPaused = false
            
            // Cancel coroutine and wait for completion
            animationJob?.cancel()
            animationJob = null
            
            Log.d(TAG, "Animation stopped and cleaned up for widget $widgetId")
        }

        private suspend fun displayFrame(bitmap: Bitmap) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(this@SimpleWidgetVideoService)
                val views = RemoteViews(packageName, R.layout.video_widget_layout)
                
                // Update the widget with current frame
                views.setImageViewBitmap(R.id.videoDisplay, bitmap)
                views.setViewVisibility(R.id.videoDisplay, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.videoThumbnail, android.view.View.GONE)
                
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying frame", e)
                // Stop animation on repeated errors to prevent leak
                if (!isRunning) {
                    stop()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed - performing complete cleanup")
        
        try {
            // Stop all animations first
            activeAnimations.values.forEach { animation ->
                animation.stop()
            }
            activeAnimations.clear()
            
            // Clear cache and free all bitmap memory
            videoFrameCache.values.forEach { frames ->
                frames.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            videoFrameCache.clear()
            
            // Remove all handler callbacks
            handler.removeCallbacksAndMessages(null)
            
            // Cancel all coroutines in service scope
            serviceScope.cancel()
            
            // Release this service from leak tracking
            serviceTrackingKey?.let { key ->
                MemoryLeakDetector.releaseObject(key)
                Log.d(TAG, "Released service from tracking: $key")
            }
            serviceTrackingKey = null
            
            // Force garbage collection to help free memory immediately
            System.gc()
            
            Log.d(TAG, "Service cleanup completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during service cleanup", e)
        }
    }
}
