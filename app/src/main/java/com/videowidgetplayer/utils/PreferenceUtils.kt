package com.videowidgetplayer.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtils {
    
    private const val PREFS_NAME = "video_widget_prefs"
    private const val KEY_SELECTED_VIDEO_URI = "selected_video_uri"
    private const val KEY_WIDGET_CONFIG = "widget_config_"
    private const val KEY_WIDGET_VIDEO_URI = "widget_video_uri_"
    private const val KEY_WIDGET_PLAY_STATE = "widget_play_state_"
    private const val KEY_WIDGET_MUTE_STATE = "widget_mute_state_"
    private const val KEY_WIDGET_POSITION = "widget_position_"
    private const val KEY_WIDGET_TITLE = "widget_title_"
    private const val KEY_PLAYBACK_POSITION = "playback_position_"
    
    // Video queue management keys
    private const val KEY_WIDGET_VIDEO_QUEUE = "widget_video_queue_"
    private const val KEY_WIDGET_CURRENT_INDEX = "widget_current_index_"
    private const val KEY_WIDGET_SHUFFLE_ENABLED = "widget_shuffle_enabled_"
    private const val KEY_WIDGET_LOOP_MODE = "widget_loop_mode_"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // Widget-specific preferences
    fun saveWidgetVideoUri(context: Context, widgetId: Int, uri: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_WIDGET_VIDEO_URI + widgetId, uri)
            .apply()
    }
    
    fun getWidgetVideoUri(context: Context, widgetId: Int): String? {
        return getSharedPreferences(context).getString(KEY_WIDGET_VIDEO_URI + widgetId, null)
    }
    
    fun saveWidgetPlayState(context: Context, widgetId: Int, isPlaying: Boolean) {
        getSharedPreferences(context).edit()
            .putBoolean(KEY_WIDGET_PLAY_STATE + widgetId, isPlaying)
            .apply()
    }
    
    fun getWidgetPlayState(context: Context, widgetId: Int): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_WIDGET_PLAY_STATE + widgetId, false)
    }
    
    fun setWidgetPlayState(context: Context, widgetId: Int, isPlaying: Boolean) {
        saveWidgetPlayState(context, widgetId, isPlaying)
    }
    
    fun saveWidgetMuteState(context: Context, widgetId: Int, isMuted: Boolean) {
        getSharedPreferences(context).edit()
            .putBoolean(KEY_WIDGET_MUTE_STATE + widgetId, isMuted)
            .apply()
    }
    
    fun getWidgetMuteState(context: Context, widgetId: Int): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_WIDGET_MUTE_STATE + widgetId, true) // Default muted for widgets
    }
    
    fun setWidgetMuteState(context: Context, widgetId: Int, isMuted: Boolean) {
        saveWidgetMuteState(context, widgetId, isMuted)
    }
    
    fun saveWidgetPosition(context: Context, widgetId: Int, position: Long) {
        getSharedPreferences(context).edit()
            .putLong(KEY_WIDGET_POSITION + widgetId, position)
            .apply()
    }
    
    fun getWidgetPosition(context: Context, widgetId: Int): Long {
        return getSharedPreferences(context).getLong(KEY_WIDGET_POSITION + widgetId, 0L)
    }
    
    fun saveWidgetTitle(context: Context, widgetId: Int, title: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_WIDGET_TITLE + widgetId, title)
            .apply()
    }
    
    fun getWidgetTitle(context: Context, widgetId: Int): String? {
        return getSharedPreferences(context).getString(KEY_WIDGET_TITLE + widgetId, null)
    }
    
    fun deleteWidgetPreferences(context: Context, widgetId: Int) {
        getSharedPreferences(context).edit()
            .remove(KEY_WIDGET_VIDEO_URI + widgetId)
            .remove(KEY_WIDGET_PLAY_STATE + widgetId)
            .remove(KEY_WIDGET_POSITION + widgetId)
            .remove(KEY_WIDGET_TITLE + widgetId)
            .remove(KEY_WIDGET_CONFIG + widgetId) // Legacy support
            .apply()
    }
    
    // General app preferences
    fun saveSelectedVideoUri(context: Context, uri: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_SELECTED_VIDEO_URI, uri)
            .apply()
    }
    
    fun getSelectedVideoUri(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SELECTED_VIDEO_URI, null)
    }
    
    // Multiple video URIs support
    fun saveSelectedVideoUris(context: Context, uris: List<String>) {
        val urisString = uris.joinToString("|")
        getSharedPreferences(context).edit()
            .putString("selected_video_uris", urisString)
            .apply()
    }
    
    fun getSelectedVideoUris(context: Context): List<String> {
        val urisString = getSharedPreferences(context).getString("selected_video_uris", null)
        return if (urisString.isNullOrEmpty()) {
            emptyList()
        } else {
            urisString.split("|").filter { it.isNotEmpty() }
        }
    }
    
    fun clearSelectedVideoUris(context: Context) {
        getSharedPreferences(context).edit()
            .remove("selected_video_uris")
            .apply()
    }
    
    // Legacy widget config methods (for backward compatibility)
    fun saveWidgetConfig(context: Context, widgetId: Int, videoUri: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_WIDGET_CONFIG + widgetId, videoUri)
            .apply()
    }
    
    fun getWidgetConfig(context: Context, widgetId: Int): String? {
        return getSharedPreferences(context).getString(KEY_WIDGET_CONFIG + widgetId, null)
    }
    
    fun removeWidgetConfig(context: Context, widgetId: Int) {
        getSharedPreferences(context).edit()
            .remove(KEY_WIDGET_CONFIG + widgetId)
            .apply()
    }
    
    // Video playback preferences
    fun savePlaybackPosition(context: Context, videoUri: String, position: Long) {
        getSharedPreferences(context).edit()
            .putLong(KEY_PLAYBACK_POSITION + videoUri.hashCode(), position)
            .apply()
    }
    
    fun getPlaybackPosition(context: Context, videoUri: String): Long {
        return getSharedPreferences(context).getLong(KEY_PLAYBACK_POSITION + videoUri.hashCode(), 0L)
    }
    
    // Utility methods
    fun hasWidgetConfiguration(context: Context, widgetId: Int): Boolean {
        return getWidgetVideoUri(context, widgetId) != null || 
               getWidgetConfig(context, widgetId) != null
    }
    
    fun getAllConfiguredWidgets(context: Context): List<Int> {
        val prefs = getSharedPreferences(context)
        val configuredWidgets = mutableListOf<Int>()
        
        for (key in prefs.all.keys) {
            when {
                key.startsWith(KEY_WIDGET_VIDEO_URI) -> {
                    val widgetId = key.removePrefix(KEY_WIDGET_VIDEO_URI).toIntOrNull()
                    if (widgetId != null && prefs.getString(key, null) != null) {
                        configuredWidgets.add(widgetId)
                    }
                }
                key.startsWith(KEY_WIDGET_CONFIG) -> {
                    val widgetId = key.removePrefix(KEY_WIDGET_CONFIG).toIntOrNull()
                    if (widgetId != null && prefs.getString(key, null) != null) {
                        if (!configuredWidgets.contains(widgetId)) {
                            configuredWidgets.add(widgetId)
                        }
                    }
                }
            }
        }
        
        return configuredWidgets.sorted()
    }
    
    // Video queue management methods
    fun setWidgetVideoQueue(context: Context, widgetId: Int, videoUris: List<String>) {
        val queueString = videoUris.joinToString("|")
        getSharedPreferences(context).edit()
            .putString(KEY_WIDGET_VIDEO_QUEUE + widgetId, queueString)
            .apply()
    }
    
    fun getWidgetVideoQueue(context: Context, widgetId: Int): List<String> {
        val queueString = getSharedPreferences(context).getString(KEY_WIDGET_VIDEO_QUEUE + widgetId, "")
        return if (queueString.isNullOrEmpty()) {
            emptyList()
        } else {
            queueString.split("|").filter { it.isNotEmpty() }
        }
    }
    
    fun setWidgetCurrentVideoIndex(context: Context, widgetId: Int, index: Int) {
        getSharedPreferences(context).edit()
            .putInt(KEY_WIDGET_CURRENT_INDEX + widgetId, index)
            .apply()
    }
    
    fun getWidgetCurrentVideoIndex(context: Context, widgetId: Int): Int {
        return getSharedPreferences(context).getInt(KEY_WIDGET_CURRENT_INDEX + widgetId, 0)
    }
    
    fun setWidgetShuffleEnabled(context: Context, widgetId: Int, enabled: Boolean) {
        getSharedPreferences(context).edit()
            .putBoolean(KEY_WIDGET_SHUFFLE_ENABLED + widgetId, enabled)
            .apply()
    }
    
    fun getWidgetShuffleEnabled(context: Context, widgetId: Int): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_WIDGET_SHUFFLE_ENABLED + widgetId, false)
    }
    
    fun setWidgetLoopMode(context: Context, widgetId: Int, loopMode: Int) {
        getSharedPreferences(context).edit()
            .putInt(KEY_WIDGET_LOOP_MODE + widgetId, loopMode)
            .apply()
    }
    
    fun getWidgetLoopMode(context: Context, widgetId: Int): Int {
        return getSharedPreferences(context).getInt(KEY_WIDGET_LOOP_MODE + widgetId, 0) // Default: NONE
    }
    
    fun clearAllPreferences(context: Context) {
        getSharedPreferences(context).edit().clear().apply()
    }
}
