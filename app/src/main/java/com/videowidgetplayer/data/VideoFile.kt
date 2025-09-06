package com.videowidgetplayer.data

import android.net.Uri

data class VideoFile(
    val id: Long,
    val displayName: String,
    val uri: Uri,
    val duration: Long, // Duration in milliseconds
    val size: Long, // Size in bytes
    val data: String, // File path
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0, // Unix timestamp
    val mimeType: String = ""
) {
    // Legacy compatibility
    val name: String get() = displayName
    val path: String get() = data
    
    /**
     * Get duration in seconds
     */
    val durationSeconds: Int get() = (duration / 1000).toInt()
    
    /**
     * Check if this is a short video (≤ 60 seconds)
     */
    val isShortVideo: Boolean get() = durationSeconds <= 60
    
    /**
     * Get formatted duration string
     */
    val formattedDuration: String get() {
        val seconds = durationSeconds % 60
        val minutes = (durationSeconds / 60) % 60
        val hours = durationSeconds / 3600
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Get formatted file size string
     */
    val formattedSize: String get() {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1.0 -> String.format("%.1f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$size B"
        }
    }
    
    /**
     * Get resolution string
     */
    val resolution: String get() = if (width > 0 && height > 0) "${width}x${height}" else "Unknown"
    
    /**
     * Check if video has valid dimensions
     */
    val hasValidDimensions: Boolean get() = width > 0 && height > 0
}
