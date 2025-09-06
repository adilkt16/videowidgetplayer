package com.videowidgetplayer.widgets

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Manages gesture sensitivity settings and gesture recognition for video widgets
 */
class WidgetGestureManager private constructor() {
    
    companion object {
        private const val TAG = "WidgetGestureManager"
        private const val PREFS_NAME = "widget_gesture_prefs"
        
        // Gesture sensitivity levels
        const val SENSITIVITY_LOW = 0
        const val SENSITIVITY_MEDIUM = 1
        const val SENSITIVITY_HIGH = 2
        
        // Default gesture thresholds (in pixels)
        private const val DEFAULT_MIN_SWIPE_DISTANCE = 100f
        private const val DEFAULT_MAX_SWIPE_TIME = 300L // milliseconds
        private const val DEFAULT_MIN_VELOCITY = 100f // pixels per second
        
        @Volatile
        private var INSTANCE: WidgetGestureManager? = null
        
        fun getInstance(): WidgetGestureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetGestureManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Gesture sensitivity settings
     */
    data class GestureSensitivity(
        val minSwipeDistance: Float,
        val maxSwipeTime: Long,
        val minVelocity: Float
    ) {
        companion object {
            fun fromLevel(level: Int): GestureSensitivity {
                return when (level) {
                    SENSITIVITY_LOW -> GestureSensitivity(
                        minSwipeDistance = 150f,
                        maxSwipeTime = 500L,
                        minVelocity = 50f
                    )
                    SENSITIVITY_HIGH -> GestureSensitivity(
                        minSwipeDistance = 75f,
                        maxSwipeTime = 200L,
                        minVelocity = 150f
                    )
                    else -> GestureSensitivity( // MEDIUM
                        minSwipeDistance = DEFAULT_MIN_SWIPE_DISTANCE,
                        maxSwipeTime = DEFAULT_MAX_SWIPE_TIME,
                        minVelocity = DEFAULT_MIN_VELOCITY
                    )
                }
            }
        }
    }
    
    /**
     * Gesture direction enum
     */
    enum class SwipeDirection {
        LEFT, RIGHT, UP, DOWN, NONE
    }
    
    /**
     * Gesture event data
     */
    data class GestureEvent(
        val direction: SwipeDirection,
        val distance: Float,
        val velocity: Float,
        val duration: Long,
        val confidence: Float
    )
    
    /**
     * Touch point data for gesture calculation
     */
    data class TouchPoint(
        val x: Float,
        val y: Float,
        val timestamp: Long
    )
    
    /**
     * Set gesture sensitivity for a widget
     */
    fun setGestureSensitivity(context: Context, widgetId: Int, sensitivity: Int) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putInt("gesture_sensitivity_$widgetId", sensitivity)
            .apply()
        Log.d(TAG, "Set gesture sensitivity for widget $widgetId to level $sensitivity")
    }
    
    /**
     * Get gesture sensitivity for a widget
     */
    fun getGestureSensitivity(context: Context, widgetId: Int): Int {
        val prefs = getPreferences(context)
        return prefs.getInt("gesture_sensitivity_$widgetId", SENSITIVITY_MEDIUM)
    }
    
    /**
     * Get gesture sensitivity settings for a widget
     */
    fun getGestureSettings(context: Context, widgetId: Int): GestureSensitivity {
        val level = getGestureSensitivity(context, widgetId)
        return GestureSensitivity.fromLevel(level)
    }
    
    /**
     * Analyze touch points to detect gesture
     */
    fun analyzeGesture(
        context: Context,
        widgetId: Int,
        startPoint: TouchPoint,
        endPoint: TouchPoint
    ): GestureEvent? {
        val settings = getGestureSettings(context, widgetId)
        
        // Calculate gesture metrics
        val deltaX = endPoint.x - startPoint.x
        val deltaY = endPoint.y - startPoint.y
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        val duration = endPoint.timestamp - startPoint.timestamp
        
        // Check minimum requirements
        if (distance < settings.minSwipeDistance || duration > settings.maxSwipeTime) {
            Log.d(TAG, "Gesture rejected: distance=$distance, duration=$duration")
            return null
        }
        
        // Calculate velocity
        val velocity = if (duration > 0) distance / (duration / 1000f) else 0f
        if (velocity < settings.minVelocity) {
            Log.d(TAG, "Gesture rejected: velocity too low ($velocity)")
            return null
        }
        
        // Determine direction
        val direction = determineDirection(deltaX, deltaY)
        if (direction == SwipeDirection.NONE) {
            Log.d(TAG, "Gesture rejected: no clear direction")
            return null
        }
        
        // Calculate confidence based on gesture clarity
        val confidence = calculateConfidence(deltaX, deltaY, distance, velocity, settings)
        
        Log.d(TAG, "Gesture detected: $direction, distance=$distance, velocity=$velocity, confidence=$confidence")
        
        return GestureEvent(
            direction = direction,
            distance = distance,
            velocity = velocity,
            duration = duration,
            confidence = confidence
        )
    }
    
    /**
     * Determine swipe direction from deltas
     */
    private fun determineDirection(deltaX: Float, deltaY: Float): SwipeDirection {
        val absX = abs(deltaX)
        val absY = abs(deltaY)
        
        // Must be primarily horizontal or vertical
        return if (absX > absY * 1.5f) {
            // Horizontal swipe
            if (deltaX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        } else if (absY > absX * 1.5f) {
            // Vertical swipe
            if (deltaY > 0) SwipeDirection.DOWN else SwipeDirection.UP
        } else {
            // Too diagonal, not a clear swipe
            SwipeDirection.NONE
        }
    }
    
    /**
     * Calculate gesture confidence score
     */
    private fun calculateConfidence(
        deltaX: Float,
        deltaY: Float,
        distance: Float,
        velocity: Float,
        settings: GestureSensitivity
    ): Float {
        val absX = abs(deltaX)
        val absY = abs(deltaY)
        
        // Directional clarity (how straight the gesture is)
        val directionalClarity = if (absX > absY) {
            absX / (absX + absY)
        } else {
            absY / (absX + absY)
        }
        
        // Distance factor (longer swipes are more confident)
        val distanceFactor = (distance / settings.minSwipeDistance).coerceAtMost(2f) / 2f
        
        // Velocity factor (faster swipes are more confident)
        val velocityFactor = (velocity / settings.minVelocity).coerceAtMost(3f) / 3f
        
        // Combine factors
        return (directionalClarity * 0.5f + distanceFactor * 0.3f + velocityFactor * 0.2f)
            .coerceIn(0f, 1f)
    }
    
    /**
     * Check if gesture conflicts with system gestures
     */
    fun isGestureConflicting(context: Context, gestureEvent: GestureEvent): Boolean {
        // Check for potential conflicts with system home screen gestures
        return when (gestureEvent.direction) {
            SwipeDirection.UP -> {
                // Up swipes might conflict with app drawer or recent apps
                gestureEvent.distance > 200f && gestureEvent.velocity > 200f
            }
            SwipeDirection.DOWN -> {
                // Down swipes might conflict with notifications
                gestureEvent.distance > 200f && gestureEvent.velocity > 200f
            }
            SwipeDirection.LEFT, SwipeDirection.RIGHT -> {
                // Horizontal swipes are generally safer but can conflict with page switching
                false // We'll handle these
            }
            SwipeDirection.NONE -> false
        }
    }
    
    /**
     * Enable or disable gesture handling for a widget
     */
    fun setGestureEnabled(context: Context, widgetId: Int, enabled: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putBoolean("gesture_enabled_$widgetId", enabled)
            .apply()
        Log.d(TAG, "Gesture handling ${if (enabled) "enabled" else "disabled"} for widget $widgetId")
    }
    
    /**
     * Check if gesture handling is enabled for a widget
     */
    fun isGestureEnabled(context: Context, widgetId: Int): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean("gesture_enabled_$widgetId", true) // Default enabled
    }
    
    /**
     * Get launcher-specific gesture adjustments
     */
    fun getLauncherAdjustments(context: Context): GestureSensitivity? {
        val launcherPackage = getLauncherPackage(context)
        
        return when {
            launcherPackage.contains("nova") -> GestureSensitivity(
                minSwipeDistance = 120f,
                maxSwipeTime = 250L,
                minVelocity = 120f
            )
            launcherPackage.contains("action") -> GestureSensitivity(
                minSwipeDistance = 90f,
                maxSwipeTime = 350L,
                minVelocity = 80f
            )
            launcherPackage.contains("pixel") || launcherPackage.contains("google") -> GestureSensitivity(
                minSwipeDistance = 110f,
                maxSwipeTime = 300L,
                minVelocity = 100f
            )
            else -> null // Use default settings
        }
    }
    
    /**
     * Get the launcher package name
     */
    private fun getLauncherPackage(context: Context): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val resolveInfo = context.packageManager.resolveActivity(intent!!, 0)
            resolveInfo?.activityInfo?.packageName ?: ""
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine launcher package", e)
            ""
        }
    }
    
    /**
     * Get shared preferences for gesture settings
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Reset all gesture settings for a widget
     */
    fun resetGestureSettings(context: Context, widgetId: Int) {
        val prefs = getPreferences(context)
        prefs.edit()
            .remove("gesture_sensitivity_$widgetId")
            .remove("gesture_enabled_$widgetId")
            .apply()
        Log.d(TAG, "Reset gesture settings for widget $widgetId")
    }
}
