package com.videowidgetplayer.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.videowidgetplayer.utils.MediaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoRepository(private val context: Context) {
    
    private val _videoFiles = MutableLiveData<List<VideoFile>>()
    val videoFiles: LiveData<List<VideoFile>> = _videoFiles
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    fun loadVideoFiles() {
        _loading.value = true
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val videos = MediaUtils.getVideoFiles(context)
                _videoFiles.value = videos
            } catch (e: Exception) {
                _videoFiles.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun refreshVideoFiles() {
        loadVideoFiles()
    }
}
