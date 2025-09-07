package com.videowidgetplayer.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import com.videowidgetplayer.R
import com.videowidgetplayer.data.VideoQueueManager
import com.videowidgetplayer.data.LoopMode
import com.videowidgetplayer.services.VideoPlaybackService
import com.videowidgetplayer.services.WidgetGestureService
import com.videowidgetplayer.utils.PreferenceUtils

class WidgetVideoManager private constructor() {
    
    companion object {
        private const val TAG = "WidgetVideoManager"
        
        @Volatile
        private var INSTANCE: WidgetVideoManager? = null
        
        fun getInstance(): WidgetVideoManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetVideoManager().also { INSTANCE = it }
            }
        }
    }
    
    private val queueManager = VideoQueueManager.getInstance()
    private var playbackService: VideoPlaybackService? = null
    private var gestureService: WidgetGestureService? = null
    
    fun playVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Playing video for widget: $widgetId")
        try {
            val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
            if (videoUris.isNotEmpty()) {
                val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
                val videoUri = if (currentIndex < videoUris.size) {
                    videoUris[currentIndex]
                } else {
                    videoUris[0] // Fallback to first video
                }
                loadVideoForWidget(context, widgetId, videoUri)
                PreferenceUtils.setWidgetPlayState(context, widgetId, true)
                updateWidgetPlayButton(context, widgetId, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
        }
    }
    
    /**
     * Enhanced navigation: Go to next video in queue
     */
    fun playNextVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Playing next video for widget: $widgetId")
        try {
            val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
            if (videoUris.size > 1) {
                val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
                val nextIndex = (currentIndex + 1) % videoUris.size // Loop to beginning
                
                PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, nextIndex)
                val nextVideoUri = videoUris[nextIndex]
                
                loadVideoForWidget(context, widgetId, nextVideoUri)
                PreferenceUtils.setWidgetPlayState(context, widgetId, true)
                updateWidgetPlayButton(context, widgetId, true)
                
                Log.d(TAG, "Switched to video $nextIndex: $nextVideoUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing next video", e)
        }
    }
    
    /**
     * Enhanced navigation: Go to previous video in queue
     */
    fun playPreviousVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Playing previous video for widget: $widgetId")
        try {
            val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
            if (videoUris.size > 1) {
                val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else videoUris.size - 1 // Loop to end
                
                PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, prevIndex)
                val prevVideoUri = videoUris[prevIndex]
                
                loadVideoForWidget(context, widgetId, prevVideoUri)
                PreferenceUtils.setWidgetPlayState(context, widgetId, true)
                updateWidgetPlayButton(context, widgetId, true)
                
                Log.d(TAG, "Switched to video $prevIndex: $prevVideoUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing previous video", e)
        }
    }
    
    fun pauseVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Pausing video for widget: $widgetId")
        try {
            stopVideo(context, widgetId)
            PreferenceUtils.setWidgetPlayState(context, widgetId, false)
            updateWidgetPlayButton(context, widgetId, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing video", e)
        }
    }
    
    fun nextVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Next video for widget: $widgetId")
        try {
            val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
            val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
            val nextIndex = if (currentIndex + 1 < videoUris.size) currentIndex + 1 else 0
            
            PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, nextIndex)
            loadVideoForWidget(context, widgetId, videoUris[nextIndex])
            updateWidgetDisplay(context, widgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Error going to next video", e)
        }
    }
    
    fun previousVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Previous video for widget: $widgetId")
        try {
            val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
            val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
            val prevIndex = if (currentIndex - 1 >= 0) currentIndex - 1 else videoUris.size - 1
            
            PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, prevIndex)
            loadVideoForWidget(context, widgetId, videoUris[prevIndex])
            updateWidgetDisplay(context, widgetId)
        } catch (e: Exception) {
            Log.e(TAG, "Error going to previous video", e)
        }
    }
    
    fun toggleMute(context: Context, widgetId: Int) {
        Log.d(TAG, "Toggling mute for widget: $widgetId")
        try {
            val currentMuteState = PreferenceUtils.getWidgetMuteState(context, widgetId)
            val newMuteState = !currentMuteState
            PreferenceUtils.setWidgetMuteState(context, widgetId, newMuteState)
            updateWidgetMuteButton(context, widgetId, newMuteState)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
        }
    }
    
    fun loadVideoForWidget(context: Context, widgetId: Int, videoUri: String) {
        Log.d(TAG, "Loading video for widget $widgetId: $videoUri")
        // This would integrate with VideoPlaybackService
        // For now, just update the widget display
        updateWidgetThumbnail(context, widgetId, videoUri)
    }
    
    fun initialize(context: Context, widgetId: Int) {
        Log.d(TAG, "Initializing widget manager for: $widgetId")
        updateWidget(context, widgetId)
    }
    
    fun initializeVideoQueue(context: Context, widgetId: Int) {
        Log.d(TAG, "Initializing video queue for: $widgetId")
        val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
        if (videoUris.isNotEmpty()) {
            queueManager.initializeQueue(context, widgetId, videoUris)
        }
    }
    
    fun togglePlayPause(context: Context, widgetId: Int) {
        val isPlaying = PreferenceUtils.getWidgetPlayState(context, widgetId)
        if (isPlaying) {
            pauseVideo(context, widgetId)
        } else {
            playVideo(context, widgetId)
        }
    }
    
    fun setVolume(context: Context, widgetId: Int, volume: Float) {
        Log.d(TAG, "Setting volume for widget $widgetId: $volume")
        // Basic implementation
    }
    
    fun toggleShuffle(context: Context, widgetId: Int) {
        Log.d(TAG, "Toggling shuffle for widget: $widgetId")
        val currentShuffle = PreferenceUtils.getWidgetShuffleEnabled(context, widgetId)
        PreferenceUtils.setWidgetShuffleEnabled(context, widgetId, !currentShuffle)
    }
    
    fun cycleLoopMode(context: Context, widgetId: Int) {
        Log.d(TAG, "Cycling loop mode for widget: $widgetId")
        val currentMode = PreferenceUtils.getWidgetLoopMode(context, widgetId)
        val nextMode = (currentMode + 1) % 3
        PreferenceUtils.setWidgetLoopMode(context, widgetId, nextMode)
    }
    
    fun nextVideoWithGesture(context: Context, widgetId: Int) {
        nextVideo(context, widgetId)
    }
    
    fun previousVideoWithGesture(context: Context, widgetId: Int) {
        previousVideo(context, widgetId)
    }
    
    fun enableGestureSupport(context: Context, widgetId: Int) {
        Log.d(TAG, "Enabling gesture support for widget: $widgetId")
    }
    
    fun disableGestureSupport(context: Context, widgetId: Int) {
        Log.d(TAG, "Disabling gesture support for widget: $widgetId")
    }
    
    fun setGestureSensitivity(context: Context, sensitivity: Float) {
        Log.d(TAG, "Setting gesture sensitivity: $sensitivity")
    }
    
    fun release(context: Context, widgetId: Int) {
        Log.d(TAG, "Releasing resources for widget: $widgetId")
        queueManager.removeQueue(widgetId)
    }
    
    private fun stopVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Stopping video for widget: $widgetId")
        // Stop playback service if needed
    }
    
    private fun updateWidgetPlayButton(context: Context, widgetId: Int, isPlaying: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.video_widget)
        
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        views.setImageViewResource(R.id.play_pause_button, playPauseIcon)
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    private fun updateWidgetMuteButton(context: Context, widgetId: Int, isMuted: Boolean) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.video_widget)
        
        val muteIcon = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
        views.setImageViewResource(R.id.mute_button, muteIcon)
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    private fun updateWidgetThumbnail(context: Context, widgetId: Int, videoUri: String) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.video_widget)
        
        // Set placeholder for now
        views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    private fun updateWidgetDisplay(context: Context, widgetId: Int) {
        Log.d(TAG, "Updating widget display for: $widgetId")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.video_widget)
        
        // Update queue info
        val videoUris = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
        val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
        val queueInfo = "${currentIndex + 1} of ${videoUris.size}"
        views.setTextViewText(R.id.queue_info, queueInfo)
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    fun updateWidget(context: Context, widgetId: Int) {
        Log.d(TAG, "Updating widget: $widgetId")
        try {
            updateWidgetDisplay(context, widgetId)
            
            val isPlaying = PreferenceUtils.getWidgetPlayState(context, widgetId)
            updateWidgetPlayButton(context, widgetId, isPlaying)
            
            val isMuted = PreferenceUtils.getWidgetMuteState(context, widgetId)
            updateWidgetMuteButton(context, widgetId, isMuted)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
        }
    }
}
