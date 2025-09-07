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
import com.videowidgetplayer.data.SelectedVideosManager
import com.videowidgetplayer.utils.WidgetPreferences
import com.videowidgetplayer.widget.VideoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service for simulating video playback in widgets using frame sequences
 * Since full video playback in widgets is limited, this creates a frame-by-frame animation
 */
class WidgetVideoService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val activeWidgets = mutableMapOf<Int, WidgetVideoAnimator>()
    
    companion object {
        const val ACTION_PLAY_VIDEO = "com.videowidgetplayer.PLAY_VIDEO"
        const val ACTION_PAUSE_VIDEO = "com.videowidgetplayer.PAUSE_VIDEO"
        const val ACTION_STOP_VIDEO = "com.videowidgetplayer.STOP_VIDEO"
        const val EXTRA_APPWIDGET_ID = "appwidget_id"
        const val EXTRA_VIDEO_URI = "video_uri"
        private const val TAG = "WidgetVideoService"
        private const val FRAME_INTERVAL = 500L // 500ms between frames (2 FPS for widget)
        private const val MAX_FRAMES = 20 // Maximum frames to extract per video
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
                    startVideoAnimation(appWidgetId, videoUri)
                }
            }
            ACTION_PAUSE_VIDEO -> {
                pauseVideoAnimation(appWidgetId)
            }
            ACTION_STOP_VIDEO -> {
                stopVideoAnimation(appWidgetId)
            }
        }
        
        return START_STICKY
    }

    private fun startVideoAnimation(appWidgetId: Int, videoUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Stop existing animator for this widget if any
                stopVideoAnimation(appWidgetId)
                
                // Extract frames from video
                val frames = extractVideoFrames(videoUri)
                
                if (frames.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val animator = WidgetVideoAnimator(appWidgetId, frames)
                        activeWidgets[appWidgetId] = animator
                        animator.start()
                    }
                    
                    Log.d(TAG, "Started video animation for widget $appWidgetId with ${frames.size} frames")
                } else {
                    Log.w(TAG, "No frames extracted for video: $videoUri")
                    // Fallback to showing single thumbnail
                    showVideoThumbnail(appWidgetId, videoUri)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting video animation for widget $appWidgetId", e)
                showVideoThumbnail(appWidgetId, videoUri)
            }
        }
    }

    private fun pauseVideoAnimation(appWidgetId: Int) {
        activeWidgets[appWidgetId]?.pause()
    }

    private fun stopVideoAnimation(appWidgetId: Int) {
        activeWidgets[appWidgetId]?.stop()
        activeWidgets.remove(appWidgetId)
    }

    private suspend fun extractVideoFrames(videoUri: Uri): List<Bitmap> = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(this@WidgetVideoService, videoUri)
            
            // Get video duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            if (duration > 0) {
                // Extract frames at regular intervals
                val frameInterval = duration / MAX_FRAMES.coerceAtMost((duration / 1000).toInt())
                
                for (i in 0 until MAX_FRAMES) {
                    val timeUs = (i * frameInterval * 1000).coerceAtMost(duration * 1000 - 1000)
                    
                    try {
                        val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        bitmap?.let { 
                            // Scale down for widget use
                            val scaledBitmap = Bitmap.createScaledBitmap(it, 
                                it.width / 2, it.height / 2, true)
                            frames.add(scaledBitmap)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error extracting frame at ${timeUs}us", e)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video frames", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
        
        frames
    }

    private fun showVideoThumbnail(appWidgetId: Int, videoUri: Uri) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            VideoWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error showing thumbnail for widget $appWidgetId", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop all active animators
        activeWidgets.values.forEach { it.stop() }
        activeWidgets.clear()
    }

    /**
     * Inner class to handle frame-by-frame animation for widgets
     */
    private inner class WidgetVideoAnimator(
        private val appWidgetId: Int,
        private val frames: List<Bitmap>
    ) {
        private var currentFrameIndex = 0
        private var isRunning = false
        private var animationRunnable: Runnable? = null

        fun start() {
            if (frames.isEmpty()) return
            
            isRunning = true
            currentFrameIndex = 0
            scheduleNextFrame()
        }

        fun pause() {
            isRunning = false
            animationRunnable?.let { handler.removeCallbacks(it) }
        }

        fun stop() {
            isRunning = false
            animationRunnable?.let { handler.removeCallbacks(it) }
            
            // Cleanup bitmaps
            frames.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }

        private fun scheduleNextFrame() {
            if (!isRunning) return
            
            animationRunnable = Runnable {
                if (isRunning && currentFrameIndex < frames.size) {
                    displayFrame(frames[currentFrameIndex])
                    currentFrameIndex = (currentFrameIndex + 1) % frames.size
                    scheduleNextFrame()
                }
            }
            
            handler.postDelayed(animationRunnable!!, FRAME_INTERVAL)
        }

        private fun displayFrame(bitmap: Bitmap) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(this@WidgetVideoService)
                val views = RemoteViews(packageName, R.layout.video_widget_layout)
                
                // Show the frame
                views.setImageViewBitmap(R.id.videoDisplay, bitmap)
                views.setViewVisibility(R.id.videoDisplay, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.videoThumbnail, android.view.View.GONE)
                
                // Update widget state
                val widgetPrefs = WidgetPreferences(this@WidgetVideoService)
                
                // Set up button actions (this recreates the click handlers)
                VideoWidgetProvider.updateAppWidget(this@WidgetVideoService, appWidgetManager, appWidgetId)
                
                // Then update just the image
                views.setImageViewBitmap(R.id.videoDisplay, bitmap)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying frame for widget $appWidgetId", e)
            }
        }
    }
}
