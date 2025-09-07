package com.videowidgetplayer.data

import android.net.Uri

/**
 * Data class representing a video file with widget-specific information
 * Following the spec: Videos must be ≤60 seconds for widget use
 */
data class VideoFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val duration: Long, // in milliseconds
    val size: Long,
    val thumbnailUri: Uri? = null,
    val isSelected: Boolean = false
) {
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
