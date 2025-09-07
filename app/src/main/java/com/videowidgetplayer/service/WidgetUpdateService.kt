package com.videowidgetplayer.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.videowidgetplayer.data.SelectedVideosManager
import com.videowidgetplayer.utils.WidgetPreferences
import com.videowidgetplayer.widget.VideoWidgetProvider

/**
 * Enhanced service for managing widget auto-refresh and video cycling
 * Now works with selected videos from homescreen
 */
class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnables = mutableMapOf<Int, Runnable>()
    
    companion object {
        const val EXTRA_APPWIDGET_ID = VideoWidgetProvider.EXTRA_APPWIDGET_ID
        private const val TAG = "WidgetUpdateService"
        private const val DEFAULT_CYCLE_INTERVAL = 15_000L // 15 seconds between video cycles
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
            
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            startVideoRotation(appWidgetId)
        }
        
        return START_STICKY
    }

    private fun startVideoRotation(appWidgetId: Int) {
        // Cancel any existing rotation for this widget
        stopVideoRotation(appWidgetId)
        
        val widgetPrefs = WidgetPreferences(this)
        val selectedVideosManager = SelectedVideosManager(this)
        
        // Get refresh interval from preferences, default to 15 seconds
        val refreshInterval = widgetPrefs.getAutoRefreshInterval(appWidgetId).takeIf { it > 0 } 
            ?: DEFAULT_CYCLE_INTERVAL
        
        val updateRunnable = object : Runnable {
            override fun run() {
                try {
                    // Only cycle if widget is currently playing
                    if (widgetPrefs.getPlayingState(appWidgetId)) {
                        val selectedVideos = selectedVideosManager.loadSelectedVideos()
                        
                        if (selectedVideos.isNotEmpty()) {
                            // Get current video index
                            val currentIndex = widgetPrefs.getCurrentVideoIndex(appWidgetId)
                            
                            // Move to next video in the list
                            val nextIndex = (currentIndex + 1) % selectedVideos.size
                            widgetPrefs.saveCurrentVideoIndex(appWidgetId, nextIndex)
                            
                            // Update widget to show new video
                            val appWidgetManager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                            VideoWidgetProvider.updateAppWidget(this@WidgetUpdateService, appWidgetManager, appWidgetId)
                            
                            Log.d(TAG, "Cycled widget $appWidgetId to video $nextIndex/${selectedVideos.size}")
                        }
                    }
                    
                    // Schedule next cycle
                    handler.postDelayed(this, refreshInterval)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error during video rotation for widget $appWidgetId", e)
                    // Continue anyway
                    handler.postDelayed(this, refreshInterval)
                }
            }
        }
        
        updateRunnables[appWidgetId] = updateRunnable
        handler.postDelayed(updateRunnable, refreshInterval)
        
        Log.d(TAG, "Started video rotation for widget $appWidgetId with interval ${refreshInterval}ms")
    }

    private fun stopVideoRotation(appWidgetId: Int) {
        updateRunnables[appWidgetId]?.let { runnable ->
            handler.removeCallbacks(runnable)
            updateRunnables.remove(appWidgetId)
            Log.d(TAG, "Stopped video rotation for widget $appWidgetId")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed, stopping all video rotations")
        // Cancel all pending updates
        updateRunnables.values.forEach { runnable ->
            handler.removeCallbacks(runnable)
        }
        updateRunnables.clear()
    }
}
