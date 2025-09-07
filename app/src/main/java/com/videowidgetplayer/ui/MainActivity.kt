package com.videowidgetplayer.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.videowidgetplayer.widget.VideoWidgetProvider
import kotlinx.coroutines.launch

/**
 * Main Activity with new flow: Add videos → Show selected → Launch widget
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
        // Reload videos when app comes back to foreground
        // This ensures we have the latest state even after force close
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
                android.util.Log.d("MainActivity", "Successfully loaded ${loadedVideos.size} videos from storage")
                // Show a subtle confirmation to user
                Toast.makeText(this, "Loaded ${loadedVideos.size} saved videos", Toast.LENGTH_SHORT).show()
            } else {
                android.util.Log.d("MainActivity", "No previously selected videos found")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error loading selected videos", e)
            Toast.makeText(this, "Error loading saved videos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        // Create a new list to ensure adapter sees the change
        val currentList = selectedVideos.toList()
        selectedVideoAdapter.submitList(currentList)
        
        val hasVideos = selectedVideos.isNotEmpty()
        binding.emptyText.visibility = if (hasVideos) View.GONE else View.VISIBLE
        binding.selectedVideosRecyclerView.visibility = if (hasVideos) View.VISIBLE else View.GONE
        binding.launchWidgetButton.visibility = if (hasVideos) View.VISIBLE else View.GONE
        
        if (hasVideos) {
            binding.launchWidgetButton.text = "Launch the Widget (${selectedVideos.size} videos)"
        }
        
        // Scroll to show the latest added video
        if (currentList.isNotEmpty()) {
            binding.selectedVideosRecyclerView.smoothScrollToPosition(currentList.size - 1)
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
        videoPickerLauncher.launch("video/*")
    }

    private fun processSelectedVideos(uris: List<Uri>) {
        lifecycleScope.launch {
            try {
                val validVideos = mutableListOf<VideoFile>()
                val invalidVideos = mutableListOf<String>()
                val duplicateVideos = mutableListOf<String>()
                
                for (uri in uris) {
                    // Request persistent permission for this URI
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d("MainActivity", "Granted persistent permission for: $uri")
                    } catch (e: Exception) {
                        android.util.Log.w("MainActivity", "Could not take persistent permission for: $uri", e)
                    }
                    
                    val video = videoRepository.getVideoByUri(uri)
                    if (video != null && video.isValidForWidget()) {
                        // Check if video is already selected
                        if (selectedVideos.any { it.id == video.id }) {
                            duplicateVideos.add(video.name)
                        } else {
                            validVideos.add(video)
                        }
                    } else {
                        invalidVideos.add(getFileName(uri))
                    }
                }
                
                // Add valid videos to selected list AND save to persistence
                for (video in validVideos) {
                    selectedVideos.add(video)
                }
                
                // Save the entire updated list to persistence
                selectedVideosManager.saveSelectedVideos(selectedVideos)
                
                updateUI()
                
                // Show detailed results
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
        return uri.lastPathSegment ?: "Unknown file"
    }

    private fun removeVideoAtPosition(position: Int) {
        android.util.Log.d("MainActivity", "Attempting to remove video at position: $position")
        android.util.Log.d("MainActivity", "Current selectedVideos list size: ${selectedVideos.size}")
        
        if (position >= 0 && position < selectedVideos.size) {
            val removedVideo = selectedVideos.removeAt(position)
            android.util.Log.d("MainActivity", "Successfully removed video at position $position: ${removedVideo.name}")
            
            // Save the updated list to persistence
            selectedVideosManager.saveSelectedVideos(selectedVideos)
            updateUI()
            Toast.makeText(this, "Video '${removedVideo.name}' removed", Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.w("MainActivity", "Invalid position: $position (list size: ${selectedVideos.size})")
            Toast.makeText(this, "Could not remove video - invalid position", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeVideo(video: VideoFile) {
        android.util.Log.d("MainActivity", "Attempting to remove video: ${video.name} (id=${video.id})")
        android.util.Log.d("MainActivity", "Current selectedVideos list:")
        selectedVideos.forEachIndexed { index, v ->
            android.util.Log.d("MainActivity", "  [$index] ${v.name} (id=${v.id})")
        }
        
        val indexToRemove = selectedVideos.indexOfFirst { it.id == video.id }
        if (indexToRemove != -1) {
            val removedVideo = selectedVideos.removeAt(indexToRemove)
            android.util.Log.d("MainActivity", "Successfully removed video at index $indexToRemove: ${removedVideo.name}")
            
            // Save the updated list to persistence
            selectedVideosManager.saveSelectedVideos(selectedVideos)
            updateUI()
            Toast.makeText(this, "Video '${removedVideo.name}' removed", Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.w("MainActivity", "Video not found in list: ${video.name} (id=${video.id})")
            Toast.makeText(this, "Could not find video to remove", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openVideoPlayer(video: VideoFile) {
        showVideoPreviewDialog(video)
    }
    
    private fun showVideoPreviewDialog(video: VideoFile) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_video_preview, null)
        val playerView = dialogView.findViewById<com.google.android.exoplayer2.ui.PlayerView>(R.id.dialogPlayerView)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(video.name)
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> 
                dialog.dismiss()
            }
            .create()
        
        // First, validate that we can access the URI
        try {
            val cursor = contentResolver.query(
                video.uri,
                arrayOf(android.provider.MediaStore.Video.Media._ID),
                null,
                null,
                null
            )
            val isAccessible = cursor?.use { it.count > 0 } ?: false
            cursor?.close()
            
            if (!isAccessible) {
                android.util.Log.e("MainActivity", "URI not accessible: ${video.uri}")
                Toast.makeText(this, "Video file is no longer accessible. Please re-select this video.", Toast.LENGTH_LONG).show()
                
                // Remove the inaccessible video from the list
                removeVideo(video)
                return
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error checking URI accessibility: ${video.uri}", e)
            Toast.makeText(this, "Cannot access video file: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Setup ExoPlayer for the dialog
        val exoPlayer = com.google.android.exoplayer2.ExoPlayer.Builder(this).build()
        playerView.player = exoPlayer
        
        try {
            android.util.Log.d("MainActivity", "Attempting to play video: ${video.uri}")
            val mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(video.uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error playing video: ${video.uri}", e)
            android.widget.Toast.makeText(this, "Cannot play video: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            dialog.dismiss()
            return
        }
        
        dialog.setOnDismissListener {
            exoPlayer.release()
        }
        
        dialog.show()
    }

    private fun launchWidget() {
        if (selectedVideos.isEmpty()) {
            Toast.makeText(this, "Please select videos first", Toast.LENGTH_SHORT).show()
            return
        }

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, VideoWidgetProvider::class.java)
        
        // Try automatic widget creation first
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            val successCallback = android.app.PendingIntent.getBroadcast(
                this, 0, 
                Intent().apply {
                    putExtra("selected_videos_count", selectedVideos.size)
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val result = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            
            if (result) {
                Toast.makeText(this, "Widget will be added to home screen", Toast.LENGTH_LONG).show()
            } else {
                showManualWidgetInstructions()
            }
        } else {
            showManualWidgetInstructions()
        }
    }

    private fun showManualWidgetInstructions() {
        Toast.makeText(
            this, 
            "Please add widget manually:\n1. Long press on home screen\n2. Select Widgets\n3. Find 'Video Widget'\n4. Drag to home screen", 
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showPermissionError() {
        Toast.makeText(this, "Storage permission is required to access videos", Toast.LENGTH_LONG).show()
    }
}
