package com.videowidgetplayer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.videowidgetplayer.R
import com.videowidgetplayer.ui.MainActivity
import com.videowidgetplayer.utils.PreferenceUtils

class VideoWidgetProvider : AppWidgetProvider() {
    
    companion object {
        private const val TAG = "VideoWidgetProvider"
        
        // Custom action constants
        const val ACTION_WIDGET_PLAY = "com.videowidgetplayer.action.WIDGET_PLAY"
        const val ACTION_WIDGET_PAUSE = "com.videowidgetplayer.action.WIDGET_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.videowidgetplayer.action.WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "com.videowidgetplayer.action.WIDGET_PREVIOUS"
        const val ACTION_WIDGET_REWIND = "com.videowidgetplayer.action.WIDGET_REWIND"
        const val ACTION_WIDGET_FORWARD = "com.videowidgetplayer.action.WIDGET_FORWARD"
        const val ACTION_WIDGET_MUTE = "com.videowidgetplayer.action.WIDGET_MUTE"
        const val ACTION_WIDGET_UNMUTE = "com.videowidgetplayer.action.WIDGET_UNMUTE"
        const val ACTION_WIDGET_SHUFFLE = "com.videowidgetplayer.action.WIDGET_SHUFFLE"
        const val ACTION_WIDGET_LOOP = "com.videowidgetplayer.action.WIDGET_LOOP"
        const val ACTION_WIDGET_CONFIGURE = "com.videowidgetplayer.action.WIDGET_CONFIGURE"
        
        // Widget size thresholds
        private const val COMPACT_WIDTH_THRESHOLD = 250
        private const val LARGE_WIDTH_THRESHOLD = 320
        
        /**
         * Update a specific widget instance
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            Log.d(TAG, "=== WIDGET UPDATE DEBUG START ===")
            Log.d(TAG, "Updating widget: $appWidgetId")
            
            try {
                // Get widget options to determine size
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widgetLayout = getWidgetLayout(options)
                Log.d(TAG, "Widget layout selected: $widgetLayout")
                
                // Create RemoteViews object with appropriate layout
                val views = RemoteViews(context.packageName, widgetLayout)
                Log.d(TAG, "Created RemoteViews for package: ${context.packageName}")
                
                // Load widget preferences
                val videoQueue = PreferenceUtils.getWidgetVideoQueue(context, appWidgetId)
                val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, appWidgetId)
                Log.d(TAG, "Video queue size: ${videoQueue.size}, current index: $currentIndex")
                
                val videoUri = if (videoQueue.isNotEmpty() && currentIndex < videoQueue.size) {
                    videoQueue[currentIndex]
                } else {
                    // Fallback to app-selected videos if no widget-specific videos found
                    val appVideos = PreferenceUtils.getAppSelectedVideos(context)
                    if (appVideos.isNotEmpty()) {
                        Log.d(TAG, "Using app-selected videos as fallback")
                        // Set up the widget with app-selected videos
                        PreferenceUtils.setWidgetVideoQueue(context, appWidgetId, appVideos)
                        PreferenceUtils.setWidgetCurrentVideoIndex(context, appWidgetId, 0)
                        appVideos[0]
                    } else {
                        null
                    }
                }
                
                Log.d(TAG, "Selected video URI: $videoUri")
                
                val isPlaying = PreferenceUtils.getWidgetPlayState(context, appWidgetId)
                Log.d(TAG, "Is playing: $isPlaying")
                
                // Also save the current video URI for backward compatibility
                if (videoUri != null) {
                    PreferenceUtils.saveWidgetVideoUri(context, appWidgetId, videoUri)
                    Log.d(TAG, "Saved video URI for backward compatibility")
                } else {
                    Log.w(TAG, "NO VIDEO URI FOUND - this may cause 'Can't load widget'")
                }
                
                // Update widget content
                Log.d(TAG, "Updating widget content...")
                updateWidgetContent(context, views, appWidgetId, videoUri, isPlaying, widgetLayout)
                
                // Set up click listeners
                Log.d(TAG, "Setting up click listeners...")
                setupClickListeners(context, views, appWidgetId, widgetLayout)
                
                // Update the widget
                Log.d(TAG, "Applying widget update...")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                Log.d(TAG, "Widget update completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "CRITICAL ERROR updating widget $appWidgetId", e)
                
                // Create emergency fallback widget
                try {
                    val fallbackViews = RemoteViews(context.packageName, R.layout.video_widget)
                    fallbackViews.setTextViewText(R.id.widget_title, "Widget Error - Check Logs")
                    fallbackViews.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
                    appWidgetManager.updateAppWidget(appWidgetId, fallbackViews)
                    Log.d(TAG, "Applied emergency fallback widget")
                } catch (fe: Exception) {
                    Log.e(TAG, "Even fallback widget failed!", fe)
                }
            }
            Log.d(TAG, "=== WIDGET UPDATE DEBUG END ===")
        }
        
        /**
         * Determine which layout to use based on widget size
         */
        fun getWidgetLayout(options: Bundle?): Int {
            if (options == null) return R.layout.video_widget
            
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            
            return when {
                minWidth >= LARGE_WIDTH_THRESHOLD && minHeight >= 250 -> R.layout.video_widget_large
                minWidth < COMPACT_WIDTH_THRESHOLD -> R.layout.video_widget_compact
                else -> R.layout.video_widget
            }
        }
        
        /**
         * Update widget content based on video data and state
         */
        private fun updateWidgetContent(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            videoUri: String?,
            isPlaying: Boolean,
            layoutId: Int
        ) {
            Log.d(TAG, "=== CONTENT UPDATE DEBUG ===")
            
            // Update title
            val title = if (videoUri != null) {
                val videoTitle = getVideoTitle(context, videoUri)
                Log.d(TAG, "Video title extracted: $videoTitle")
                videoTitle
            } else {
                val defaultTitle = context.getString(R.string.video_widget_name)
                Log.d(TAG, "Using default title: $defaultTitle")
                defaultTitle
            }
            
            try {
                views.setTextViewText(R.id.widget_title, title)
                Log.d(TAG, "Set widget title successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting widget title", e)
            }
            
            // Update play/pause button
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            val playPauseDesc = if (isPlaying) {
                context.getString(R.string.pause)
            } else {
                context.getString(R.string.play)
            }
            
            try {
                views.setImageViewResource(R.id.play_pause_button, playPauseIcon)
                views.setContentDescription(R.id.play_pause_button, playPauseDesc)
                Log.d(TAG, "Set play/pause button successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting play/pause button", e)
            }
            
            // Update mute button
            try {
                updateMuteButton(context, views, appWidgetId, layoutId)
                Log.d(TAG, "Updated mute button successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating mute button", e)
            }
            
            // Update video thumbnail
            if (videoUri != null) {
                Log.d(TAG, "Loading video thumbnail for URI: $videoUri")
                try {
                    Log.d(TAG, "Loading video thumbnail for URI: $videoUri")
                    loadVideoThumbnail(context, views, videoUri)
                    views.setViewVisibility(R.id.play_overlay, 
                        if (isPlaying) android.view.View.GONE else android.view.View.VISIBLE)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading video thumbnail, using placeholder", e)
                    views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
                }
            } else {
                Log.d(TAG, "No video URI, using placeholder")
                views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
                views.setViewVisibility(R.id.play_overlay, android.view.View.VISIBLE)
            }
            
            // Update layout-specific elements
            when (layoutId) {
                R.layout.video_widget_large -> {
                    Log.d(TAG, "Updating large widget content")
                    updateLargeWidgetContent(context, views, appWidgetId, videoUri)
                }
                R.layout.video_widget_compact -> {
                    Log.d(TAG, "Updating compact widget content")
                    updateCompactWidgetContent(context, views, appWidgetId)
                }
                else -> {
                    Log.d(TAG, "Using standard widget layout")
                }
            }
            Log.d(TAG, "=== CONTENT UPDATE COMPLETE ===")
        }
        
        /**
         * Update content specific to large widget layout
         */
        private fun updateLargeWidgetContent(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            videoUri: String?
        ) {
            // Update video title in overlay
            if (videoUri != null) {
                val videoTitle = getVideoTitle(context, videoUri)
                views.setTextViewText(R.id.video_title, videoTitle)
            }
        }
        
        /**
         * Update content specific to compact widget layout
         */
        private fun updateCompactWidgetContent(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int
        ) {
            // Compact layout has minimal content
            // Most content is handled in the main update function
        }
        
        /**
         * Update mute button appearance and state
         */
        private fun updateMuteButton(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            layoutId: Int
        ) {
            // Check if mute button exists in this layout
            if (!hasView(layoutId, R.id.mute_button)) return
            
            val isMuted = PreferenceUtils.getWidgetMuteState(context, appWidgetId)
            
            val muteIcon = if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            val muteDesc = if (isMuted) {
                context.getString(R.string.unmute)
            } else {
                context.getString(R.string.mute)
            }
            
            views.setImageViewResource(R.id.mute_button, muteIcon)
            views.setContentDescription(R.id.mute_button, muteDesc)
        }
        
        /**
         * Set up click listeners for widget buttons
         */
        private fun setupClickListeners(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            layoutId: Int
        ) {
            // Main video area click - open configuration or start playback
            val mainIntent = Intent(context, VideoWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.video_container, mainPendingIntent)
            
            // Play/Pause button
            setupButtonClick(context, views, appWidgetId, R.id.play_pause_button, ACTION_WIDGET_PLAY)
            
            // Navigation buttons (if present in layout)
            if (hasView(layoutId, R.id.previous_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.previous_button, ACTION_WIDGET_PREVIOUS)
            }
            if (hasView(layoutId, R.id.next_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.next_button, ACTION_WIDGET_NEXT)
            }
            if (hasView(layoutId, R.id.rewind_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.rewind_button, ACTION_WIDGET_REWIND)
            }
            if (hasView(layoutId, R.id.forward_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.forward_button, ACTION_WIDGET_FORWARD)
            }
            
            // Mute button (if present)
            if (hasView(layoutId, R.id.mute_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.mute_button, ACTION_WIDGET_MUTE)
            }
            
            // Shuffle button (if present)
            if (hasView(layoutId, R.id.shuffle_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.shuffle_button, ACTION_WIDGET_SHUFFLE)
            }
            
            // Loop button (if present)
            if (hasView(layoutId, R.id.loop_button)) {
                setupButtonClick(context, views, appWidgetId, R.id.loop_button, ACTION_WIDGET_LOOP)
            }
            
            // Settings button (if present)
            if (hasView(layoutId, R.id.widget_settings)) {
                setupButtonClick(context, views, appWidgetId, R.id.widget_settings, ACTION_WIDGET_CONFIGURE)
            }
        }
        
        /**
         * Set up click listener for a specific button
         */
        private fun setupButtonClick(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            buttonId: Int,
            action: String
        ) {
            val intent = Intent(context, VideoWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                appWidgetId * 1000 + buttonId, // Unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(buttonId, pendingIntent)
        }
        
        /**
         * Check if a layout contains a specific view
         */
        private fun hasView(layoutId: Int, viewId: Int): Boolean {
            return when (layoutId) {
                R.layout.video_widget_compact -> viewId in listOf(
                    R.id.previous_button, R.id.play_pause_button, R.id.next_button, R.id.mute_button
                )
                R.layout.video_widget_large -> true // Large layout has all views
                else -> viewId in listOf(
                    R.id.play_pause_button, R.id.previous_button, R.id.next_button,
                    R.id.rewind_button, R.id.forward_button, R.id.mute_button, R.id.widget_settings
                )
            }
        }
        
        /**
         * Load video thumbnail using MediaMetadataRetriever
         */
        private fun loadVideoThumbnail(context: Context, views: RemoteViews, videoUri: String) {
            try {
                Log.d(TAG, "Loading thumbnail for URI: $videoUri")
                val retriever = MediaMetadataRetriever()
                var thumbnail: Bitmap? = null
                
                try {
                    retriever.setDataSource(context, Uri.parse(videoUri))
                    
                    // Try to get frame at 1 second, if that fails get first frame
                    thumbnail = try {
                        retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (e: Exception) {
                        Log.d(TAG, "Couldn't get frame at 1s, trying first frame")
                        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    
                    if (thumbnail != null) {
                        // Scale down the thumbnail to reasonable size for widget
                        val scaledThumbnail = Bitmap.createScaledBitmap(
                            thumbnail, 
                            minOf(thumbnail.width, 300), 
                            minOf(thumbnail.height, 200), 
                            true
                        )
                        
                        views.setImageViewBitmap(R.id.video_thumbnail, scaledThumbnail)
                        Log.d(TAG, "Successfully loaded video thumbnail (${scaledThumbnail.width}x${scaledThumbnail.height})")
                        
                        // Don't recycle original if it's different from scaled
                        if (scaledThumbnail != thumbnail) {
                            thumbnail.recycle()
                        }
                    } else {
                        Log.w(TAG, "Retrieved thumbnail is null, using placeholder")
                        views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
                    }
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing MediaMetadataRetriever", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading thumbnail for $videoUri", e)
                views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
            }
        }
        
        /**
         * Get video title from URI (placeholder implementation)
         */
        private fun getVideoTitle(context: Context, videoUri: String): String {
            return try {
                // Extract filename from URI
                val uri = Uri.parse(videoUri)
                uri.lastPathSegment?.substringBeforeLast(".") ?: "Unknown Video"
            } catch (e: Exception) {
                "Video"
            }
        }
        
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        Log.d(TAG, "Widget $appWidgetId options changed")
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val action = intent.action
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
            AppWidgetManager.INVALID_APPWIDGET_ID)
        
        Log.d(TAG, "Received action: $action for widget: $appWidgetId")
        
        when (action) {
            ACTION_WIDGET_PLAY, ACTION_WIDGET_PAUSE -> {
                handlePlayPause(context, appWidgetId)
            }
            ACTION_WIDGET_NEXT -> {
                handleNext(context, appWidgetId)
            }
            ACTION_WIDGET_PREVIOUS -> {
                handlePrevious(context, appWidgetId)
            }
            ACTION_WIDGET_REWIND -> {
                handleRewind(context, appWidgetId)
            }
            ACTION_WIDGET_FORWARD -> {
                handleForward(context, appWidgetId)
            }
            ACTION_WIDGET_MUTE, ACTION_WIDGET_UNMUTE -> {
                handleMuteToggle(context, appWidgetId)
            }
            ACTION_WIDGET_SHUFFLE -> {
                handleShuffle(context, appWidgetId)
            }
            ACTION_WIDGET_LOOP -> {
                handleLoop(context, appWidgetId)
            }
            ACTION_WIDGET_CONFIGURE -> {
                handleConfigure(context, appWidgetId)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        Log.d(TAG, "First widget added")
        
        // Initialize WidgetVideoManager
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, 0) // Use 0 as placeholder for general initialization
        
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        Log.d(TAG, "Last widget removed")
        
        // Release WidgetVideoManager resources
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.release(context, 0) // Use 0 as placeholder
        
        super.onDisabled(context)
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "Widgets deleted: ${appWidgetIds.contentToString()}")
        
        // Clean up preferences for deleted widgets
        for (appWidgetId in appWidgetIds) {
            PreferenceUtils.deleteWidgetPreferences(context, appWidgetId)
        }
        
        super.onDeleted(context, appWidgetIds)
    }
    
    /**
     * Handle play/pause action
     */
    private fun handlePlayPause(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        Log.d(TAG, "Handling play/pause for widget: $appWidgetId")
        
        // Use WidgetVideoManager to handle playback
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.togglePlayPause(context, appWidgetId)
    }
    
    /**
     * Handle next video action
     */
    private fun handleNext(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // Use WidgetVideoManager for next video functionality
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.nextVideo(context, appWidgetId)
        Log.d(TAG, "Next video requested for widget: $appWidgetId")
    }
    
    /**
     * Handle previous video action
     */
    private fun handlePrevious(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // Use WidgetVideoManager for previous video functionality
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.previousVideo(context, appWidgetId)
        Log.d(TAG, "Previous video requested for widget: $appWidgetId")
    }
    
    /**
     * Handle rewind action
     */
    private fun handleRewind(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // Use WidgetVideoManager for rewind seek
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        // TODO: Add seek rewind functionality to WidgetVideoManager
        Log.d(TAG, "Rewind requested for widget: $appWidgetId")
    }
    
    /**
     * Handle fast forward action
     */
    private fun handleForward(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // Use WidgetVideoManager for forward seek
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        // TODO: Add seek forward functionality to WidgetVideoManager
        Log.d(TAG, "Fast forward requested for widget: $appWidgetId")
    }
    
    /**
     * Handle mute/unmute toggle action
     */
    private fun handleMuteToggle(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        Log.d(TAG, "Handling mute toggle for widget: $appWidgetId")
        
        // Toggle mute state
        val currentMuteState = PreferenceUtils.getWidgetMuteState(context, appWidgetId)
        val newMuteState = !currentMuteState
        PreferenceUtils.setWidgetMuteState(context, appWidgetId, newMuteState)
        
        // Update video manager volume
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.setVolume(context, appWidgetId, if (newMuteState) 0f else 1f)
        
        // Update widget to reflect new mute state
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
    
    /**
     * Handle shuffle toggle action
     */
    private fun handleShuffle(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        Log.d(TAG, "Handling shuffle toggle for widget: $appWidgetId")
        
        // Use WidgetVideoManager for shuffle functionality
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.toggleShuffle(context, appWidgetId)
        
        // Update widget to reflect new shuffle state
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
    
    /**
     * Handle loop mode cycle action
     */
    private fun handleLoop(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        Log.d(TAG, "Handling loop cycle for widget: $appWidgetId")
        
        // Use WidgetVideoManager for loop functionality
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context, appWidgetId)
        videoManager.cycleLoopMode(context, appWidgetId)
        
        // Update widget to reflect new loop state
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }
    
    /**
     * Handle configure action
     */
    private fun handleConfigure(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        val intent = Intent(context, VideoWidgetConfigureActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
