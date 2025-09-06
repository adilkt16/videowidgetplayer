package com.videowidgetplayer.widgets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.RemoteViews
import com.videowidgetplayer.R
import kotlinx.coroutines.*

/**
 * Manages smooth transition animations between videos in widgets
 */
class WidgetTransitionManager private constructor() {
    
    companion object {
        private const val TAG = "WidgetTransitionManager"
        
        // Animation durations
        private const val FADE_DURATION = 200L
        private const val SLIDE_DURATION = 300L
        private const val ZOOM_DURATION = 250L
        
        // Animation types
        const val TRANSITION_FADE = 0
        const val TRANSITION_SLIDE_LEFT = 1
        const val TRANSITION_SLIDE_RIGHT = 2
        const val TRANSITION_ZOOM = 3
        
        @Volatile
        private var INSTANCE: WidgetTransitionManager? = null
        
        fun getInstance(): WidgetTransitionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetTransitionManager().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeTransitions = mutableMapOf<Int, Job>()
    
    /**
     * Transition configuration
     */
    data class TransitionConfig(
        val type: Int = TRANSITION_FADE,
        val duration: Long = FADE_DURATION,
        val showProgress: Boolean = true,
        val enableHapticFeedback: Boolean = true
    )
    
    /**
     * Execute transition animation for video change
     */
    fun executeTransition(
        context: Context,
        widgetId: Int,
        fromVideoUri: String?,
        toVideoUri: String,
        direction: SwipeDirection,
        config: TransitionConfig = TransitionConfig()
    ) {
        Log.d(TAG, "Starting transition for widget $widgetId: $fromVideoUri -> $toVideoUri")
        
        // Cancel any existing transition for this widget
        cancelTransition(widgetId)
        
        val transitionJob = scope.launch {
            try {
                when (config.type) {
                    TRANSITION_SLIDE_LEFT -> executeSlideTransition(context, widgetId, direction, config)
                    TRANSITION_SLIDE_RIGHT -> executeSlideTransition(context, widgetId, direction, config)
                    TRANSITION_ZOOM -> executeZoomTransition(context, widgetId, config)
                    else -> executeFadeTransition(context, widgetId, config)
                }
                
                // Update with new video content
                updateVideoContent(context, widgetId, toVideoUri)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during transition for widget $widgetId", e)
                // Fallback to immediate update
                updateVideoContent(context, widgetId, toVideoUri)
            } finally {
                activeTransitions.remove(widgetId)
            }
        }
        
        activeTransitions[widgetId] = transitionJob
    }
    
    /**
     * Execute fade transition
     */
    private suspend fun executeFadeTransition(
        context: Context,
        widgetId: Int,
        config: TransitionConfig
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, getWidgetLayout(context, widgetId))
        
        // Show loading state
        if (config.showProgress) {
            views.setViewVisibility(R.id.loading_indicator, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.play_overlay, android.view.View.GONE)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
        
        // Simulate fade duration
        delay(config.duration)
        
        // Hide loading
        if (config.showProgress) {
            views.setViewVisibility(R.id.loading_indicator, android.view.View.GONE)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
    
    /**
     * Execute slide transition
     */
    private suspend fun executeSlideTransition(
        context: Context,
        widgetId: Int,
        direction: SwipeDirection,
        config: TransitionConfig
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, getWidgetLayout(context, widgetId))
        
        // Show transition indicator
        views.setViewVisibility(R.id.loading_indicator, android.view.View.VISIBLE)
        
        // Add directional hint if possible
        when (direction) {
            SwipeDirection.LEFT -> {
                // Sliding to next video
                views.setTextViewText(R.id.transition_hint, "Next →")
            }
            SwipeDirection.RIGHT -> {
                // Sliding to previous video
                views.setTextViewText(R.id.transition_hint, "← Previous")
            }
            else -> {}
        }
        
        if (hasView(getWidgetLayout(context, widgetId), R.id.transition_hint)) {
            views.setViewVisibility(R.id.transition_hint, android.view.View.VISIBLE)
        }
        
        appWidgetManager.updateAppWidget(widgetId, views)
        
        // Simulate slide duration
        delay(config.duration)
        
        // Hide transition elements
        views.setViewVisibility(R.id.loading_indicator, android.view.View.GONE)
        if (hasView(getWidgetLayout(context, widgetId), R.id.transition_hint)) {
            views.setViewVisibility(R.id.transition_hint, android.view.View.GONE)
        }
        
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    /**
     * Execute zoom transition
     */
    private suspend fun executeZoomTransition(
        context: Context,
        widgetId: Int,
        config: TransitionConfig
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, getWidgetLayout(context, widgetId))
        
        // Show loading with zoom effect simulation
        views.setViewVisibility(R.id.loading_indicator, android.view.View.VISIBLE)
        appWidgetManager.updateAppWidget(widgetId, views)
        
        // Simulate zoom animation timing
        delay(config.duration / 2)
        
        // Midpoint - could change thumbnail alpha or add effects
        delay(config.duration / 2)
        
        views.setViewVisibility(R.id.loading_indicator, android.view.View.GONE)
        appWidgetManager.updateAppWidget(widgetId, views)
    }
    
    /**
     * Update video content after transition
     */
    private fun updateVideoContent(context: Context, widgetId: Int, videoUri: String) {
        try {
            val videoManager = WidgetVideoManager.getInstance()
            videoManager.loadVideoForWidget(context, widgetId, videoUri)
            
            Log.d(TAG, "Updated video content for widget $widgetId: $videoUri")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating video content for widget $widgetId", e)
        }
    }
    
    /**
     * Cancel active transition for a widget
     */
    fun cancelTransition(widgetId: Int) {
        activeTransitions[widgetId]?.let { job ->
            job.cancel()
            activeTransitions.remove(widgetId)
            Log.d(TAG, "Cancelled transition for widget $widgetId")
        }
    }
    
    /**
     * Check if transition is active for a widget
     */
    fun isTransitionActive(widgetId: Int): Boolean {
        return activeTransitions[widgetId]?.isActive == true
    }
    
    /**
     * Get widget layout for transition
     */
    private fun getWidgetLayout(context: Context, widgetId: Int): Int {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            VideoWidgetProvider.getWidgetLayout(options)
        } catch (e: Exception) {
            Log.w(TAG, "Error getting widget layout, using default", e)
            R.layout.video_widget
        }
    }
    
    /**
     * Check if layout has specific view
     */
    private fun hasView(layoutId: Int, viewId: Int): Boolean {
        return when (layoutId) {
            R.layout.video_widget_large -> true // Large layout has all views
            R.layout.video_widget_compact -> viewId in listOf(
                R.id.video_thumbnail, R.id.play_pause_button, R.id.loading_indicator
            )
            else -> viewId in listOf(
                R.id.video_thumbnail, R.id.play_pause_button, R.id.loading_indicator,
                R.id.play_overlay
            )
        }
    }
    
    /**
     * Create transition based on gesture direction
     */
    fun createTransitionForGesture(
        gestureDirection: SwipeDirection,
        gestureVelocity: Float
    ): TransitionConfig {
        return when (gestureDirection) {
            SwipeDirection.LEFT -> TransitionConfig(
                type = TRANSITION_SLIDE_LEFT,
                duration = calculateDurationFromVelocity(gestureVelocity, SLIDE_DURATION),
                showProgress = true
            )
            SwipeDirection.RIGHT -> TransitionConfig(
                type = TRANSITION_SLIDE_RIGHT,
                duration = calculateDurationFromVelocity(gestureVelocity, SLIDE_DURATION),
                showProgress = true
            )
            else -> TransitionConfig(
                type = TRANSITION_FADE,
                duration = FADE_DURATION
            )
        }
    }
    
    /**
     * Calculate animation duration based on gesture velocity
     */
    private fun calculateDurationFromVelocity(velocity: Float, baseDuration: Long): Long {
        // Faster gestures get shorter animations
        val velocityFactor = (velocity / 200f).coerceIn(0.5f, 2f)
        return (baseDuration / velocityFactor).toLong().coerceIn(100L, 500L)
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        activeTransitions.clear()
        Log.d(TAG, "WidgetTransitionManager cleaned up")
    }
    
    /**
     * Swipe direction enum
     */
    enum class SwipeDirection {
        LEFT, RIGHT, UP, DOWN, NONE
    }
}
