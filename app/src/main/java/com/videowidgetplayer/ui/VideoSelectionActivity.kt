package com.videowidgetplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.videowidgetplayer.R
import com.videowidgetplayer.adapters.VideoSelectionAdapter
import com.videowidgetplayer.data.VideoFile
import com.videowidgetplayer.data.VideoRepository
import com.videowidgetplayer.utils.PreferenceUtils
import kotlinx.coroutines.launch

class VideoSelectionActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoSelectionAdapter
    private lateinit var emptyView: TextView
    private lateinit var loadingView: View
    private lateinit var confirmButton: Button
    private lateinit var cancelButton: Button
    private lateinit var selectionCountText: TextView
    
    private lateinit var videoRepository: VideoRepository
    private val selectedVideos = mutableSetOf<VideoFile>()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            loadVideos()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_selection)
        
        initializeViews()
        setupRecyclerView()
        setupButtons()
        
        videoRepository = VideoRepository(this)
        
        checkPermissionsAndLoadVideos()
    }
    
    private fun initializeViews() {
        recyclerView = findViewById(R.id.videos_recycler_view)
        emptyView = findViewById(R.id.empty_view)
        loadingView = findViewById(R.id.loading_view)
        confirmButton = findViewById(R.id.confirm_button)
        cancelButton = findViewById(R.id.cancel_button)
        selectionCountText = findViewById(R.id.selection_count_text)
        
        supportActionBar?.apply {
            title = getString(R.string.select_videos)
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupRecyclerView() {
        adapter = VideoSelectionAdapter(
            onVideoClick = { video ->
                toggleVideoSelection(video)
            },
            onVideoLongClick = { video ->
                showVideoDetailsDialog(video)
                true
            }
        )
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@VideoSelectionActivity, 2)
            adapter = this@VideoSelectionActivity.adapter
        }
    }
    
    private fun setupButtons() {
        confirmButton.setOnClickListener {
            if (selectedVideos.isNotEmpty()) {
                showConfirmationDialog()
            } else {
                Toast.makeText(this, R.string.no_videos_selected, Toast.LENGTH_SHORT).show()
            }
        }
        
        cancelButton.setOnClickListener {
            if (selectedVideos.isNotEmpty()) {
                showCancelConfirmationDialog()
            } else {
                finish()
            }
        }
        
        updateSelectionUI()
    }
    
    private fun checkPermissionsAndLoadVideos() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            loadVideos()
        } else {
            if (shouldShowRequestPermissionRationale(missingPermissions.first())) {
                showPermissionRationaleDialog(missingPermissions.toTypedArray())
            } else {
                requestPermissionLauncher.launch(missingPermissions.toTypedArray())
            }
        }
    }
    
    private fun loadVideos() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val videos = videoRepository.getShortVideos(maxDurationSeconds = 60)
                
                runOnUiThread {
                    showLoading(false)
                    
                    if (videos.isEmpty()) {
                        showEmptyState()
                    } else {
                        showVideos(videos)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    showLoading(false)
                    showErrorDialog(e.message ?: getString(R.string.error_loading_videos))
                }
            }
        }
    }
    
    private fun showVideos(videos: List<VideoFile>) {
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        adapter.submitList(videos)
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.VISIBLE
        emptyView.text = getString(R.string.no_short_videos_found)
    }
    
    private fun showLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
        emptyView.visibility = View.GONE
    }
    
    private fun toggleVideoSelection(video: VideoFile) {
        if (selectedVideos.contains(video)) {
            selectedVideos.remove(video)
        } else {
            selectedVideos.add(video)
        }
        
        adapter.updateSelection(selectedVideos)
        updateSelectionUI()
    }
    
    private fun updateSelectionUI() {
        val count = selectedVideos.size
        selectionCountText.text = resources.getQuantityString(
            R.plurals.videos_selected_count,
            count,
            count
        )
        
        confirmButton.isEnabled = count > 0
        confirmButton.text = if (count > 0) {
            getString(R.string.confirm_selection_with_count, count)
        } else {
            getString(R.string.confirm_selection)
        }
    }
    
    private fun showConfirmationDialog() {
        val message = resources.getQuantityString(
            R.plurals.confirm_video_selection_message,
            selectedVideos.size,
            selectedVideos.size
        )
        
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_selection)
            .setMessage(message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                saveSelectedVideos()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.discard_selection)
            .setMessage(R.string.discard_selection_message)
            .setPositiveButton(R.string.discard) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.keep_selecting, null)
            .show()
    }
    
    private fun showVideoDetailsDialog(video: VideoFile) {
        val details = buildString {
            appendLine("${getString(R.string.file_name)}: ${video.displayName}")
            appendLine("${getString(R.string.duration)}: ${formatDuration(video.duration)}")
            appendLine("${getString(R.string.size)}: ${formatFileSize(video.size)}")
            appendLine("${getString(R.string.resolution)}: ${video.width}x${video.height}")
            appendLine("${getString(R.string.path)}: ${video.data}")
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.video_details))
            .setMessage(details)
            .setPositiveButton(R.string.close, null)
            .show()
    }
    
    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.video_permission_rationale)
            .setPositiveButton(R.string.grant_permission) { _, _ ->
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                // TODO: Open app settings
                finish()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(message)
            .setPositiveButton(R.string.retry) { _, _ ->
                loadVideos()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                finish()
            }
            .show()
    }
    
    private fun saveSelectedVideos() {
        try {
            val videoUris = selectedVideos.map { it.uri.toString() }
            PreferenceUtils.saveSelectedVideoUris(this, videoUris)
            
            Toast.makeText(
                this,
                resources.getQuantityString(
                    R.plurals.videos_saved_message,
                    selectedVideos.size,
                    selectedVideos.size
                ),
                Toast.LENGTH_LONG
            ).show()
            
            setResult(RESULT_OK)
            finish()
            
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error_saving_videos, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
    
    private fun formatFileSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$sizeBytes B"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onBackPressed() {
        if (selectedVideos.isNotEmpty()) {
            showCancelConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }
}
