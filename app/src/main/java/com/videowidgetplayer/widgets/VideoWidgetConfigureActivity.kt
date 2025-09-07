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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.videowidgetplayer.R
import com.videowidgetplayer.databinding.ActivityVideoWidgetConfigureBinding
import com.videowidgetplayer.utils.PreferenceUtils

class VideoWidgetConfigureActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VideoWidgetConfigure"
    }
    
    private lateinit var binding: ActivityVideoWidgetConfigureBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val selectedVideoUris = mutableListOf<String>()
    
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
    }
    
    private fun setupUI() {
        binding.addButton.setOnClickListener {
            if (selectedVideoUris.isEmpty()) {
                Toast.makeText(this, "Please select at least one video", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            configureWidget()
        }
        
        binding.selectVideoButton.setOnClickListener {
            selectVideosForWidget()
        }
        
        // Set up gesture settings if available in layout
        setupGestureSettings()
        
        // Update UI based on selected videos
        updateVideoSelectionDisplay()
    }
    
    private fun setupGestureSettings() {
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
        } else {
            binding.selectVideoButton.text = getString(R.string.select_videos)
            binding.addButton.isEnabled = false
        }
    }
    
    private fun configureWidget() {
        Log.d(TAG, "Configuring widget $appWidgetId with ${selectedVideoUris.size} videos")
        
        try {
            val context = this@VideoWidgetConfigureActivity
            
            // Initialize the video queue for the widget
            val videoManager = WidgetVideoManager.getInstance()
            videoManager.initialize(context, appWidgetId)
            videoManager.initializeVideoQueue(context, appWidgetId)
            
            // Save the selected video queue
            PreferenceUtils.setWidgetVideoQueue(context, appWidgetId, selectedVideoUris)
            
            // Apply gesture settings
            applyGestureSettings(context, videoManager)
            
            // Configure the widget
            val appWidgetManager = AppWidgetManager.getInstance(context)
            VideoWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
            
            // Return success result
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring widget", e)
            Toast.makeText(this, "Error configuring widget", Toast.LENGTH_SHORT).show()
        }
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
}
