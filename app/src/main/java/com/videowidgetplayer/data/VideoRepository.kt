package com.videowidgetplayer.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.videowidgetplayer.utils.MediaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {
    
    private val _videoFiles = MutableLiveData<List<VideoFile>>()
    val videoFiles: LiveData<List<VideoFile>> = _videoFiles
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _shortVideos = MutableLiveData<List<VideoFile>>()
    val shortVideos: LiveData<List<VideoFile>> = _shortVideos
    
    /**
     * Load all video files from device storage
     */
    fun loadVideoFiles() {
        _loading.value = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = withContext(Dispatchers.IO) {
                    MediaUtils.getVideoFiles(context)
                }
                _videoFiles.value = videos
            } catch (e: Exception) {
                _videoFiles.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Load video files filtered by maximum duration
     * @param maxDurationSeconds Maximum duration in seconds (default: 60)
     */
    suspend fun getShortVideos(maxDurationSeconds: Int = 60): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            MediaUtils.getShortVideoFiles(context, maxDurationSeconds)
        }
    }
    
    /**
     * Load short videos and update LiveData
     * @param maxDurationSeconds Maximum duration in seconds
     */
    fun loadShortVideos(maxDurationSeconds: Int = 60) {
        _loading.value = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = getShortVideos(maxDurationSeconds)
                _shortVideos.value = videos
            } catch (e: Exception) {
                _shortVideos.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Get a specific video by ID
     */
    suspend fun getVideoById(videoId: Long): VideoFile? {
        return withContext(Dispatchers.IO) {
            MediaUtils.getVideoById(context, videoId)
        }
    }
    
    /**
     * Get videos by URIs
     */
    suspend fun getVideosByUris(uris: List<String>): List<VideoFile> {
        return withContext(Dispatchers.IO) {
            MediaUtils.getVideosByUris(context, uris)
        }
    }
    
    /**
     * Check if a video file exists and is accessible
     */
    suspend fun isVideoAccessible(videoFile: VideoFile): Boolean {
        return withContext(Dispatchers.IO) {
            MediaUtils.isVideoAccessible(context, videoFile.uri)
        }
    }
    
    /**
     * Refresh video files cache
     */
    fun refreshVideoFiles() {
        loadVideoFiles()
    }
    
    /**
     * Refresh short videos cache
     */
    fun refreshShortVideos(maxDurationSeconds: Int = 60) {
        loadShortVideos(maxDurationSeconds)
    }
    
    /**
     * Get video files synchronously (use with caution)
     */
    fun getVideoFilesSync(): List<VideoFile> {
        return try {
            MediaUtils.getVideoFiles(context)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get short video files synchronously (use with caution)
     */
    fun getShortVideoFilesSync(maxDurationSeconds: Int = 60): List<VideoFile> {
        return try {
            MediaUtils.getShortVideoFiles(context, maxDurationSeconds)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
