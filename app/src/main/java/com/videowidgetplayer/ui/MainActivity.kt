package com.videowidgetplayer.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.videowidgetplayer.R
import com.videowidgetplayer.adapter.SelectedVideoAdapter
import com.videowidgetplayer.data.SelectedVideosManager
import com.videowidgetplayer.data.VideoFile
import com.videowidgetplayer.data.VideoRepository
import com.videowidgetplayer.databinding.ActivityMainBinding
import com.videowidgetplayer.utils.MemoryLeakDetector
import com.videowidgetplayer.widget.VideoWidgetProvider
import kotlinx.coroutines.launch

/**
 * Main Activity - Enhanced to list selected videos in widgets with muted playback
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoRepository: VideoRepository
    private lateinit var selectedVideosManager: SelectedVideosManager
    private lateinit var selectedVideoAdapter: SelectedVideoAdapter
    private var selectedVideos = mutableListOf<VideoFile>()

    // Permission launcher for media access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openVideoPicker()
        } else {
            showPermissionError()
        }
    }

    // Video picker launcher
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            processSelectedVideos(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupRecyclerView()
        loadSelectedVideos()
    }

    override fun onResume() {
        super.onResume()
        loadSelectedVideos()
    }

    private fun setupViews() {
        videoRepository = VideoRepository(this)
        selectedVideosManager = SelectedVideosManager(this)
        
        binding.addVideoButton.setOnClickListener {
            checkPermissionAndOpenPicker()
        }
        
        binding.launchWidgetButton.setOnClickListener {
            launchWidget()
        }
        
        binding.debugMemoryButton.setOnClickListener {
            checkMemoryLeaks()
        }
    }

    private fun setupRecyclerView() {
        selectedVideoAdapter = SelectedVideoAdapter(
            onViewVideo = { video ->
                openVideoPlayer(video)
            },
            onRemoveVideo = { position ->
                removeVideoAtPosition(position)
            }
        )
        
        binding.selectedVideosRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = selectedVideoAdapter
        }
    }

    private fun loadSelectedVideos() {
        try {
            selectedVideos.clear()
            val loadedVideos = selectedVideosManager.loadSelectedVideos()
            selectedVideos.addAll(loadedVideos)
            updateUI()
            
            if (loadedVideos.isNotEmpty()) {
                Toast.makeText(this, "Loaded ${loadedVideos.size} saved videos", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading saved videos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val currentList = selectedVideos.toList()
        selectedVideoAdapter.submitList(currentList)
        
        val hasVideos = selectedVideos.isNotEmpty()
        binding.emptyText.visibility = if (hasVideos) View.GONE else View.VISIBLE
        binding.selectedVideosRecyclerView.visibility = if (hasVideos) View.VISIBLE else View.GONE
        binding.launchWidgetButton.visibility = if (hasVideos) View.VISIBLE else View.GONE
        
        if (hasVideos) {
            binding.launchWidgetButton.text = "Launch the Widget (${selectedVideos.size} videos)"
        }
        
        // Update all existing widgets with the new video selection
        updateExistingWidgets()
    }

    private fun updateExistingWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val myProvider = ComponentName(this, VideoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(myProvider)
            
            for (appWidgetId in appWidgetIds) {
                VideoWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openVideoPicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openVideoPicker() {
        try {
            videoPickerLauncher.launch("video/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening video picker: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPermissionError() {
        Toast.makeText(
            this,
            "Permission denied. Cannot access videos without permission.",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun processSelectedVideos(uris: List<Uri>) {
        lifecycleScope.launch {
            try {
                val validVideos = mutableListOf<VideoFile>()
                val invalidVideos = mutableListOf<String>()
                val duplicateVideos = mutableListOf<String>()
                
                for (uri in uris) {
                    val videoFile = videoRepository.getVideoByUri(uri)
                    
                    if (videoFile != null && videoFile.isValidForWidget()) {
                        val isDuplicate = selectedVideos.any { it.id == videoFile.id }
                        
                        if (!isDuplicate) {
                            validVideos.add(videoFile)
                        } else {
                            duplicateVideos.add(videoFile.name)
                        }
                    } else {
                        invalidVideos.add(getFileName(uri))
                    }
                }
                
                for (video in validVideos) {
                    selectedVideos.add(video)
                }
                
                selectedVideosManager.saveSelectedVideos(selectedVideos)
                updateUI()
                
                val messageParts = mutableListOf<String>()
                
                if (validVideos.isNotEmpty()) {
                    messageParts.add("Added ${validVideos.size} video(s) successfully!")
                }
                
                if (duplicateVideos.isNotEmpty()) {
                    messageParts.add("${duplicateVideos.size} video(s) already selected.")
                }
                
                if (invalidVideos.isNotEmpty()) {
                    messageParts.add("${invalidVideos.size} video(s) were too long (>60 seconds).")
                }
                
                if (messageParts.isEmpty()) {
                    messageParts.add("No valid videos found.")
                }
                
                val message = messageParts.joinToString(" ")
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error processing videos: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex) ?: "Unknown"
                    } else {
                        "Unknown"
                    }
                } else {
                    "Unknown"
                }
            } ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun removeVideoAtPosition(position: Int) {
        if (position < 0 || position >= selectedVideos.size) {
            Toast.makeText(this, "Error removing video", Toast.LENGTH_SHORT).show()
            return
        }
        
        val video = selectedVideos[position]
        val removedVideo = selectedVideos.removeAt(position)
        
        selectedVideosManager.saveSelectedVideos(selectedVideos)
        updateUI()
        Toast.makeText(this, "Video '${removedVideo.name}' removed", Toast.LENGTH_SHORT).show()
    }

    private fun openVideoPlayer(video: VideoFile) {
        try {
            val playerDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(video.name)
                .setView(R.layout.dialog_video_player)
                .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
                .create()
            
            playerDialog.show()
            
            val playerView = playerDialog.findViewById<com.google.android.exoplayer2.ui.PlayerView>(R.id.dialogPlayerView)
            val exoPlayer = com.google.android.exoplayer2.ExoPlayer.Builder(this).build()
            
            playerView?.player = exoPlayer
            
            try {
                val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(video.uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot play video: ${e.message}", Toast.LENGTH_LONG).show()
                playerDialog.dismiss()
                return
            }
            
            playerDialog.setOnDismissListener {
                exoPlayer.release()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot play video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchWidget() {
        if (selectedVideos.isEmpty()) {
            Toast.makeText(this, "Please select videos first", Toast.LENGTH_SHORT).show()
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, VideoWidgetProvider::class.java)
        
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val successCallback = android.app.PendingIntent.getBroadcast(
                this, 0, 
                Intent().apply {
                    putExtra("selected_videos_count", selectedVideos.size)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val pinResult = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            
            if (pinResult) {
                Toast.makeText(this, "Widget added! Your ${selectedVideos.size} videos will play with muted audio.", Toast.LENGTH_LONG).show()
            } else {
                showManualWidgetInstructions()
            }
        } else {
            showManualWidgetInstructions()
        }
    }

    private fun showManualWidgetInstructions() {
        val message = """
            To add the video widget:
            1. Long press on your home screen
            2. Select 'Widgets'
            3. Find 'Video Widget Player'
            4. Drag it to your home screen
            
            Your ${selectedVideos.size} selected videos will automatically appear in the widget and play with muted audio.
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Widget Manually")
            .setMessage(message)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    private fun checkMemoryLeaks() {
        // Force garbage collection before checking
        MemoryLeakDetector.forceGC()
        
        val leakCount = MemoryLeakDetector.checkForLeaks()
        val message = if (leakCount == 0) {
            "✓ No memory leaks detected!"
        } else {
            "⚠ Found $leakCount potential memory leaks"
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d("MainActivity", "Memory leak check completed. Found: $leakCount potential leaks")
    }
}
