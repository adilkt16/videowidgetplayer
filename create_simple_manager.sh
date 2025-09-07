#!/bin/bash

# Create a minimal working version by replacing problematic methods with simple implementations

FILE="/home/user/Desktop/projects/videowidget/VideoWidgetPlayer/app/src/main/java/com/videowidgetplayer/widgets/WidgetVideoManager.kt"

# Create a simplified version with basic functionality only
cat > "$FILE" << 'EOF'
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
                val currentVideoUri = videoUris[0] // Simple: just play first video
                loadVideoForWidget(context, widgetId, currentVideoUri)
                PreferenceUtils.setWidgetPlayState(context, widgetId, true)
                updateWidgetPlayButton(context, widgetId, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
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
    
    private fun loadVideoForWidget(context: Context, widgetId: Int, videoUri: String) {
        Log.d(TAG, "Loading video for widget $widgetId: $videoUri")
        // This would integrate with VideoPlaybackService
        // For now, just update the widget display
        updateWidgetThumbnail(context, widgetId, videoUri)
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
EOF

echo "Created simplified WidgetVideoManager"
