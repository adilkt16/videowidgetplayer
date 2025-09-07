package com.videowidgetplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.videowidgetplayer.R
import com.videowidgetplayer.ui.MainActivity
import com.videowidgetplayer.utils.WidgetPreferences

/**
 * Background service for widget audio playback
 * Handles audio playback for widget videos while maintaining synchronization with video frames
 */
class WidgetAudioService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentWidgetId: Int = -1
    private var currentVideoUri: Uri? = null
    private var isAudioFocusGranted = false
    
    companion object {
        const val ACTION_PLAY_AUDIO = "com.videowidgetplayer.PLAY_AUDIO"
        const val ACTION_PAUSE_AUDIO = "com.videowidgetplayer.PAUSE_AUDIO"
        const val ACTION_STOP_AUDIO = "com.videowidgetplayer.STOP_AUDIO"
        const val ACTION_MUTE_AUDIO = "com.videowidgetplayer.MUTE_AUDIO"
        const val ACTION_UNMUTE_AUDIO = "com.videowidgetplayer.UNMUTE_AUDIO"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_VIDEO_URI = "video_uri"
        
        private const val TAG = "WidgetAudioService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "widget_audio_channel"
    }
    
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        Log.d(TAG, "WidgetAudioService created")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        val widgetId = intent?.getIntExtra(EXTRA_WIDGET_ID, -1) ?: -1
        val videoUriString = intent?.getStringExtra(EXTRA_VIDEO_URI)
        val videoUri = videoUriString?.let { Uri.parse(it) }
        
        when (intent?.action) {
            ACTION_PLAY_AUDIO -> {
                if (widgetId != -1 && videoUri != null) {
                    playAudio(widgetId, videoUri)
                }
            }
            ACTION_PAUSE_AUDIO -> {
                pauseAudio()
            }
            ACTION_STOP_AUDIO -> {
                stopAudio()
            }
            ACTION_MUTE_AUDIO -> {
                muteAudio(widgetId)
            }
            ACTION_UNMUTE_AUDIO -> {
                unmuteAudio(widgetId)
            }
        }
        
        return START_STICKY
    }
    
    private fun playAudio(widgetId: Int, videoUri: Uri) {
        try {
            Log.d(TAG, "Starting audio playback for widget $widgetId")
            currentWidgetId = widgetId
            currentVideoUri = videoUri
            
            // Check if widget should be muted
            val widgetPrefs = WidgetPreferences(this)
            val isMuted = widgetPrefs.getMutedState(widgetId)
            
            if (isMuted) {
                Log.d(TAG, "Widget is muted, not starting audio")
                return
            }
            
            // Request audio focus
            if (!requestAudioFocus()) {
                Log.w(TAG, "Could not get audio focus")
                return
            }
            
            // Release any existing MediaPlayer
            releaseMediaPlayer()
            
            // Create and configure MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@WidgetAudioService, videoUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener(this@WidgetAudioService)
                setOnCompletionListener(this@WidgetAudioService)
                setOnErrorListener(this@WidgetAudioService)
                isLooping = true // Loop the audio to match widget frame cycling
                prepareAsync()
            }
            
            // Start foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification())
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio playback", e)
            stopSelf()
        }
    }
    
    private fun pauseAudio() {
        Log.d(TAG, "Pausing audio")
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }
    
    private fun stopAudio() {
        Log.d(TAG, "Stopping audio")
        releaseMediaPlayer()
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }
    
    private fun muteAudio(widgetId: Int) {
        Log.d(TAG, "Muting audio for widget $widgetId")
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
        // Update widget preferences
        val widgetPrefs = WidgetPreferences(this)
        widgetPrefs.saveMutedState(widgetId, true)
    }
    
    private fun unmuteAudio(widgetId: Int) {
        Log.d(TAG, "Unmuting audio for widget $widgetId")
        
        // Update widget preferences
        val widgetPrefs = WidgetPreferences(this)
        widgetPrefs.saveMutedState(widgetId, false)
        
        // Resume or start audio if video is available
        currentVideoUri?.let { uri ->
            if (widgetPrefs.getPlayingState(widgetId)) {
                playAudio(widgetId, uri)
            }
        }
    }
    
    private fun requestAudioFocus(): Boolean {
        audioManager?.let { manager ->
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange)
                    }
                    .build()
                
                val result = manager.requestAudioFocus(audioFocusRequest!!)
                isAudioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                isAudioFocusGranted
            } else {
                @Suppress("DEPRECATION")
                val result = manager.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange) },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
                isAudioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                isAudioFocusGranted
            }
        }
        return false
    }
    
    private fun abandonAudioFocus() {
        audioManager?.let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    manager.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus { }
            }
        }
        isAudioFocusGranted = false
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.let { player ->
                    if (!player.isPlaying) {
                        player.start()
                    }
                    player.setVolume(1.0f, 1.0f)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                stopAudio()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }
    
    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Audio playback for video widgets"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Widget Video Audio")
            .setContentText("Playing audio for video widget")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(TAG, "MediaPlayer prepared, starting playback")
        mp?.start()
    }
    
    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(TAG, "Audio playback completed")
        // Audio will loop automatically due to isLooping = true
    }
    
    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
        stopAudio()
        return true
    }
    
    override fun onDestroy() {
        Log.d(TAG, "WidgetAudioService destroyed")
        releaseMediaPlayer()
        abandonAudioFocus()
        super.onDestroy()
    }
}
