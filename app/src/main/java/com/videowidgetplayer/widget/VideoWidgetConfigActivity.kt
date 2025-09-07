package com.videowidgetplayer.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.videowidgetplayer.R
import com.videowidgetplayer.adapter.VideoSelectionAdapter
import com.videowidgetplayer.data.VideoFile
import com.videowidgetplayer.data.VideoRepository
import com.videowidgetplayer.databinding.ActivityWidgetConfigBinding
import com.videowidgetplayer.utils.WidgetPreferences
import kotlinx.coroutines.launch

/**
 * Widget configuration activity
 * Following the spec: Configure widget with selected videos (≤60 seconds)
 */
class VideoWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigBinding
    private lateinit var videoRepository: VideoRepository
    private lateinit var videoAdapter: VideoSelectionAdapter
    private lateinit var widgetPrefs: WidgetPreferences
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val selectedVideos = mutableListOf<VideoFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the result to CANCELED. This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setupViews()
        setupRecyclerView()
        loadVideos()
    }

    private fun setupViews() {
        videoRepository = VideoRepository(this)
        widgetPrefs = WidgetPreferences(this)

        binding.addButton.setOnClickListener {
            if (selectedVideos.isNotEmpty()) {
                configureWidget()
            }
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }

        updateAddButton()
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoSelectionAdapter { video, isSelected ->
            if (isSelected) {
                selectedVideos.add(video)
            } else {
                selectedVideos.removeAll { it.id == video.id }
            }
            updateAddButton()
        }

        binding.videoRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@VideoWidgetConfigActivity)
            adapter = videoAdapter
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
                showEmptyState()
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
    }

    private fun updateAddButton() {
        val hasSelection = selectedVideos.isNotEmpty()
        binding.addButton.isEnabled = hasSelection
        binding.addButton.text = if (hasSelection) {
            "Add Widget (${selectedVideos.size} videos)"
        } else {
            "Select videos first"
        }
    }

    private fun configureWidget() {
        val videoUris = selectedVideos.map { it.uri }
        
        // Save widget configuration
        widgetPrefs.saveVideoUris(appWidgetId, videoUris)
        widgetPrefs.saveCurrentVideoIndex(appWidgetId, 0)
        widgetPrefs.savePlayingState(appWidgetId, false)
        widgetPrefs.saveMutedState(appWidgetId, false)

        // Update the widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        VideoWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

        // Return success
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}
