package com.videowidgetplayer.service

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.videowidgetplayer.utils.WidgetPreferences
import com.videowidgetplayer.widget.VideoWidgetProvider

/**
 * Service for managing widget auto-refresh and periodic video rotation
 * Following the spec: Auto-refresh with configurable timing
 */
class WidgetUpdateService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnables = mutableMapOf<Int, Runnable>()
    
    companion object {
        const val EXTRA_APPWIDGET_ID = VideoWidgetProvider.EXTRA_APPWIDGET_ID
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra(EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
            
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            startAutoRefresh(appWidgetId)
        }
        
        return START_STICKY
    }

    private fun startAutoRefresh(appWidgetId: Int) {
        // Cancel any existing refresh for this widget
        stopAutoRefresh(appWidgetId)
        
        val widgetPrefs = WidgetPreferences(this)
        val refreshInterval = widgetPrefs.getAutoRefreshInterval(appWidgetId)
        
        val updateRunnable = object : Runnable {
            override fun run() {
                if (widgetPrefs.getPlayingState(appWidgetId)) {
                    // Get next random video
                    widgetPrefs.getNextRandomVideoIndex(appWidgetId)
                    
                    // Update widget
                    val appWidgetManager = AppWidgetManager.getInstance(this@WidgetUpdateService)
                    VideoWidgetProvider.updateAppWidget(this@WidgetUpdateService, appWidgetManager, appWidgetId)
                }
                
                // Schedule next update
                handler.postDelayed(this, refreshInterval)
            }
        }
        
        updateRunnables[appWidgetId] = updateRunnable
        handler.postDelayed(updateRunnable, refreshInterval)
    }

    private fun stopAutoRefresh(appWidgetId: Int) {
        updateRunnables[appWidgetId]?.let { runnable ->
            handler.removeCallbacks(runnable)
            updateRunnables.remove(appWidgetId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending updates
        updateRunnables.values.forEach { runnable ->
            handler.removeCallbacks(runnable)
        }
        updateRunnables.clear()
    }
}
