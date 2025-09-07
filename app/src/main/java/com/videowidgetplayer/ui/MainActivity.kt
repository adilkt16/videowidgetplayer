package com.videowidgetplayer.ui

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import com.videowidgetplayer.adapter.VideoSelectionAdapter
import com.videowidgetplayer.data.VideoFile
import com.videowidgetplayer.data.VideoRepository
import com.videowidgetplayer.databinding.ActivityMainBinding
import com.videowidgetplayer.widget.VideoWidgetProvider
import kotlinx.coroutines.launch

/**
 * Main Activity for video selection and widget management
 * Following the spec: Gallery integration, multi-selection, 60-second filtering
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoRepository: VideoRepository
    private lateinit var videoAdapter: VideoSelectionAdapter
    private var selectedVideos = mutableListOf<VideoFile>()

    // Permission launcher for media access
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadVideos()
        } else {
            showPermissionError()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupRecyclerView()
        checkPermissionAndLoadVideos()
    }

    private fun setupViews() {
        videoRepository = VideoRepository(this)
        
        binding.addWidgetButton.setOnClickListener {
            addWidgetToHomeScreen()
        }
        
        updateAddWidgetButton()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoSelectionAdapter { video, isSelected ->
            if (isSelected) {
                selectedVideos.add(video)
            } else {
                selectedVideos.removeAll { it.id == video.id }
            }
            updateAddWidgetButton()
        }
        
        binding.videoRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
        }
    }

    private fun checkPermissionAndLoadVideos() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                loadVideos()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun loadVideos() {
        lifecycleScope.launch {
            try {
                val videos = videoRepository.getVideosFromGallery()
                
                if (videos.isEmpty()) {
                    showEmptyState()
                } else {
                    showVideoList(videos)
                }
            } catch (e: Exception) {
                showError("Failed to load videos: ${e.message}")
            }
        }
    }

    private fun showVideoList(videos: List<VideoFile>) {
        binding.videoRecyclerView.visibility = View.VISIBLE
        binding.emptyText.visibility = View.GONE
        videoAdapter.submitList(videos)
    }

    private fun showEmptyState() {
        binding.videoRecyclerView.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
        binding.emptyText.text = "No videos found under 60 seconds.\n\nThe widget only supports videos that are 60 seconds or shorter."
    }

    private fun showPermissionError() {
        binding.videoRecyclerView.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
        binding.emptyText.text = getString(R.string.permission_required)
        binding.addWidgetButton.text = getString(R.string.grant_permission)
        binding.addWidgetButton.setOnClickListener {
            checkPermissionAndLoadVideos()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateAddWidgetButton() {
        val hasSelection = selectedVideos.isNotEmpty()
        binding.addWidgetButton.isEnabled = hasSelection
        binding.addWidgetButton.text = if (hasSelection) {
            "Add Widget (${selectedVideos.size} videos)"
        } else {
            getString(R.string.select_at_least_one)
        }
    }

    private fun addWidgetToHomeScreen() {
        if (selectedVideos.isEmpty()) {
            Toast.makeText(this, getString(R.string.select_at_least_one), Toast.LENGTH_SHORT).show()
            return
        }

        // Launch widget picker
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, VideoWidgetProvider::class.java)
        
        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            // Create intent for widget configuration
            val configIntent = Intent(this, VideoWidgetProvider::class.java).apply {
                action = "com.videowidgetplayer.CONFIGURE_WIDGET"
                putExtra("selected_video_uris", selectedVideos.map { it.uri.toString() }.toTypedArray())
            }
            
            val successCallback = android.app.PendingIntent.getBroadcast(
                this, 0, configIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val result = appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            
            if (result) {
                Toast.makeText(this, "Widget will be added to home screen", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "Unable to add widget. Please try adding from widgets menu.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Please add widget from home screen widgets menu", Toast.LENGTH_LONG).show()
        }
    }
}
