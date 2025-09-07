package com.videowidgetplayer.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Manager for selected videos persistence
 * Enhanced with proper Uri serialization and error handling
 */
class SelectedVideosManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "selected_videos_prefs"
        private const val KEY_SELECTED_VIDEOS = "selected_videos"
        private const val TAG = "SelectedVideosManager"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Save selected videos to persistent storage
     */
    fun saveSelectedVideos(videos: List<VideoFile>) {
        try {
            val videosJson = gson.toJson(videos)
            val success = prefs.edit().putString(KEY_SELECTED_VIDEOS, videosJson).commit()
            Log.d(TAG, "Saved ${videos.size} videos, success: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving selected videos", e)
        }
    }
    
    /**
     * Load selected videos from persistent storage
     */
    fun loadSelectedVideos(): List<VideoFile> {
        return try {
            val videosJson = prefs.getString(KEY_SELECTED_VIDEOS, null)
            if (videosJson.isNullOrEmpty()) {
                Log.d(TAG, "No saved videos found")
                return emptyList()
            }
            
            val type = object : TypeToken<List<VideoFile>>() {}.type
            val videos = gson.fromJson<List<VideoFile>>(videosJson, type) ?: emptyList()
            
            // Validate and filter out any corrupted entries
            val validVideos = videos.filter { video ->
                try {
                    // Test if the URI is valid and accessible
                    video.uri.toString()
                    video.name.isNotEmpty()
                    video.id > 0
                    
                    // Test if we can actually access the content
                    val cursor = context.contentResolver.query(
                        video.uri,
                        arrayOf(android.provider.MediaStore.Video.Media._ID),
                        null,
                        null,
                        null
                    )
                    val isAccessible = cursor?.use { it.count > 0 } ?: false
                    
                    if (!isAccessible) {
                        Log.w(TAG, "URI no longer accessible: ${video.uri}")
                        false
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Removing corrupted video entry: ${video.name}", e)
                    false
                }
            }
            
            // If we removed corrupted entries, save the cleaned list
            if (validVideos.size != videos.size) {
                Log.i(TAG, "Cleaned up ${videos.size - validVideos.size} corrupted entries")
                saveSelectedVideos(validVideos)
            }
            
            Log.d(TAG, "Loaded ${validVideos.size} valid videos")
            validVideos
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading selected videos, returning empty list", e)
            // Clear corrupted data
            clearAllVideos()
            emptyList()
        }
    }
    
    /**
     * Add a video to selected list
     */
    fun addVideo(video: VideoFile) {
        try {
            val currentVideos = loadSelectedVideos().toMutableList()
            if (!currentVideos.any { it.id == video.id }) {
                currentVideos.add(video)
                saveSelectedVideos(currentVideos)
                Log.d(TAG, "Added video: ${video.name}")
            } else {
                Log.d(TAG, "Video already exists: ${video.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding video: ${video.name}", e)
        }
    }
    
    /**
     * Remove a video from selected list
     */
    fun removeVideo(video: VideoFile) {
        try {
            val currentVideos = loadSelectedVideos().toMutableList()
            val beforeCount = currentVideos.size
            currentVideos.removeAll { it.id == video.id }
            val afterCount = currentVideos.size
            
            if (beforeCount != afterCount) {
                saveSelectedVideos(currentVideos)
                Log.d(TAG, "Removed video: ${video.name}")
            } else {
                Log.d(TAG, "Video not found for removal: ${video.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing video: ${video.name}", e)
        }
    }
    
    /**
     * Get count of selected videos
     */
    fun getSelectedVideoCount(): Int {
        return try {
            loadSelectedVideos().size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video count", e)
            0
        }
    }
    
    /**
     * Clear all selected videos
     */
    fun clearAllVideos() {
        try {
            val success = prefs.edit().remove(KEY_SELECTED_VIDEOS).commit()
            Log.d(TAG, "Cleared all videos, success: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all videos", e)
        }
    }
    
    /**
     * Check if storage is working properly
     */
    fun isStorageWorking(): Boolean {
        return try {
            val testList = listOf<VideoFile>()
            saveSelectedVideos(testList)
            loadSelectedVideos()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Storage test failed", e)
            false
        }
    }
}
