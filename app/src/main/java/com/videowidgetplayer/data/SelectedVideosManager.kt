package com.videowidgetplayer.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manager for selected videos persistence
 */
class SelectedVideosManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "selected_videos_prefs"
        private const val KEY_SELECTED_VIDEOS = "selected_videos"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Save selected videos to persistent storage
     */
    fun saveSelectedVideos(videos: List<VideoFile>) {
        val videosJson = gson.toJson(videos)
        prefs.edit().putString(KEY_SELECTED_VIDEOS, videosJson).apply()
    }
    
    /**
     * Load selected videos from persistent storage
     */
    fun loadSelectedVideos(): List<VideoFile> {
        val videosJson = prefs.getString(KEY_SELECTED_VIDEOS, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<VideoFile>>() {}.type
            gson.fromJson(videosJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a video to selected list
     */
    fun addVideo(video: VideoFile) {
        val currentVideos = loadSelectedVideos().toMutableList()
        if (!currentVideos.any { it.id == video.id }) {
            currentVideos.add(video)
            saveSelectedVideos(currentVideos)
        }
    }
    
    /**
     * Remove a video from selected list
     */
    fun removeVideo(video: VideoFile) {
        val currentVideos = loadSelectedVideos().toMutableList()
        currentVideos.removeAll { it.id == video.id }
        saveSelectedVideos(currentVideos)
    }
    
    /**
     * Get count of selected videos
     */
    fun getSelectedVideoCount(): Int {
        return loadSelectedVideos().size
    }
    
    /**
     * Clear all selected videos
     */
    fun clearAllVideos() {
        prefs.edit().remove(KEY_SELECTED_VIDEOS).apply()
    }
}
