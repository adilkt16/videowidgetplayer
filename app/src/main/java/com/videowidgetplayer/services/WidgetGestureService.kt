package com.videowidgetplayer.services

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.videowidgetplayer.widgets.WidgetGestureManager
import kotlin.math.abs

/**
 * Service for detecting gestures on video widgets
 * Creates invisible overlay views for gesture detection
 */
class WidgetGestureService : Service() {
    
    companion object {
        private const val TAG = "WidgetGestureService"
        const val ACTION_START_GESTURE_DETECTION = "com.videowidgetplayer.action.START_GESTURE_DETECTION"
        const val ACTION_STOP_GESTURE_DETECTION = "com.videowidgetplayer.action.STOP_GESTURE_DETECTION"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_WIDGET_BOUNDS = "widget_bounds"
    }
    
    private lateinit var windowManager: WindowManager
    private val gestureManager = WidgetGestureManager.getInstance()
    private val activeOverlays = mutableMapOf<Int, GestureOverlay>()
    
    /**
     * Gesture overlay for a specific widget
     */
    private inner class GestureOverlay(
        private val widgetId: Int,
        private val bounds: WidgetBounds
    ) : FrameLayout(this@WidgetGestureService), View.OnTouchListener {
        
        private var startPoint: WidgetGestureManager.TouchPoint? = null
        private var isGestureActive = false
        
        init {
            setOnTouchListener(this)
            alpha = 0f // Completely transparent
        }
        
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (event == null || !gestureManager.isGestureEnabled(this@WidgetGestureService, widgetId)) {
                return false
            }
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startPoint = WidgetGestureManager.TouchPoint(
                        x = event.x,
                        y = event.y,
                        timestamp = System.currentTimeMillis()
                    )
                    isGestureActive = true
                    Log.d(TAG, "Gesture started for widget $widgetId at (${event.x}, ${event.y})")
                    return true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    if (isGestureActive && startPoint != null) {
                        val deltaX = abs(event.x - startPoint!!.x)
                        val deltaY = abs(event.y - startPoint!!.y)
                        
                        // Cancel if movement is too small or too vertical
                        if (deltaY > deltaX * 2) {
                            Log.d(TAG, "Gesture cancelled: too vertical")
                            isGestureActive = false
                            return false
                        }
                    }
                    return isGestureActive
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isGestureActive && startPoint != null) {
                        val endPoint = WidgetGestureManager.TouchPoint(
                            x = event.x,
                            y = event.y,
                            timestamp = System.currentTimeMillis()
                        )
                        
                        processGesture(startPoint!!, endPoint)
                    }
                    
                    startPoint = null
                    isGestureActive = false
                    return true
                }
            }
            
            return false
        }
        
        private fun processGesture(
            startPoint: WidgetGestureManager.TouchPoint,
            endPoint: WidgetGestureManager.TouchPoint
        ) {
            val gestureEvent = gestureManager.analyzeGesture(
                this@WidgetGestureService,
                widgetId,
                startPoint,
                endPoint
            )
            
            if (gestureEvent != null && !gestureManager.isGestureConflicting(this@WidgetGestureService, gestureEvent)) {
                handleGestureEvent(widgetId, gestureEvent)
            }
        }
    }
    
    /**
     * Widget bounds data
     */
    data class WidgetBounds(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        Log.d(TAG, "WidgetGestureService created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GESTURE_DETECTION -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    startGestureDetection(widgetId, intent)
                }
            }
            ACTION_STOP_GESTURE_DETECTION -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    stopGestureDetection(widgetId)
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startGestureDetection(widgetId: Int, intent: Intent) {
        try {
            // Remove existing overlay if present
            stopGestureDetection(widgetId)
            
            val bounds = extractWidgetBounds(intent)
            if (bounds == null) {
                Log.w(TAG, "No widget bounds provided for widget $widgetId")
                return
            }
            
            val overlay = GestureOverlay(widgetId, bounds)
            val layoutParams = createOverlayLayoutParams(bounds)
            
            windowManager.addView(overlay, layoutParams)
            activeOverlays[widgetId] = overlay
            
            Log.d(TAG, "Started gesture detection for widget $widgetId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting gesture detection for widget $widgetId", e)
        }
    }
    
    private fun stopGestureDetection(widgetId: Int) {
        activeOverlays[widgetId]?.let { overlay ->
            try {
                windowManager.removeView(overlay)
                activeOverlays.remove(widgetId)
                Log.d(TAG, "Stopped gesture detection for widget $widgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping gesture detection for widget $widgetId", e)
            }
        }
    }
    
    private fun extractWidgetBounds(intent: Intent): WidgetBounds? {
        return try {
            WidgetBounds(
                x = intent.getIntExtra("bounds_x", 0),
                y = intent.getIntExtra("bounds_y", 0),
                width = intent.getIntExtra("bounds_width", 0),
                height = intent.getIntExtra("bounds_height", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting widget bounds", e)
            null
        }
    }
    
    private fun createOverlayLayoutParams(bounds: WidgetBounds): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            bounds.width,
            bounds.height,
            bounds.x,
            bounds.y,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }
    
    private fun handleGestureEvent(widgetId: Int, gestureEvent: WidgetGestureManager.GestureEvent) {
        Log.d(TAG, "Processing gesture event for widget $widgetId: ${gestureEvent.direction}")
        
        when (gestureEvent.direction) {
            WidgetGestureManager.SwipeDirection.LEFT -> {
                // Swipe left - next video
                sendGestureAction(widgetId, "next", gestureEvent)
            }
            WidgetGestureManager.SwipeDirection.RIGHT -> {
                // Swipe right - previous video  
                sendGestureAction(widgetId, "previous", gestureEvent)
            }
            else -> {
                Log.d(TAG, "Unhandled gesture direction: ${gestureEvent.direction}")
            }
        }
    }
    
    private fun sendGestureAction(widgetId: Int, action: String, gestureEvent: WidgetGestureManager.GestureEvent) {
        val intent = Intent("com.videowidgetplayer.action.GESTURE_$action").apply {
            putExtra("widget_id", widgetId)
            putExtra("gesture_velocity", gestureEvent.velocity)
            putExtra("gesture_distance", gestureEvent.distance)
            putExtra("gesture_confidence", gestureEvent.confidence)
            putExtra("gesture_duration", gestureEvent.duration)
            setPackage(packageName)
        }
        sendBroadcast(intent)
        Log.d(TAG, "Sent gesture action: $action for widget $widgetId " +
                "(velocity=${gestureEvent.velocity}, confidence=${gestureEvent.confidence})")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all active overlays
        activeOverlays.keys.toList().forEach { widgetId ->
            stopGestureDetection(widgetId)
        }
        
        Log.d(TAG, "WidgetGestureService destroyed")
    }
}
