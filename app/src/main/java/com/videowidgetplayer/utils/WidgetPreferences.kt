package com.videowidgetplayer.utils

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Utility class for managing widget preferences
 * Following the spec: Store widget video selections and playback settings
 */
class WidgetPreferences(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "video_widget_prefs"
        private const val PREF_PREFIX_KEY = "appwidget_"
        private const val PREF_VIDEO_URIS = "_video_uris"
        private const val PREF_CURRENT_INDEX = "_current_index"
        private const val PREF_IS_PLAYING = "_is_playing"
        private const val PREF_IS_MUTED = "_is_muted"
        private const val PREF_AUTO_REFRESH_INTERVAL = "_auto_refresh"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
    
    /**
     * Save selected video URIs for a widget
     */
    fun saveVideoUris(appWidgetId: Int, videoUris: List<Uri>) {
        val uriStrings = videoUris.map { it.toString() }
        prefs.edit()
            .putStringSet(PREF_PREFIX_KEY + appWidgetId + PREF_VIDEO_URIS, uriStrings.toSet())
            .apply()
    }
    
    /**
     * Get saved video URIs for a widget
     */
    fun getVideoUris(appWidgetId: Int): List<Uri> {
        val uriStrings = prefs.getStringSet(PREF_PREFIX_KEY + appWidgetId + PREF_VIDEO_URIS, emptySet())
        return uriStrings?.map { Uri.parse(it) } ?: emptyList()
    }
    
    /**
     * Save current video index
     */
    fun saveCurrentVideoIndex(appWidgetId: Int, index: Int) {
        prefs.edit()
            .putInt(PREF_PREFIX_KEY + appWidgetId + PREF_CURRENT_INDEX, index)
            .apply()
    }
    
    /**
     * Get current video index
     */
    fun getCurrentVideoIndex(appWidgetId: Int): Int {
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + PREF_CURRENT_INDEX, 0)
    }
    
    /**
     * Save playing state
     */
    fun savePlayingState(appWidgetId: Int, isPlaying: Boolean) {
        prefs.edit()
            .putBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_IS_PLAYING, isPlaying)
            .apply()
    }
    
    /**
     * Get playing state
     */
    fun getPlayingState(appWidgetId: Int): Boolean {
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_IS_PLAYING, false)
    }
    
    /**
     * Save muted state
     */
    fun saveMutedState(appWidgetId: Int, isMuted: Boolean) {
        prefs.edit()
            .putBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_IS_MUTED, isMuted)
            .apply()
    }
    
    /**
     * Get muted state
     */
    fun getMutedState(appWidgetId: Int): Boolean {
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + PREF_IS_MUTED, false)
    }
    
    /**
     * Save auto-refresh interval (in milliseconds)
     */
    fun saveAutoRefreshInterval(appWidgetId: Int, intervalMs: Long) {
        prefs.edit()
            .putLong(PREF_PREFIX_KEY + appWidgetId + PREF_AUTO_REFRESH_INTERVAL, intervalMs)
            .apply()
    }
    
    /**
     * Get auto-refresh interval (default 30 seconds)
     */
    fun getAutoRefreshInterval(appWidgetId: Int): Long {
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId + PREF_AUTO_REFRESH_INTERVAL, 30_000)
    }
    
    /**
     * Delete all preferences for a widget
     */
    fun deleteWidgetPrefs(appWidgetId: Int) {
        val editor = prefs.edit()
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_VIDEO_URIS)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_CURRENT_INDEX)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_IS_PLAYING)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_IS_MUTED)
        editor.remove(PREF_PREFIX_KEY + appWidgetId + PREF_AUTO_REFRESH_INTERVAL)
        editor.apply()
    }
    
    /**
     * Check if widget has been configured
     */
    fun isWidgetConfigured(appWidgetId: Int): Boolean {
        return getVideoUris(appWidgetId).isNotEmpty()
    }
    
    /**
     * Get next video index with random rotation
     */
    fun getNextRandomVideoIndex(appWidgetId: Int): Int {
        val videoUris = getVideoUris(appWidgetId)
        if (videoUris.isEmpty()) return 0
        
        val currentIndex = getCurrentVideoIndex(appWidgetId)
        var nextIndex = (Math.random() * videoUris.size).toInt()
        
        // Ensure we don't get the same video twice in a row (if more than 1 video)
        if (videoUris.size > 1 && nextIndex == currentIndex) {
            nextIndex = (nextIndex + 1) % videoUris.size
        }
        
        saveCurrentVideoIndex(appWidgetId, nextIndex)
        return nextIndex
    }
}
