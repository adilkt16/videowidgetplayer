package com.videowidgetplayer.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import androidx.recyclerview.widget.LinearLayoutManager
import com.videowidgetplayer.R
import com.videowidgetplayer.adapters.ConfigVideoAdapter
import com.videowidgetplayer.databinding.ActivityVideoWidgetConfigureBinding
import com.videowidgetplayer.ui.MainActivity
import com.videowidgetplayer.utils.PreferenceUtils

class VideoWidgetConfigureActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VideoWidgetConfigure"
    }
    
    private lateinit var binding: ActivityVideoWidgetConfigureBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val selectedVideoUris = mutableListOf<String>()
    private lateinit var configVideoAdapter: ConfigVideoAdapter
    private var exoPlayer: ExoPlayer? = null
    
    // Register for multiple video selection
    private val videoSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        handleVideoSelection(uris)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)
        
        binding = ActivityVideoWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get the widget ID from the intent
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        
        // If the intent doesn't contain a valid widget ID, finish the activity
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        
        setupUI()
        loadAppSelectedVideos()
    }
    
    private fun setupUI() {
        binding.addButton.setOnClickListener {
            if (selectedVideoUris.isEmpty()) {
                showMainAppRedirectDialog()
                return@setOnClickListener
            }
            
            configureWidget()
        }

        binding.selectVideoButton.setOnClickListener {
            showMainAppRedirectDialog()
        }
        
        // Setup cancel button
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        // Initialize RecyclerView and adapter
        setupSelectedVideosRecyclerView()

        // Set up gesture settings if available in layout
        setupGestureSettings()

        // Update UI based on selected videos
        updateVideoSelectionDisplay()
    }
    
    private fun setupSelectedVideosRecyclerView() {
        configVideoAdapter = ConfigVideoAdapter(
            onVideoPlay = { videoUri ->
                playVideo(videoUri)
            },
            onVideoRemove = { videoUri ->
                removeVideoFromSelection(videoUri)
            }
        )
        
        binding.selectedVideosRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@VideoWidgetConfigureActivity)
            adapter = configVideoAdapter
        }
    }
    
    private fun playVideo(videoUri: String) {
        try {
            // Release previous player if exists
            exoPlayer?.release()
            
            // Create new player
            exoPlayer = ExoPlayer.Builder(this).build()
            
            // Create media item
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUri))
            exoPlayer?.setMediaItem(mediaItem)
            
            // Prepare and play
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
            
            // Add listener for completion
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        exoPlayer?.seekTo(0)
                        exoPlayer?.pause()
                    }
                }
            })
            
            Toast.makeText(this, "Playing video preview", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video preview", e)
            Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeVideoFromSelection(videoUri: String) {
        AlertDialog.Builder(this)
            .setTitle("Remove Video")
            .setMessage("Remove this video from the widget selection?")
            .setPositiveButton("Remove") { _, _ ->
                selectedVideoUris.remove(videoUri)
                updateVideoSelectionDisplay()
                Toast.makeText(this, "Video removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }    private fun setupGestureSettings() {
        try {
            // Check if gesture settings views exist
            val gestureToggle = binding.root.findViewById<android.widget.Switch>(R.id.gesture_enabled_switch)
            val sensitivitySpinner = binding.root.findViewById<android.widget.Spinner>(R.id.gesture_sensitivity_spinner)
            
            if (gestureToggle != null) {
                // Set default gesture enabled state
                gestureToggle.isChecked = true
                
                gestureToggle.setOnCheckedChangeListener { _, isChecked ->
                    Log.d(TAG, "Gesture enabled changed: $isChecked")
                    sensitivitySpinner?.isEnabled = isChecked
                }
            }
            
            if (sensitivitySpinner != null) {
                // Setup sensitivity options
                val sensitivities = arrayOf("Low", "Medium", "High")
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, sensitivities)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                sensitivitySpinner.adapter = adapter
                sensitivitySpinner.setSelection(1) // Default to Medium
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "Gesture settings not available in layout", e)
        }
    }
    
    private fun loadAppSelectedVideos() {
        Log.d(TAG, "Loading videos selected in main app...")
        selectedVideoUris.clear()
        selectedVideoUris.addAll(PreferenceUtils.getAppSelectedVideos(this))
        
        Log.d(TAG, "Loaded ${selectedVideoUris.size} videos from app selection")
        selectedVideoUris.forEachIndexed { index, uri ->
            Log.d(TAG, "App video $index: $uri")
        }
        
        updateVideoSelectionDisplay()
    }
    
    private fun showMainAppRedirectDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Videos in Main App")
            .setMessage("Please select videos in the main Video Widget Player app first.\n\nWould you like to open the main app now?")
            .setPositiveButton("Open Main App") { dialog, _ ->
                dialog.dismiss()
                openMainApp()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }
    
    private fun openMainApp() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening main app", e)
            Toast.makeText(this, "Could not open main app", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun selectVideosForWidget() {
        Log.d(TAG, "Opening video selection")
        
        try {
            // Launch video selection for multiple files
            videoSelectionLauncher.launch("video/*")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening video selection", e)
            Toast.makeText(this, "Error opening video selection", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleVideoSelection(uris: List<Uri>) {
        Log.d(TAG, "=== VIDEO SELECTION DEBUG ===")
        Log.d(TAG, "Selected ${uris.size} videos")
        
        selectedVideoUris.clear()
        
        for (uri in uris) {
            try {
                // Take persistent permission for the URI
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                selectedVideoUris.add(uri.toString())
                Log.d(TAG, "Added video: $uri")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing video URI: $uri", e)
            }
        }
        
        Log.d(TAG, "Total videos selected: ${selectedVideoUris.size}")
        selectedVideoUris.forEachIndexed { index, uri ->
            Log.d(TAG, "Video $index: $uri")
        }
        
        updateVideoSelectionDisplay()
        
        if (selectedVideoUris.isNotEmpty()) {
            Toast.makeText(this, 
                "Selected ${selectedVideoUris.size} video(s)", 
                Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateVideoSelectionDisplay() {
        val videoCount = selectedVideoUris.size
        if (videoCount > 0) {
            binding.selectVideoButton.text = getString(R.string.videos_selected, videoCount)
            binding.addButton.isEnabled = true
            
            // Show videos list, hide empty message
            binding.selectedVideosRecyclerView.visibility = android.view.View.VISIBLE
            binding.noVideosText.visibility = android.view.View.GONE
            
            // Update adapter
            configVideoAdapter.updateVideos(selectedVideoUris)
        } else {
            binding.selectVideoButton.text = getString(R.string.select_videos)
            binding.addButton.isEnabled = false
            
            // Show empty message, hide videos list
            binding.selectedVideosRecyclerView.visibility = android.view.View.GONE
            binding.noVideosText.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun configureWidget() {
        Log.d(TAG, "=== WIDGET CONFIGURATION START ===")
        Log.d(TAG, "Configuring widget $appWidgetId with ${selectedVideoUris.size} videos")
        
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e(TAG, "INVALID WIDGET ID")
            return
        }
        
        // Reload app-selected videos to ensure we have the latest
        loadAppSelectedVideos()
        
        if (selectedVideoUris.isEmpty()) {
            Log.e(TAG, "NO VIDEOS SELECTED IN APP")
            Toast.makeText(this, "No videos selected in main app. Please select videos first.", Toast.LENGTH_LONG).show()
            showMainAppRedirectDialog()
            return
        }
        
        try {
            val context = this@VideoWidgetConfigureActivity
            
            // Initialize the video queue for the widget
            Log.d(TAG, "Initializing video manager...")
            val videoManager = WidgetVideoManager.getInstance()
            videoManager.initialize(context, appWidgetId)
            videoManager.initializeVideoQueue(context, appWidgetId)
            
            // Save the selected video queue
            Log.d(TAG, "Saving video queue...")
            PreferenceUtils.setWidgetVideoQueue(context, appWidgetId, selectedVideoUris)
            
            // Verify queue was saved
            val savedQueue = PreferenceUtils.getWidgetVideoQueue(context, appWidgetId)
            Log.d(TAG, "VERIFICATION: Saved queue size = ${savedQueue.size}")
            
            // Set the first video as current and save for backward compatibility
            if (selectedVideoUris.isNotEmpty()) {
                Log.d(TAG, "Setting first video as current...")
                PreferenceUtils.setWidgetCurrentVideoIndex(context, appWidgetId, 0)
                PreferenceUtils.saveWidgetVideoUri(context, appWidgetId, selectedVideoUris[0])
                
                // Extract and save video title
                val videoTitle = getVideoTitle(context, selectedVideoUris[0])
                PreferenceUtils.saveWidgetTitle(context, appWidgetId, videoTitle)
                
                Log.d(TAG, "FIRST VIDEO: ${selectedVideoUris[0]}")
                Log.d(TAG, "VIDEO TITLE: $videoTitle")
                Log.d(TAG, "Configured widget $appWidgetId with ${selectedVideoUris.size} videos")
            } else {
                Log.w(TAG, "No videos selected for widget $appWidgetId")
            }
            
            // Apply gesture settings
            Log.d(TAG, "Applying gesture settings...")
            applyGestureSettings(context, videoManager)
            
            // Configure the widget
            Log.d(TAG, "Calling VideoWidgetProvider.updateAppWidget...")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            VideoWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
            
            // Return success result
            Log.d(TAG, "Widget configuration COMPLETED SUCCESSFULLY")
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR in configureWidget", e)
            Toast.makeText(this, "Error configuring widget", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "=== WIDGET CONFIGURATION END ===")
    }
    
    private fun applyGestureSettings(context: Context, videoManager: WidgetVideoManager) {
        try {
            val gestureToggle = binding.root.findViewById<android.widget.Switch>(R.id.gesture_enabled_switch)
            val sensitivitySpinner = binding.root.findViewById<android.widget.Spinner>(R.id.gesture_sensitivity_spinner)
            
            val gestureEnabled = gestureToggle?.isChecked ?: true
            val sensitivityLevel = sensitivitySpinner?.selectedItemPosition ?: WidgetGestureManager.SENSITIVITY_MEDIUM
            
            if (gestureEnabled) {
                videoManager.enableGestureSupport(context, appWidgetId)
                videoManager.setGestureSensitivity(context, sensitivityLevel.toFloat())
                Log.d(TAG, "Enabled gestures with sensitivity level $sensitivityLevel")
            } else {
                videoManager.disableGestureSupport(context, appWidgetId)
                Log.d(TAG, "Disabled gestures for widget")
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply gesture settings, using defaults", e)
            // Enable gestures by default if settings are not available
            videoManager.enableGestureSupport(context, appWidgetId)
        }
    }
    
    private fun getVideoTitle(context: Context, videoUri: String): String {
        return try {
            val uri = Uri.parse(videoUri)
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.TITLE),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val titleIndex = it.getColumnIndex(MediaStore.Video.Media.TITLE)
                    
                    when {
                        displayNameIndex >= 0 -> it.getString(displayNameIndex) ?: "Unknown Video"
                        titleIndex >= 0 -> it.getString(titleIndex) ?: "Unknown Video"
                        else -> "Unknown Video"
                    }
                } else {
                    "Unknown Video"
                }
            } ?: "Unknown Video"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video title", e)
            "Unknown Video"
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
