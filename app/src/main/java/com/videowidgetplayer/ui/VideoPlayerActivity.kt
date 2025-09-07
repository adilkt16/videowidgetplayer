package com.videowidgetplayer.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.videowidgetplayer.databinding.ActivityVideoPlayerBinding

/**
 * Simple video player activity - no full screen to avoid crashes
 */
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var videoUri: Uri? = null

    companion object {
        private const val EXTRA_VIDEO_URI = "extra_video_uri"
        private const val EXTRA_VIDEO_NAME = "extra_video_name"

        fun createIntent(context: Context, videoUri: Uri, videoName: String): Intent {
            return Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URI, videoUri.toString())
                putExtra(EXTRA_VIDEO_NAME, videoName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = Uri.parse(intent.getStringExtra(EXTRA_VIDEO_URI))
        val videoName = intent.getStringExtra(EXTRA_VIDEO_NAME) ?: "Video"
        
        title = videoName

        setupPlayer()
        setupControls()
    }

    private fun setupPlayer() {
        try {
            exoPlayer = ExoPlayer.Builder(this).build()
            binding.playerView.player = exoPlayer

            videoUri?.let { uri ->
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.playWhenReady = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun setupControls() {
        binding.closeButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
}
