package com.videowidgetplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.videowidgetplayer.R
import com.videowidgetplayer.databinding.ActivityMainBinding
import com.videowidgetplayer.utils.PreferenceUtils

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeApp()
        } else {
            // Handle permission denial
            showPermissionDeniedMessage()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        checkAndRequestPermissions()
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else {
            // Pre-Android 13 permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            initializeApp()
        }
    }
    
    private fun initializeApp() {
        // Initialize the main UI and functionality
        setupUI()
    }
    
    private fun setupUI() {
        updateVideoSelectionUI()
        
        binding.setupWidgetButton.setOnClickListener {
            // Navigate to widget setup instruction
            showWidgetSetupInstructions()
        }
        binding.browseVideosButton.setOnClickListener {
            // Open video browser
            openVideoBrowser()
        }
    }
    
    private fun updateVideoSelectionUI() {
        val selectedCount = PreferenceUtils.getAppSelectedVideosCount(this)
        if (selectedCount > 0) {
            binding.welcomeText.text = "Video Widget Player\n\n✅ $selectedCount video(s) selected for widgets"
            binding.browseVideosButton.text = "Change Selected Videos ($selectedCount)"
        } else {
            binding.welcomeText.text = "Video Widget Player\n\n📹 Select videos to use in your widgets"
            binding.browseVideosButton.text = "Select Videos for Widgets"
        }
    }
    
    private fun showWidgetSetupInstructions() {
        android.util.Log.d("MainActivity", "Setup Widget button clicked")
        val selectedCount = PreferenceUtils.getAppSelectedVideosCount(this)
        
        if (selectedCount == 0) {
            android.app.AlertDialog.Builder(this)
                .setTitle("No Videos Selected")
                .setMessage("Please select videos first using the 'Select Videos for Widgets' button, then you can add widgets to your home screen.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            android.app.AlertDialog.Builder(this)
                .setTitle("Setup Video Widget")
                .setMessage("Great! You have $selectedCount video(s) selected.\n\nTo add a Video Widget:\n\n1. Long-press on your home screen\n2. Select 'Widgets'\n3. Find 'Video Widget Player'\n4. Drag the widget to your home screen\n\nThe widget will automatically use your selected videos!")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
    
    private fun openVideoBrowser() {
        android.util.Log.d("MainActivity", "Browse Videos button clicked")
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                type = "video/*"
                putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            videoBrowserLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error opening video browser", e)
            android.widget.Toast.makeText(this, "Error opening video browser: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    private val videoBrowserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("MainActivity", "Video browser result: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val selectedUris = mutableListOf<String>()
                
                val clipData = data.clipData
                if (clipData != null) {
                    // Multiple videos selected
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedUris.add(uri.toString())
                    }
                    android.util.Log.d("MainActivity", "Selected ${selectedUris.size} videos")
                } else {
                    // Single video selected
                    val uri = data.data
                    if (uri != null) {
                        selectedUris.add(uri.toString())
                        android.util.Log.d("MainActivity", "Selected video: $uri")
                    }
                }
                
                if (selectedUris.isNotEmpty()) {
                    // Save selected videos to app preferences
                    PreferenceUtils.saveAppSelectedVideos(this, selectedUris)
                    
                    // Update UI
                    updateVideoSelectionUI()
                    
                    android.widget.Toast.makeText(this, "✅ Selected ${selectedUris.size} video(s) for widgets", android.widget.Toast.LENGTH_LONG).show()
                    
                    // Show success dialog
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Videos Selected!")
                        .setMessage("${selectedUris.size} video(s) selected successfully!\n\nYou can now add widgets to your home screen and they will use these videos automatically.")
                        .setPositiveButton("Add Widget Now") { dialog, _ -> 
                            dialog.dismiss()
                            showWidgetSetupInstructions()
                        }
                        .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }
    
    private fun showPermissionDeniedMessage() {
        binding.welcomeText.text = "Media access permissions are required for this app to function properly."
    }
}
