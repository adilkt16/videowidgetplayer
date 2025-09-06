package com.videowidgetplayer.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceUtils(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "video_widget_prefs"
        private const val KEY_SELECTED_VIDEO_URI = "selected_video_uri"
        private const val KEY_WIDGET_CONFIG = "widget_config_"
        private const val KEY_PLAYBACK_POSITION = "playback_position_"
    }
    
    fun saveSelectedVideoUri(uri: String) {
        sharedPreferences.edit()
            .putString(KEY_SELECTED_VIDEO_URI, uri)
            .apply()
    }
    
    fun getSelectedVideoUri(): String? {
        return sharedPreferences.getString(KEY_SELECTED_VIDEO_URI, null)
    }
    
    fun saveWidgetConfig(widgetId: Int, videoUri: String) {
        sharedPreferences.edit()
            .putString(KEY_WIDGET_CONFIG + widgetId, videoUri)
            .apply()
    }
    
    fun getWidgetConfig(widgetId: Int): String? {
        return sharedPreferences.getString(KEY_WIDGET_CONFIG + widgetId, null)
    }
    
    fun removeWidgetConfig(widgetId: Int) {
        sharedPreferences.edit()
            .remove(KEY_WIDGET_CONFIG + widgetId)
            .apply()
    }
    
    fun savePlaybackPosition(videoUri: String, position: Long) {
        sharedPreferences.edit()
            .putLong(KEY_PLAYBACK_POSITION + videoUri.hashCode(), position)
            .apply()
    }
    
    fun getPlaybackPosition(videoUri: String): Long {
        return sharedPreferences.getLong(KEY_PLAYBACK_POSITION + videoUri.hashCode(), 0L)
    }
}
