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
import com.videowidgetplayer.utils.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Video Widget Provider
 * Following the spec: Widget-first design with video playback controls
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
            val videoUris = widgetPrefs.getVideoUris(appWidgetId)
            
            if (videoUris.isEmpty()) {
                updateUnconfiguredWidget(context, appWidgetManager, appWidgetId)
                return
            }

            val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
            val isPlaying = widgetPrefs.getPlayingState(appWidgetId)
            val isMuted = widgetPrefs.getMutedState(appWidgetId)
            
            val currentVideoUri = if (currentIndex < videoUris.size) {
                videoUris[currentIndex]
            } else {
                videoUris.firstOrNull()
            }

            if (currentVideoUri != null) {
                updateConfiguredWidget(
                    context, 
                    appWidgetManager, 
                    appWidgetId, 
                    currentVideoUri, 
                    isPlaying, 
                    isMuted
                )
            } else {
                updateUnconfiguredWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateConfiguredWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            videoUri: Uri,
            isPlaying: Boolean,
            isMuted: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.video_widget_layout)

            // Set button click handlers
            setupButtonActions(context, views, appWidgetId)

            // Update play/pause button icon
            views.setImageViewResource(
                R.id.playPauseButton,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )

            // Update mute button icon
            views.setImageViewResource(
                R.id.muteButton,
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_up
            )

            // Load video thumbnail asynchronously
            loadVideoThumbnail(context, views, appWidgetId, videoUri, appWidgetManager)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            
            // Start video update service if playing
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
            
            // Set up config intent
            val configIntent = Intent(context, VideoWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
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

            // Mute button
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
        val videoUris = widgetPrefs.getVideoUris(appWidgetId)
        if (videoUris.isEmpty()) return

        val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
        val nextIndex = (currentIndex + 1) % videoUris.size
        widgetPrefs.saveCurrentVideoIndex(appWidgetId, nextIndex)
        
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun navigateToPreviousVideo(
        context: Context,
        appWidgetId: Int,
        widgetPrefs: WidgetPreferences,
        appWidgetManager: AppWidgetManager
    ) {
        val videoUris = widgetPrefs.getVideoUris(appWidgetId)
        if (videoUris.isEmpty()) return

        val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
        val previousIndex = if (currentIndex > 0) currentIndex - 1 else videoUris.size - 1
        widgetPrefs.saveCurrentVideoIndex(appWidgetId, previousIndex)
        
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val widgetPrefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            widgetPrefs.deleteWidgetPrefs(appWidgetId)
        }
    }
}
