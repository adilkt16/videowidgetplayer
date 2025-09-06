package com.videowidgetplayer.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
            Log.d(TAG, "Updating widget: $appWidgetId")
            
            try {
                // Get widget options to determine size
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val widgetLayout = getWidgetLayout(options)
                
                // Create RemoteViews object with appropriate layout
                val views = RemoteViews(context.packageName, widgetLayout)
                
                // Load widget preferences
                val videoUri = PreferenceUtils.getWidgetVideoUri(context, appWidgetId)
                val isPlaying = PreferenceUtils.getWidgetPlayState(context, appWidgetId)
                
                // Update widget content
                updateWidgetContent(context, views, appWidgetId, videoUri, isPlaying, widgetLayout)
                
                // Set up click listeners
                setupClickListeners(context, views, appWidgetId, widgetLayout)
                
                // Update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget $appWidgetId", e)
            }
        }
        
        /**
         * Determine which layout to use based on widget size
         */
        private fun getWidgetLayout(options: Bundle?): Int {
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
            // Update title
            val title = if (videoUri != null) {
                getVideoTitle(context, videoUri)
            } else {
                context.getString(R.string.video_widget_name)
            }
            views.setTextViewText(R.id.widget_title, title)
            
            // Update play/pause button
            val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            val playPauseDesc = if (isPlaying) {
                context.getString(R.string.pause)
            } else {
                context.getString(R.string.play)
            }
            
            views.setImageViewResource(R.id.play_pause_button, playPauseIcon)
            views.setContentDescription(R.id.play_pause_button, playPauseDesc)
            
            // Update video thumbnail
            if (videoUri != null) {
                loadVideoThumbnail(context, views, videoUri)
                views.setViewVisibility(R.id.play_overlay, 
                    if (isPlaying) android.view.View.GONE else android.view.View.VISIBLE)
            } else {
                views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
                views.setViewVisibility(R.id.play_overlay, android.view.View.VISIBLE)
            }
            
            // Update layout-specific elements
            when (layoutId) {
                R.layout.video_widget_large -> {
                    updateLargeWidgetContent(context, views, appWidgetId, videoUri)
                }
                R.layout.video_widget_compact -> {
                    updateCompactWidgetContent(context, views, appWidgetId)
                }
            }
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
            // Update video duration
            val duration = if (videoUri != null) {
                getVideoDuration(context, videoUri)
            } else "0:00"
            views.setTextViewText(R.id.video_duration, duration)
            
            // Update progress bar (placeholder values)
            views.setProgressBar(R.id.progress_bar, 100, 0, false)
            views.setTextViewText(R.id.current_time, "00:00")
            views.setTextViewText(R.id.total_time, duration)
            
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
                    R.id.previous_button, R.id.play_pause_button, R.id.next_button
                )
                R.layout.video_widget_large -> true // Large layout has all views
                else -> viewId in listOf(
                    R.id.play_pause_button, R.id.previous_button, R.id.next_button,
                    R.id.rewind_button, R.id.forward_button, R.id.widget_settings
                )
            }
        }
        
        /**
         * Load video thumbnail (placeholder implementation)
         */
        private fun loadVideoThumbnail(context: Context, views: RemoteViews, videoUri: String) {
            try {
                // This is a placeholder - in a real implementation, you would:
                // 1. Use MediaMetadataRetriever to extract thumbnail
                // 2. Load thumbnail asynchronously
                // 3. Cache thumbnails for performance
                views.setImageViewResource(R.id.video_thumbnail, R.drawable.ic_video_placeholder)
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
        
        /**
         * Get video duration (placeholder implementation)
         */
        private fun getVideoDuration(context: Context, videoUri: String): String {
            return try {
                // This would use MediaMetadataRetriever in a real implementation
                "0:00"
            } catch (e: Exception) {
                "0:00"
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
            ACTION_WIDGET_CONFIGURE -> {
                handleConfigure(context, appWidgetId)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        Log.d(TAG, "First widget added")
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        Log.d(TAG, "Last widget removed")
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
        
        val isCurrentlyPlaying = PreferenceUtils.getWidgetPlayState(context, appWidgetId)
        val newPlayState = !isCurrentlyPlaying
        
        PreferenceUtils.setWidgetPlayState(context, appWidgetId, newPlayState)
        
        // Update widget to reflect new state
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)
        
        // TODO: Integrate with actual video player
        Log.d(TAG, "Widget $appWidgetId play state changed to: $newPlayState")
    }
    
    /**
     * Handle next video action
     */
    private fun handleNext(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // TODO: Implement next video logic
        Log.d(TAG, "Next video requested for widget: $appWidgetId")
    }
    
    /**
     * Handle previous video action
     */
    private fun handlePrevious(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // TODO: Implement previous video logic
        Log.d(TAG, "Previous video requested for widget: $appWidgetId")
    }
    
    /**
     * Handle rewind action
     */
    private fun handleRewind(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // TODO: Implement rewind logic
        Log.d(TAG, "Rewind requested for widget: $appWidgetId")
    }
    
    /**
     * Handle fast forward action
     */
    private fun handleForward(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        
        // TODO: Implement fast forward logic
        Log.d(TAG, "Fast forward requested for widget: $appWidgetId")
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
