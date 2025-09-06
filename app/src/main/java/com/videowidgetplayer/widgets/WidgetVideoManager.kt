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
import com.videowidgetplayer.services.VideoPlaybackService
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
    
    private var videoPlaybackService: VideoPlaybackService? = null
    private var isServiceConnected = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VideoPlaybackService.VideoPlaybackBinder
            videoPlaybackService = binder.getService()
            isServiceConnected = true
            Log.d(TAG, "Video playback service connected")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            videoPlaybackService = null
            isServiceConnected = false
            Log.d(TAG, "Video playback service disconnected")
        }
    }
    
    /**
     * Initialize the video manager and bind to playback service
     */
    fun initialize(context: Context) {
        if (!isServiceConnected) {
            val intent = Intent(context, VideoPlaybackService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    
    /**
     * Release resources and unbind from service
     */
    fun release(context: Context) {
        if (isServiceConnected) {
            try {
                context.unbindService(serviceConnection)
                isServiceConnected = false
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding service", e)
            }
        }
    }
    
    /**
     * Load video for a specific widget
     */
    fun loadVideoForWidget(context: Context, widgetId: Int, videoUri: String) {
        Log.d(TAG, "Loading video for widget $widgetId: $videoUri")
        
        try {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = VideoPlaybackService.ACTION_LOAD_VIDEO
                putExtra(VideoPlaybackService.EXTRA_WIDGET_ID, widgetId)
                putExtra(VideoPlaybackService.EXTRA_VIDEO_URI, videoUri)
            }
            context.startService(intent)
            
            // Update widget UI to show loading state
            updateWidgetLoadingState(context, widgetId, true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video for widget", e)
            handleVideoError(context, widgetId, "Failed to load video")
        }
    }
    
    /**
     * Play video in widget
     */
    fun playVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Playing video for widget: $widgetId")
        
        try {
            val videoUri = PreferenceUtils.getWidgetVideoUri(context, widgetId)
            
            if (videoUri.isNullOrEmpty()) {
                Log.w(TAG, "No video URI found for widget: $widgetId")
                handleVideoError(context, widgetId, "No video configured")
                return
            }
            
            // Ensure video is loaded first
            if (!isVideoLoadedForWidget(widgetId)) {
                loadVideoForWidget(context, widgetId, videoUri)
            }
            
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = VideoPlaybackService.ACTION_PLAY
                putExtra(VideoPlaybackService.EXTRA_WIDGET_ID, widgetId)
            }
            context.startService(intent)
            
            // Update widget UI immediately
            PreferenceUtils.setWidgetPlayState(context, widgetId, true)
            updateWidgetPlayState(context, widgetId, true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
            handleVideoError(context, widgetId, "Playback failed")
        }
    }
    
    /**
     * Pause video in widget
     */
    fun pauseVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Pausing video for widget: $widgetId")
        
        try {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = VideoPlaybackService.ACTION_PAUSE
                putExtra(VideoPlaybackService.EXTRA_WIDGET_ID, widgetId)
            }
            context.startService(intent)
            
            // Update widget UI immediately
            PreferenceUtils.setWidgetPlayState(context, widgetId, false)
            updateWidgetPlayState(context, widgetId, false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing video", e)
        }
    }
    
    /**
     * Stop video in widget
     */
    fun stopVideo(context: Context, widgetId: Int) {
        Log.d(TAG, "Stopping video for widget: $widgetId")
        
        try {
            val intent = Intent(context, VideoPlaybackService::class.java).apply {
                action = VideoPlaybackService.ACTION_STOP
                putExtra(VideoPlaybackService.EXTRA_WIDGET_ID, widgetId)
            }
            context.startService(intent)
            
            // Update widget UI
            PreferenceUtils.setWidgetPlayState(context, widgetId, false)
            updateWidgetPlayState(context, widgetId, false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video", e)
        }
    }
    
    /**
     * Toggle play/pause for widget
     */
    fun togglePlayPause(context: Context, widgetId: Int) {
        val isPlaying = PreferenceUtils.getWidgetPlayState(context, widgetId)
        
        if (isPlaying) {
            pauseVideo(context, widgetId)
        } else {
            playVideo(context, widgetId)
        }
    }
    
    /**
     * Check if video is loaded for widget
     */
    private fun isVideoLoadedForWidget(widgetId: Int): Boolean {
        return videoPlaybackService?.let { service ->
            // For now, we'll assume video needs to be loaded each time
            // In a more complex implementation, we could track loaded videos
            false
        } ?: false
    }
    
    /**
     * Update widget loading state
     */
    private fun updateWidgetLoadingState(context: Context, widgetId: Int, isLoading: Boolean) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.video_widget)
            
            // Show/hide loading indicator
            views.setViewVisibility(R.id.loading_indicator, 
                if (isLoading) android.view.View.VISIBLE else android.view.View.GONE)
            views.setViewVisibility(R.id.play_overlay, 
                if (isLoading) android.view.View.GONE else android.view.View.VISIBLE)
            
            appWidgetManager.updateAppWidget(widgetId, views)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget loading state", e)
        }
    }
    
    /**
     * Update widget play state UI
     */
    private fun updateWidgetPlayState(context: Context, widgetId: Int, isPlaying: Boolean) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            
            // Get the appropriate layout for this widget size
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val layoutId = VideoWidgetProvider.getWidgetLayout(options)
            val views = RemoteViews(context.packageName, layoutId)
            
            // Update play/pause button
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            val playPauseDesc = if (isPlaying) {
                context.getString(R.string.pause)
            } else {
                context.getString(R.string.play)
            }
            
            views.setImageViewResource(R.id.play_pause_button, playPauseIcon)
            views.setContentDescription(R.id.play_pause_button, playPauseDesc)
            
            // Hide/show play overlay
            if (hasView(layoutId, R.id.play_overlay)) {
                views.setViewVisibility(R.id.play_overlay, 
                    if (isPlaying) android.view.View.GONE else android.view.View.VISIBLE)
            }
            
            // Hide loading indicator
            if (hasView(layoutId, R.id.loading_indicator)) {
                views.setViewVisibility(R.id.loading_indicator, android.view.View.GONE)
            }
            
            appWidgetManager.updateAppWidget(widgetId, views)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget play state", e)
        }
    }
    
    /**
     * Handle video errors
     */
    private fun handleVideoError(context: Context, widgetId: Int, errorMessage: String) {
        Log.e(TAG, "Video error for widget $widgetId: $errorMessage")
        
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.video_widget)
            
            // Hide loading indicator
            views.setViewVisibility(R.id.loading_indicator, android.view.View.GONE)
            
            // Show error state - could set a different thumbnail or text
            views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
            views.setViewVisibility(R.id.play_overlay, android.view.View.VISIBLE)
            
            // Reset play state
            PreferenceUtils.setWidgetPlayState(context, widgetId, false)
            views.setImageViewResource(R.id.play_pause_button, R.drawable.ic_play)
            
            appWidgetManager.updateAppWidget(widgetId, views)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling video error", e)
        }
    }
    
    /**
     * Check if a layout contains a specific view
     */
    private fun hasView(layoutId: Int, viewId: Int): Boolean {
        return when (layoutId) {
            R.layout.video_widget_compact -> viewId in listOf(
                R.id.play_pause_button, R.id.video_thumbnail
            )
            R.layout.video_widget_large -> true // Large layout has all views
            else -> viewId in listOf(
                R.id.play_pause_button, R.id.video_thumbnail, R.id.play_overlay, R.id.loading_indicator
            )
        }
    }
    
    /**
     * Validate video URI
     */
    fun isValidVideoUri(context: Context, uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Invalid video URI: $uriString", e)
            false
        }
    }
    
    /**
     * Get video playback info
     */
    fun getPlaybackInfo(widgetId: Int): PlaybackInfo? {
        return videoPlaybackService?.let { service ->
            PlaybackInfo(
                isPlaying = service.isPlaying(),
                currentPosition = service.getCurrentPosition(),
                duration = service.getDuration(),
                playbackState = service.getPlaybackState()
            )
        }
    }
    
    data class PlaybackInfo(
        val isPlaying: Boolean,
        val currentPosition: Long,
        val duration: Long,
        val playbackState: Int
    )
}
