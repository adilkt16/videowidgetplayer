package com.videowidgetplayer.data

import android.content.Context
import android.util.Log
import com.videowidgetplayer.utils.PreferenceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages video queue and navigation for widgets
 */
class VideoQueueManager private constructor() {
    
    companion object {
        private const val TAG = "VideoQueueManager"
        
        @Volatile
        private var INSTANCE: VideoQueueManager? = null
        
        fun getInstance(): VideoQueueManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VideoQueueManager().also { INSTANCE = it }
            }
        }
    }
    
    // Widget-specific video queues
    private val widgetQueues = mutableMapOf<Int, VideoQueue>()
    
    // Current queue state flow for reactive updates
    private val _currentQueueState = MutableStateFlow<VideoQueueState?>(null)
    val currentQueueState: StateFlow<VideoQueueState?> = _currentQueueState.asStateFlow()
    
    /**
     * Initialize or update video queue for a widget
     */
    fun initializeQueue(context: Context, widgetId: Int, videoUris: List<String>) {
        Log.d(TAG, "Initializing queue for widget $widgetId with ${videoUris.size} videos")
        
        if (videoUris.isEmpty()) {
            Log.w(TAG, "Empty video list provided for widget $widgetId")
            return
        }
        
        val currentIndex = PreferenceUtils.getWidgetCurrentVideoIndex(context, widgetId)
        val shuffleEnabled = PreferenceUtils.getWidgetShuffleEnabled(context, widgetId)
        val loopMode = PreferenceUtils.getWidgetLoopMode(context, widgetId)
        
        val queue = VideoQueue(
            widgetId = widgetId,
            originalVideos = videoUris,
            currentIndex = currentIndex.coerceIn(0, videoUris.size - 1),
            shuffleEnabled = shuffleEnabled,
            loopMode = LoopMode.fromInt(loopMode)
        )
        
        // Apply shuffle if enabled
        if (shuffleEnabled) {
            queue.shuffle()
        }
        
        widgetQueues[widgetId] = queue
        
        // Save queue state
        saveQueueState(context, queue)
        
        // Update state flow
        updateQueueState(queue)
        
        Log.d(TAG, "Queue initialized for widget $widgetId: ${queue.getCurrentVideo()}")
    }
    
    /**
     * Get current video for widget
     */
    fun getCurrentVideo(widgetId: Int): String? {
        return widgetQueues[widgetId]?.getCurrentVideo()
    }
    
    /**
     * Move to next video in queue
     */
    fun nextVideo(context: Context, widgetId: Int): String? {
        val queue = widgetQueues[widgetId] ?: return null
        
        Log.d(TAG, "Moving to next video for widget $widgetId")
        
        val nextVideo = queue.next()
        
        if (nextVideo != null) {
            // Save updated index
            PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, queue.currentIndex)
            updateQueueState(queue)
            
            Log.d(TAG, "Next video for widget $widgetId: $nextVideo (index: ${queue.currentIndex})")
        } else {
            Log.d(TAG, "No next video available for widget $widgetId")
        }
        
        return nextVideo
    }
    
    /**
     * Move to previous video in queue
     */
    fun previousVideo(context: Context, widgetId: Int): String? {
        val queue = widgetQueues[widgetId] ?: return null
        
        Log.d(TAG, "Moving to previous video for widget $widgetId")
        
        val previousVideo = queue.previous()
        
        if (previousVideo != null) {
            // Save updated index
            PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, queue.currentIndex)
            updateQueueState(queue)
            
            Log.d(TAG, "Previous video for widget $widgetId: $previousVideo (index: ${queue.currentIndex})")
        } else {
            Log.d(TAG, "No previous video available for widget $widgetId")
        }
        
        return previousVideo
    }
    
    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle(context: Context, widgetId: Int): Boolean {
        val queue = widgetQueues[widgetId] ?: return false
        
        val newShuffleState = !queue.shuffleEnabled
        queue.shuffleEnabled = newShuffleState
        
        Log.d(TAG, "Toggling shuffle for widget $widgetId: $newShuffleState")
        
        if (newShuffleState) {
            queue.shuffle()
        } else {
            queue.unshuffle()
        }
        
        // Save shuffle state
        PreferenceUtils.setWidgetShuffleEnabled(context, widgetId, newShuffleState)
        PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, queue.currentIndex)
        
        updateQueueState(queue)
        
        return newShuffleState
    }
    
    /**
     * Set loop mode
     */
    fun setLoopMode(context: Context, widgetId: Int, loopMode: LoopMode) {
        val queue = widgetQueues[widgetId] ?: return
        
        Log.d(TAG, "Setting loop mode for widget $widgetId: $loopMode")
        
        queue.loopMode = loopMode
        
        // Save loop mode
        PreferenceUtils.setWidgetLoopMode(context, widgetId, loopMode.value)
        
        updateQueueState(queue)
    }
    
    /**
     * Get current queue info
     */
    fun getQueueInfo(widgetId: Int): VideoQueueInfo? {
        val queue = widgetQueues[widgetId] ?: return null
        
        return VideoQueueInfo(
            currentIndex = queue.currentIndex,
            totalVideos = queue.videos.size,
            currentVideo = queue.getCurrentVideo(),
            shuffleEnabled = queue.shuffleEnabled,
            loopMode = queue.loopMode,
            hasNext = queue.hasNext(),
            hasPrevious = queue.hasPrevious()
        )
    }
    
    /**
     * Jump to specific video index
     */
    fun jumpToVideo(context: Context, widgetId: Int, index: Int): String? {
        val queue = widgetQueues[widgetId] ?: return null
        
        if (index < 0 || index >= queue.videos.size) {
            Log.w(TAG, "Invalid index $index for widget $widgetId")
            return null
        }
        
        Log.d(TAG, "Jumping to video index $index for widget $widgetId")
        
        queue.currentIndex = index
        
        // Save updated index
        PreferenceUtils.setWidgetCurrentVideoIndex(context, widgetId, index)
        updateQueueState(queue)
        
        return queue.getCurrentVideo()
    }
    
    /**
     * Select random video
     */
    fun selectRandomVideo(context: Context, widgetId: Int): String? {
        val queue = widgetQueues[widgetId] ?: return null
        
        if (queue.videos.size <= 1) return queue.getCurrentVideo()
        
        Log.d(TAG, "Selecting random video for widget $widgetId")
        
        // Get random index different from current
        var randomIndex: Int
        do {
            randomIndex = (0 until queue.videos.size).random()
        } while (randomIndex == queue.currentIndex && queue.videos.size > 1)
        
        return jumpToVideo(context, widgetId, randomIndex)
    }
    
    /**
     * Remove widget queue
     */
    fun removeQueue(widgetId: Int) {
        Log.d(TAG, "Removing queue for widget $widgetId")
        widgetQueues.remove(widgetId)
        
        // Clear state if this was the current queue
        if (_currentQueueState.value?.widgetId == widgetId) {
            _currentQueueState.value = null
        }
    }
    
    /**
     * Save queue state to preferences
     */
    private fun saveQueueState(context: Context, queue: VideoQueue) {
        PreferenceUtils.setWidgetVideoQueue(context, queue.widgetId, queue.videos)
        PreferenceUtils.setWidgetCurrentVideoIndex(context, queue.widgetId, queue.currentIndex)
        PreferenceUtils.setWidgetShuffleEnabled(context, queue.widgetId, queue.shuffleEnabled)
        PreferenceUtils.setWidgetLoopMode(context, queue.widgetId, queue.loopMode.value)
    }
    
    /**
     * Update current queue state flow
     */
    private fun updateQueueState(queue: VideoQueue) {
        _currentQueueState.value = VideoQueueState(
            widgetId = queue.widgetId,
            currentIndex = queue.currentIndex,
            totalVideos = queue.videos.size,
            currentVideo = queue.getCurrentVideo(),
            shuffleEnabled = queue.shuffleEnabled,
            loopMode = queue.loopMode,
            hasNext = queue.hasNext(),
            hasPrevious = queue.hasPrevious()
        )
    }
    
    /**
     * Restore queue from preferences
     */
    fun restoreQueue(context: Context, widgetId: Int) {
        Log.d(TAG, "Restoring queue for widget $widgetId")
        
        val savedVideos = PreferenceUtils.getWidgetVideoQueue(context, widgetId)
        if (savedVideos.isNotEmpty()) {
            initializeQueue(context, widgetId, savedVideos)
        }
    }
}

/**
 * Video queue data class
 */
data class VideoQueue(
    val widgetId: Int,
    val originalVideos: List<String>,
    var videos: MutableList<String> = originalVideos.toMutableList(),
    var currentIndex: Int = 0,
    var shuffleEnabled: Boolean = false,
    var loopMode: LoopMode = LoopMode.NONE
) {
    
    fun getCurrentVideo(): String? {
        return if (currentIndex in 0 until videos.size) videos[currentIndex] else null
    }
    
    fun hasNext(): Boolean {
        return when (loopMode) {
            LoopMode.NONE -> currentIndex < videos.size - 1
            LoopMode.SINGLE -> true
            LoopMode.ALL -> true
        }
    }
    
    fun hasPrevious(): Boolean {
        return when (loopMode) {
            LoopMode.NONE -> currentIndex > 0
            LoopMode.SINGLE -> true
            LoopMode.ALL -> true
        }
    }
    
    fun next(): String? {
        return when (loopMode) {
            LoopMode.NONE -> {
                if (currentIndex < videos.size - 1) {
                    currentIndex++
                    getCurrentVideo()
                } else null
            }
            LoopMode.SINGLE -> getCurrentVideo() // Stay on same video
            LoopMode.ALL -> {
                currentIndex = if (currentIndex < videos.size - 1) {
                    currentIndex + 1
                } else {
                    0 // Loop back to first
                }
                getCurrentVideo()
            }
        }
    }
    
    fun previous(): String? {
        return when (loopMode) {
            LoopMode.NONE -> {
                if (currentIndex > 0) {
                    currentIndex--
                    getCurrentVideo()
                } else null
            }
            LoopMode.SINGLE -> getCurrentVideo() // Stay on same video
            LoopMode.ALL -> {
                currentIndex = if (currentIndex > 0) {
                    currentIndex - 1
                } else {
                    videos.size - 1 // Loop back to last
                }
                getCurrentVideo()
            }
        }
    }
    
    fun shuffle() {
        if (videos.size <= 1) return
        
        val currentVideo = getCurrentVideo()
        videos.shuffle()
        
        // Ensure current video stays at current index if possible
        currentVideo?.let { current ->
            val newIndex = videos.indexOf(current)
            if (newIndex != -1 && newIndex != currentIndex) {
                // Swap current video back to current index
                videos[newIndex] = videos[currentIndex]
                videos[currentIndex] = current
            }
        }
    }
    
    fun unshuffle() {
        val currentVideo = getCurrentVideo()
        videos.clear()
        videos.addAll(originalVideos)
        
        // Update current index to match current video in original order
        currentVideo?.let { current ->
            val originalIndex = originalVideos.indexOf(current)
            if (originalIndex != -1) {
                currentIndex = originalIndex
            }
        }
    }
}

/**
 * Loop mode enumeration
 */
enum class LoopMode(val value: Int) {
    NONE(0),    // No looping
    SINGLE(1),  // Repeat current video
    ALL(2);     // Loop through all videos
    
    companion object {
        fun fromInt(value: Int): LoopMode {
            return values().find { it.value == value } ?: NONE
        }
    }
}

/**
 * Queue state for reactive updates
 */
data class VideoQueueState(
    val widgetId: Int,
    val currentIndex: Int,
    val totalVideos: Int,
    val currentVideo: String?,
    val shuffleEnabled: Boolean,
    val loopMode: LoopMode,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Queue information data class
 */
data class VideoQueueInfo(
    val currentIndex: Int,
    val totalVideos: Int,
    val currentVideo: String?,
    val shuffleEnabled: Boolean,
    val loopMode: LoopMode,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
