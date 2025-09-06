package com.videowidgetplayer.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import com.videowidgetplayer.utils.PreferenceUtils

class VideoPlaybackService : Service() {
    
    companion object {
        private const val TAG = "VideoPlaybackService"
        
        // Actions
        const val ACTION_PLAY = "com.videowidgetplayer.action.PLAY"
        const val ACTION_PAUSE = "com.videowidgetplayer.action.PAUSE"
        const val ACTION_STOP = "com.videowidgetplayer.action.STOP"
        const val ACTION_LOAD_VIDEO = "com.videowidgetplayer.action.LOAD_VIDEO"
        
        // Extras
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_VIDEO_URI = "video_uri"
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var currentWidgetId: Int = -1
    private var currentVideoUri: String? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VideoPlaybackService created")
        initializePlayer()
    }
    
    override fun onDestroy() {
        Log.d(TAG, "VideoPlaybackService destroyed")
        releasePlayer()
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "Service bound")
        return VideoPlaybackBinder()
    }
    
    /**
     * Binder class for service binding
     */
    inner class VideoPlaybackBinder : Binder() {
        fun getService(): VideoPlaybackService = this@VideoPlaybackService
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        intent?.let { handleIntent(it) }
        
        return START_STICKY
    }
    
    /**
     * Initialize ExoPlayer
     */
    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this)
                .build()
                .also { player ->
                    // Set up player listener
                    player.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            Log.d(TAG, "Playback state changed: $playbackState")
                            handlePlaybackStateChange(playbackState)
                        }
                        
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Player error: ${error.message}", error)
                            handlePlayerError(error)
                        }
                        
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            Log.d(TAG, "Is playing changed: $isPlaying")
                            updateWidgetPlayState(isPlaying)
                        }
                    })
                    
                    // Set volume to 0 for widget playback (muted by default)
                    player.volume = 0f
                }
        }
    }
    
    /**
     * Release ExoPlayer resources
     */
    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
        }
        exoPlayer = null
    }
    
    /**
     * Handle incoming intents
     */
    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        
        Log.d(TAG, "Handling action: $action for widget: $widgetId")
        
        when (action) {
            ACTION_LOAD_VIDEO -> {
                val videoUri = intent.getStringExtra(EXTRA_VIDEO_URI)
                if (videoUri != null && widgetId != -1) {
                    loadVideo(widgetId, videoUri)
                }
            }
            ACTION_PLAY -> {
                if (widgetId != -1) {
                    playVideo(widgetId)
                }
            }
            ACTION_PAUSE -> {
                if (widgetId != -1) {
                    pauseVideo(widgetId)
                }
            }
            ACTION_STOP -> {
                if (widgetId != -1) {
                    stopVideo(widgetId)
                }
            }
        }
    }
    
    /**
     * Load video for playback
     */
    private fun loadVideo(widgetId: Int, videoUri: String) {
        Log.d(TAG, "Loading video: $videoUri for widget: $widgetId")
        
        try {
            val uri = Uri.parse(videoUri)
            val mediaItem = MediaItem.fromUri(uri)
            
            exoPlayer?.let { player ->
                player.setMediaItem(mediaItem)
                player.prepare()
                
                currentWidgetId = widgetId
                currentVideoUri = videoUri
                
                Log.d(TAG, "Video loaded successfully")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video: $videoUri", e)
            handleVideoLoadError(widgetId, e)
        }
    }
    
    /**
     * Play video
     */
    private fun playVideo(widgetId: Int) {
        Log.d(TAG, "Playing video for widget: $widgetId")
        
        try {
            // Ensure we have the correct video loaded
            if (currentWidgetId != widgetId) {
                val videoUri = PreferenceUtils.getWidgetVideoUri(this, widgetId)
                if (videoUri != null) {
                    loadVideo(widgetId, videoUri)
                }
            }
            
            exoPlayer?.let { player ->
                if (!player.isPlaying) {
                    player.play()
                    PreferenceUtils.setWidgetPlayState(this, widgetId, true)
                    Log.d(TAG, "Video playback started")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video for widget: $widgetId", e)
            handlePlaybackError(widgetId, e)
        }
    }
    
    /**
     * Pause video
     */
    private fun pauseVideo(widgetId: Int) {
        Log.d(TAG, "Pausing video for widget: $widgetId")
        
        try {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    PreferenceUtils.setWidgetPlayState(this, widgetId, false)
                    Log.d(TAG, "Video playback paused")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing video for widget: $widgetId", e)
        }
    }
    
    /**
     * Stop video
     */
    private fun stopVideo(widgetId: Int) {
        Log.d(TAG, "Stopping video for widget: $widgetId")
        
        try {
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                PreferenceUtils.setWidgetPlayState(this, widgetId, false)
                Log.d(TAG, "Video playback stopped")
            }
            
            currentWidgetId = -1
            currentVideoUri = null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping video for widget: $widgetId", e)
        }
    }
    
    /**
     * Handle playback state changes
     */
    private fun handlePlaybackStateChange(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> {
                Log.d(TAG, "Player state: IDLE")
            }
            Player.STATE_BUFFERING -> {
                Log.d(TAG, "Player state: BUFFERING")
                // Could update widget to show loading
            }
            Player.STATE_READY -> {
                Log.d(TAG, "Player state: READY")
                // Video is ready to play
            }
            Player.STATE_ENDED -> {
                Log.d(TAG, "Player state: ENDED")
                handleVideoEnded()
            }
        }
    }
    
    /**
     * Handle player errors
     */
    private fun handlePlayerError(error: PlaybackException) {
        Log.e(TAG, "ExoPlayer error: ${error.message}")
        
        // Reset play state
        if (currentWidgetId != -1) {
            PreferenceUtils.setWidgetPlayState(this, currentWidgetId, false)
        }
        
        // Could notify widget of error state
    }
    
    /**
     * Handle video load errors
     */
    private fun handleVideoLoadError(widgetId: Int, error: Exception) {
        Log.e(TAG, "Video load error for widget $widgetId: ${error.message}")
        PreferenceUtils.setWidgetPlayState(this, widgetId, false)
    }
    
    /**
     * Handle playback errors
     */
    private fun handlePlaybackError(widgetId: Int, error: Exception) {
        Log.e(TAG, "Playback error for widget $widgetId: ${error.message}")
        PreferenceUtils.setWidgetPlayState(this, widgetId, false)
    }
    
    /**
     * Handle video ended
     */
    private fun handleVideoEnded() {
        if (currentWidgetId != -1) {
            PreferenceUtils.setWidgetPlayState(this, currentWidgetId, false)
            // Could implement auto-repeat or next video logic here
        }
    }
    
    /**
     * Update widget play state
     */
    private fun updateWidgetPlayState(isPlaying: Boolean) {
        if (currentWidgetId != -1) {
            PreferenceUtils.setWidgetPlayState(this, currentWidgetId, isPlaying)
            // Could trigger widget update here
        }
    }
    
    /**
     * Get current playback state
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    /**
     * Get playback state
     */
    fun getPlaybackState(): Int {
        return exoPlayer?.playbackState ?: Player.STATE_IDLE
    }
    
    /**
     * Set playback volume
     */
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
}
