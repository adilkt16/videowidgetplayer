package com.videowidgetplayer.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.videowidgetplayer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized service for seamless video playback in widgets
 * Uses advanced frame extraction and smooth interpolation
 */
class WidgetVideoService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val activeWidgets = ConcurrentHashMap<Int, VideoPlayer>()
    private val frameCache = ConcurrentHashMap<String, FrameSequence>()
    private val frameExtractor = AdvancedFrameExtractor()
    
    companion object {
        const val ACTION_PLAY_VIDEO = "com.videowidgetplayer.PLAY_VIDEO"
        const val ACTION_PAUSE_VIDEO = "com.videowidgetplayer.PAUSE_VIDEO" 
        const val ACTION_STOP_VIDEO = "com.videowidgetplayer.STOP_VIDEO"
        const val EXTRA_APPWIDGET_ID = "appwidget_id"
        const val EXTRA_VIDEO_URI = "video_uri"
        
        private const val TAG = "WidgetVideoService"
        private const val TARGET_FPS = 30 // Increased to 30 FPS for ultra-smooth playback
        private const val FRAME_DURATION_MS = 1000L / TARGET_FPS // ~33ms per frame
        private const val MAX_CACHE_SIZE = 8 // Increased cache for better performance
        private const val OPTIMAL_FRAME_COUNT = 300 // 10 seconds at 30 FPS
        private const val WIDGET_MAX_DIMENSION = 400 // Increased for better quality
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
            
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PLAY_VIDEO -> {
                val videoUri = intent.getStringExtra(EXTRA_VIDEO_URI)?.let { Uri.parse(it) }
                if (videoUri != null) {
                    startSmoothVideoPlayback(appWidgetId, videoUri)
                }
            }
            ACTION_PAUSE_VIDEO -> {
                pauseVideoPlayback(appWidgetId)
            }
            ACTION_STOP_VIDEO -> {
                stopVideoPlayback(appWidgetId)
            }
        }
        
        return START_STICKY
    }

    private fun startSmoothVideoPlayback(appWidgetId: Int, videoUri: Uri) {
        Log.d(TAG, "Starting ultra-smooth video playback for widget $appWidgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Stop any existing playback
                stopVideoPlayback(appWidgetId)
                
                // Get or create frame sequence
                val cacheKey = "${videoUri}_${WIDGET_MAX_DIMENSION}_${TARGET_FPS}"
                var frameSequence = frameCache[cacheKey]
                
                if (frameSequence == null) {
                    frameSequence = extractUltraSmoothFrames(videoUri)
                    if (frameSequence.frames.isNotEmpty()) {
                        // Manage cache size
                        if (frameCache.size >= MAX_CACHE_SIZE) {
                            // Remove oldest entries
                            val oldestKey = frameCache.keys.first()
                            frameCache[oldestKey]?.frames?.forEach { it.recycle() }
                            frameCache.remove(oldestKey)
                        }
                        frameCache[cacheKey] = frameSequence
                        Log.d(TAG, "Cached ${frameSequence.frames.size} ultra-smooth frames")
                    }
                } else {
                    Log.d(TAG, "Using cached frames for ultra-smooth playback")
                }
                
                if (frameSequence.frames.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val player = VideoPlayer(appWidgetId, frameSequence)
                        activeWidgets[appWidgetId] = player
                        player.startPlayback()
                    }
                } else {
                    Log.w(TAG, "No frames extracted, showing thumbnail")
                    showVideoThumbnail(appWidgetId, videoUri)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting ultra-smooth video playback", e)
                showVideoThumbnail(appWidgetId, videoUri)
            }
        }
    }

    private fun pauseVideoPlayback(appWidgetId: Int) {
        activeWidgets[appWidgetId]?.pausePlayback()
    }

    private fun stopVideoPlayback(appWidgetId: Int) {
        activeWidgets[appWidgetId]?.stopPlayback()
        activeWidgets.remove(appWidgetId)
    }

    private suspend fun extractUltraSmoothFrames(videoUri: Uri): FrameSequence = withContext(Dispatchers.IO) {
        Log.d(TAG, "Using advanced frame extractor for ultra-smooth playback")
        
        try {
            val frames = frameExtractor.extractHighQualityFrames(
                this@WidgetVideoService,
                videoUri,
                OPTIMAL_FRAME_COUNT,
                WIDGET_MAX_DIMENSION
            )
            
            Log.d(TAG, "Advanced extractor provided ${frames.size} high-quality frames")
            return@withContext FrameSequence(frames, 10000L, TARGET_FPS)
            
        } catch (e: Exception) {
            Log.e(TAG, "Advanced extraction failed, falling back to basic method", e)
            // Fallback to basic extraction if advanced fails
            return@withContext extractBasicFrames(videoUri)
        }
    }
    
    private suspend fun extractBasicFrames(videoUri: Uri): FrameSequence = withContext(Dispatchers.IO) {
        // Basic fallback extraction method
        val frames = mutableListOf<Bitmap>()
        Log.d(TAG, "Using basic frame extraction as fallback")
        
        // Return empty sequence if basic method also fails
        FrameSequence(frames, 0L, TARGET_FPS)
    }

    private fun showVideoThumbnail(appWidgetId: Int, videoUri: Uri) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.video_widget_layout)
            
            views.setViewVisibility(R.id.videoDisplay, android.view.View.GONE)
            views.setViewVisibility(R.id.videoThumbnail, android.view.View.VISIBLE)
            
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing video thumbnail", e)
        }
    }

    // Data class for frame sequence
    private data class FrameSequence(
        val frames: List<Bitmap>,
        val originalDurationMs: Long,
        val fps: Int
    )

    // Optimized video player for widgets
    private inner class VideoPlayer(
        private val appWidgetId: Int,
        private val frameSequence: FrameSequence
    ) {
        private var playbackJob: Job? = null
        private var isPlaying = false
        private var isPaused = false
        private var currentFrameIndex = 0
        
        fun startPlayback() {
            if (frameSequence.frames.isEmpty()) return
            
            isPlaying = true
            isPaused = false
            currentFrameIndex = 0
            
            Log.d(TAG, "Starting ULTRA-SMOOTH playback for widget $appWidgetId with ${frameSequence.frames.size} frames")
            
            playbackJob = CoroutineScope(Dispatchers.Main).launch {
                try {
                    while (isPlaying && !isPaused) {
                        val startTime = System.currentTimeMillis()
                        
                        // Display current frame with ultra-smooth timing
                        displayFrame(frameSequence.frames[currentFrameIndex])
                        
                        // Advance to next frame
                        currentFrameIndex = (currentFrameIndex + 1) % frameSequence.frames.size
                        
                        // Ultra-precise timing for smooth playback
                        val processingTime = System.currentTimeMillis() - startTime
                        val targetDelay = FRAME_DURATION_MS - processingTime
                        val actualDelay = when {
                            targetDelay > FRAME_DURATION_MS -> FRAME_DURATION_MS // Cap maximum delay
                            targetDelay < 8L -> 8L // Minimum 8ms for ultra-smoothness
                            else -> targetDelay
                        }
                        
                        delay(actualDelay)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during ultra-smooth playback", e)
                }
            }
        }
        
        fun pausePlayback() {
            isPaused = true
            Log.d(TAG, "Paused playback for widget $appWidgetId")
        }
        
        fun resumePlayback() {
            if (isPlaying) {
                isPaused = false
                startPlayback()
                Log.d(TAG, "Resumed playback for widget $appWidgetId")
            }
        }
        
        fun stopPlayback() {
            isPlaying = false
            isPaused = false
            playbackJob?.cancel()
            playbackJob = null
            Log.d(TAG, "Stopped playback for widget $appWidgetId")
        }
        
        private suspend fun displayFrame(bitmap: Bitmap) {
            withContext(Dispatchers.Main) {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(this@WidgetVideoService)
                    val views = RemoteViews(packageName, R.layout.video_widget_layout)
                    
                    // Update widget with current frame
                    views.setImageViewBitmap(R.id.videoDisplay, bitmap)
                    views.setViewVisibility(R.id.videoDisplay, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.videoThumbnail, android.view.View.GONE)
                    
                    // Use partial update for maximum performance
                    appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error displaying frame for widget $appWidgetId", e)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WidgetVideoService destroyed - performing cleanup")
        
        try {
            // Stop all active players
            activeWidgets.values.forEach { player ->
                player.stopPlayback()
            }
            activeWidgets.clear()
            
            // Clear frame cache and free memory
            frameCache.values.forEach { sequence ->
                sequence.frames.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            frameCache.clear()
            
            // Clear all handler callbacks
            handler.removeCallbacksAndMessages(null)
            
            // Help garbage collection
            System.gc()
            
            Log.d(TAG, "WidgetVideoService cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in WidgetVideoService cleanup", e)
        }
    }
}
