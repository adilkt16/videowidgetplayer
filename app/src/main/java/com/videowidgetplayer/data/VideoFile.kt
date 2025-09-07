package com.videowidgetplayer.data

import android.net.Uri
import com.google.gson.annotations.SerializedName

/**
 * Data class representing a video file with widget-specific information
 * Following the spec: Videos must be ≤60 seconds for widget use
 */
data class VideoFile(
    val id: Long,
    val name: String,
    @SerializedName("uriString")
    private val _uriString: String,
    val duration: Long, // in milliseconds
    val size: Long,
    @SerializedName("thumbnailUriString")
    private val _thumbnailUriString: String? = null,
    val isSelected: Boolean = false
) {
    
    // Computed properties that convert strings back to Uri objects
    val uri: Uri
        get() = Uri.parse(_uriString)
    
    val thumbnailUri: Uri?
        get() = _thumbnailUriString?.let { Uri.parse(it) }
    
    // Secondary constructor for creating from Uri objects
    constructor(
        id: Long,
        name: String,
        uri: Uri,
        duration: Long,
        size: Long,
        thumbnailUri: Uri? = null,
        isSelected: Boolean = false
    ) : this(
        id = id,
        name = name,
        _uriString = uri.toString(),
        duration = duration,
        size = size,
        _thumbnailUriString = thumbnailUri?.toString(),
        isSelected = isSelected
    )
    /**
     * Check if video meets widget requirements (≤60 seconds)
     */
    fun isValidForWidget(): Boolean {
        return duration <= 60_000 // 60 seconds in milliseconds
    }

    /**
     * Get duration in human-readable format
     */
    fun getDurationFormatted(): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (minutes > 0) {
            String.format("%d:%02d", minutes, remainingSeconds)
        } else {
            String.format("0:%02d", remainingSeconds)
        }
    }

    /**
     * Get file size in human-readable format
     */
    fun getSizeFormatted(): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> String.format("%d B", size)
        }
    }
}
