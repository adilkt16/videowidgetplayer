package com.videowidgetplayer.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.videowidgetplayer.widgets.WidgetVideoManager

/**
 * Broadcast receiver for handling gesture-triggered actions
 */
class WidgetGestureReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "WidgetGestureReceiver"
        const val ACTION_GESTURE_NEXT = "com.videowidgetplayer.action.GESTURE_next"
        const val ACTION_GESTURE_PREVIOUS = "com.videowidgetplayer.action.GESTURE_previous"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val widgetId = intent.getIntExtra("widget_id", -1)
        if (widgetId == -1) {
            Log.w(TAG, "Invalid widget ID in gesture action")
            return
        }
        
        // Extract gesture data if available
        val gestureVelocity = intent.getFloatExtra("gesture_velocity", 0f)
        val gestureDistance = intent.getFloatExtra("gesture_distance", 0f)
        val gestureConfidence = intent.getFloatExtra("gesture_confidence", 1f)
        
        Log.d(TAG, "Received gesture action: ${intent.action} for widget $widgetId " +
                "(velocity=$gestureVelocity, distance=$gestureDistance, confidence=$gestureConfidence)")
        
        val videoManager = WidgetVideoManager.getInstance()
        videoManager.initialize(context)
        
        when (intent.action) {
            ACTION_GESTURE_NEXT -> {
                Log.d(TAG, "Processing swipe left gesture - next video")
                videoManager.nextVideoWithGesture(context, widgetId, gestureVelocity, true)
            }
            ACTION_GESTURE_PREVIOUS -> {
                Log.d(TAG, "Processing swipe right gesture - previous video")
                videoManager.previousVideoWithGesture(context, widgetId, gestureVelocity, true)
            }
            else -> {
                Log.w(TAG, "Unknown gesture action: ${intent.action}")
            }
        }
    }
}
