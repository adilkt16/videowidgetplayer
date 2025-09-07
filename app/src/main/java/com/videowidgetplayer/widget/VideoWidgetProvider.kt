package com.videowidgetplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.transition.Transition
import com.videowidgetplayer.R
import com.videowidgetplayer.data.VideoRepository
import com.videowidgetplayer.service.WidgetUpdateService
import com.videowidgetplayer.service.WidgetVideoService
import com.videowidgetplayer.utils.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Video Widget Provider
 * Enhanced to display selected videos from homescreen and enable muted video playback
 */
class VideoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.videowidgetplayer.PLAY_PAUSE"
        const val ACTION_MUTE_UNMUTE = "com.videowidgetplayer.MUTE_UNMUTE"
        const val ACTION_NEXT_VIDEO = "com.videowidgetplayer.NEXT_VIDEO"
        const val ACTION_PREVIOUS_VIDEO = "com.videowidgetplayer.PREVIOUS_VIDEO"
        const val ACTION_SWIPE_LEFT = "com.videowidgetplayer.SWIPE_LEFT"
        const val ACTION_SWIPE_RIGHT = "com.videowidgetplayer.SWIPE_RIGHT"
        const val EXTRA_APPWIDGET_ID = "appwidget_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val widgetPrefs = WidgetPreferences(context)
            
            // Get selected videos from homescreen
            val selectedVideosManager = com.videowidgetplayer.data.SelectedVideosManager(context)
            val selectedVideos = selectedVideosManager.loadSelectedVideos()
            
            if (selectedVideos.isEmpty()) {
                updateUnconfiguredWidget(context, appWidgetManager, appWidgetId)
                return
            }

            // Save selected videos to widget preferences for consistency
            val videoUris = selectedVideos.map { it.uri }
            widgetPrefs.saveVideoUris(appWidgetId, videoUris)
            
            // Get current video from selected list
            val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
            val adjustedIndex = if (currentIndex < selectedVideos.size) currentIndex else 0
            
            if (adjustedIndex != currentIndex) {
                widgetPrefs.saveCurrentVideoIndex(appWidgetId, adjustedIndex)
            }
            
            val currentVideo = selectedVideos[adjustedIndex]
            val isPlaying = widgetPrefs.getPlayingState(appWidgetId)
            val isMuted = widgetPrefs.getMutedState(appWidgetId)
            
            updateConfiguredWidget(
                context, 
                appWidgetManager, 
                appWidgetId, 
                currentVideo.uri, 
                isPlaying, 
                isMuted,
                currentVideo.name,
                selectedVideos.size,
                adjustedIndex + 1
            )
        }

        private fun updateConfiguredWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            videoUri: Uri,
            isPlaying: Boolean,
            isMuted: Boolean,
            videoName: String,
            totalVideos: Int,
            currentPosition: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.video_widget_layout)

            // Set button click handlers
            setupButtonActions(context, views, appWidgetId)

            // Update play/pause button icon
            views.setImageViewResource(
                R.id.playPauseButton,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            // Update mute button icon (always show muted since widget videos are always muted)
            views.setImageViewResource(
                R.id.muteButton,
                R.drawable.ic_volume_off
            )

            // Update video info
            views.setTextViewText(R.id.videoTitle, videoName)
            views.setTextViewText(R.id.videoCounter, "$currentPosition/$totalVideos")
            views.setViewVisibility(R.id.videoInfoOverlay, android.view.View.VISIBLE)

            // Handle video playback
            if (isPlaying) {
                // Start frame-based video animation (muted)
                startMutedVideoPlayback(context, appWidgetId, videoUri)
                // Show video display, hide thumbnail
                views.setViewVisibility(R.id.videoThumbnail, android.view.View.GONE)
                views.setViewVisibility(R.id.videoDisplay, android.view.View.VISIBLE)
            } else {
                // Stop video playback and show thumbnail
                stopVideoPlayback(context, appWidgetId)
                views.setViewVisibility(R.id.videoDisplay, android.view.View.GONE)
                views.setViewVisibility(R.id.videoThumbnail, android.view.View.VISIBLE)
                
                // Load video thumbnail
                loadVideoThumbnail(context, views, appWidgetId, videoUri, appWidgetManager)
            }

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Start auto-refresh service for cycling videos
            if (isPlaying) {
                startVideoUpdateService(context, appWidgetId)
            }
        }

        private fun updateUnconfiguredWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.video_widget_layout)
            
            // Show placeholder
            views.setImageViewResource(R.id.videoThumbnail, R.drawable.widget_preview)
            views.setViewVisibility(R.id.videoThumbnail, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.videoDisplay, android.view.View.GONE)
            views.setViewVisibility(R.id.videoInfoOverlay, android.view.View.GONE)
            
            // Set up config intent to open main activity for video selection
            val configIntent = Intent(context, com.videowidgetplayer.ui.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.videoThumbnail, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupButtonActions(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int
        ) {
            // Play/Pause button
            val playPauseIntent = Intent(context, VideoWidgetProvider::class.java).apply {
                action = ACTION_PLAY_PAUSE
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.playPauseButton, playPausePendingIntent)

            // Mute button (for widget videos, always muted, but can be used for future features)
            val muteIntent = Intent(context, VideoWidgetProvider::class.java).apply {
                action = ACTION_MUTE_UNMUTE
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val mutePendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 1000, muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.muteButton, mutePendingIntent)

            // Next button
            val nextIntent = Intent(context, VideoWidgetProvider::class.java).apply {
                action = ACTION_NEXT_VIDEO
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 2000, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.nextButton, nextPendingIntent)

            // Previous button
            val previousIntent = Intent(context, VideoWidgetProvider::class.java).apply {
                action = ACTION_PREVIOUS_VIDEO
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val previousPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 3000, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.previousButton, previousPendingIntent)
        }

        private fun startMutedVideoPlayback(context: Context, appWidgetId: Int, videoUri: Uri) {
            val intent = Intent(context, WidgetVideoService::class.java).apply {
                action = WidgetVideoService.ACTION_PLAY_VIDEO
                putExtra(WidgetVideoService.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(WidgetVideoService.EXTRA_VIDEO_URI, videoUri.toString())
            }
            context.startService(intent)
        }

        private fun stopVideoPlayback(context: Context, appWidgetId: Int) {
            val intent = Intent(context, WidgetVideoService::class.java).apply {
                action = WidgetVideoService.ACTION_STOP_VIDEO
                putExtra(WidgetVideoService.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            context.startService(intent)
        }

        private fun loadVideoThumbnail(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            videoUri: Uri,
            appWidgetManager: AppWidgetManager
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val appWidgetTarget = AppWidgetTarget(
                        context,
                        R.id.videoThumbnail,
                        views,
                        appWidgetId
                    )

                    Glide.with(context)
                        .asBitmap()
                        .load(videoUri)
                        .placeholder(R.drawable.widget_preview)
                        .error(R.drawable.widget_preview)
                        .centerCrop()
                        .into(appWidgetTarget)
                } catch (e: Exception) {
                    // Fallback to default preview
                    views.setImageViewResource(R.id.videoThumbnail, R.drawable.widget_preview)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun startVideoUpdateService(context: Context, appWidgetId: Int) {
            val serviceIntent = Intent(context, WidgetUpdateService::class.java).apply {
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            context.startService(serviceIntent)
        }
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val appWidgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val widgetPrefs = WidgetPreferences(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val isPlaying = widgetPrefs.getPlayingState(appWidgetId)
                widgetPrefs.savePlayingState(appWidgetId, !isPlaying)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            
            ACTION_MUTE_UNMUTE -> {
                // For widget videos, always muted, but toggle state for UI consistency
                val isMuted = widgetPrefs.getMutedState(appWidgetId)
                widgetPrefs.saveMutedState(appWidgetId, !isMuted)
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            
            ACTION_NEXT_VIDEO, ACTION_SWIPE_LEFT -> {
                navigateToNextVideo(context, appWidgetId, widgetPrefs, appWidgetManager)
            }
            
            ACTION_PREVIOUS_VIDEO, ACTION_SWIPE_RIGHT -> {
                navigateToPreviousVideo(context, appWidgetId, widgetPrefs, appWidgetManager)
            }
        }
    }

    private fun navigateToNextVideo(
        context: Context,
        appWidgetId: Int,
        widgetPrefs: WidgetPreferences,
        appWidgetManager: AppWidgetManager
    ) {
        // Get videos from homescreen selection
        val selectedVideosManager = com.videowidgetplayer.data.SelectedVideosManager(context)
        val selectedVideos = selectedVideosManager.loadSelectedVideos()
        if (selectedVideos.isEmpty()) return

        val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
        val nextIndex = (currentIndex + 1) % selectedVideos.size
        widgetPrefs.saveCurrentVideoIndex(appWidgetId, nextIndex)
        
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun navigateToPreviousVideo(
        context: Context,
        appWidgetId: Int,
        widgetPrefs: WidgetPreferences,
        appWidgetManager: AppWidgetManager
    ) {
        // Get videos from homescreen selection
        val selectedVideosManager = com.videowidgetplayer.data.SelectedVideosManager(context)
        val selectedVideos = selectedVideosManager.loadSelectedVideos()
        if (selectedVideos.isEmpty()) return

        val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
        val previousIndex = if (currentIndex > 0) currentIndex - 1 else selectedVideos.size - 1
        widgetPrefs.saveCurrentVideoIndex(appWidgetId, previousIndex)
        
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val widgetPrefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            // Stop video playback for deleted widgets
            stopVideoPlayback(context, appWidgetId)
            // Clear widget preferences
            widgetPrefs.deleteWidgetPrefs(appWidgetId)
        }
    }
}
