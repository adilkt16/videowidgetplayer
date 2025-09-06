package com.videowidgetplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.videowidgetplayer.data.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaUtils {
    
    private const val TAG = "MediaUtils"
    
    /**
     * Get all video files from device storage
     */
    suspend fun getVideoFiles(context: Context): List<VideoFile> = withContext(Dispatchers.IO) {
        val videoFiles = mutableListOf<VideoFile>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )
        
        val selection = "${MediaStore.Video.Media.DURATION} > ?"
        val selectionArgs = arrayOf("0") // Filter out 0-duration videos
        
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "Unknown"
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val path = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: ""
                    
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    videoFiles.add(
                        VideoFile(
                            id = id,
                            displayName = name,
                            uri = contentUri,
                            duration = duration,
                            size = size,
                            data = path,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            mimeType = mimeType
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing video file", e)
                }
            }
        }
        
        videoFiles
    }
    
    /**
     * Get video files filtered by maximum duration
     * @param context Android context
     * @param maxDurationSeconds Maximum duration in seconds
     */
    suspend fun getShortVideoFiles(context: Context, maxDurationSeconds: Int): List<VideoFile> = 
        withContext(Dispatchers.IO) {
        val videoFiles = mutableListOf<VideoFile>()
        val maxDurationMs = maxDurationSeconds * 1000L
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )
        
        val selection = "${MediaStore.Video.Media.DURATION} > ? AND ${MediaStore.Video.Media.DURATION} <= ?"
        val selectionArgs = arrayOf("0", maxDurationMs.toString())
        
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Video.Media.DATE_ADDED + " DESC"
        )
        
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "Unknown"
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val path = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: ""
                    
                    // Double-check duration is within limits
                    if (duration <= maxDurationMs) {
                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        
                        videoFiles.add(
                            VideoFile(
                                id = id,
                                displayName = name,
                                uri = contentUri,
                                duration = duration,
                                size = size,
                                data = path,
                                width = width,
                                height = height,
                                dateAdded = dateAdded,
                                mimeType = mimeType
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing short video file", e)
                }
            }
        }
        
        Log.d(TAG, "Found ${videoFiles.size} videos ≤ $maxDurationSeconds seconds")
        videoFiles
    }
    
    /**
     * Get a specific video by ID
     */
    suspend fun getVideoById(context: Context, videoId: Long): VideoFile? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )
        
        val selection = "${MediaStore.Video.Media._ID} = ?"
        val selectionArgs = arrayOf(videoId.toString())
        
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        
        cursor?.use { c ->
            if (c.moveToFirst()) {
                try {
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "Unknown"
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val path = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: ""
                    
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    return@withContext VideoFile(
                        id = id,
                        displayName = name,
                        uri = contentUri,
                        duration = duration,
                        size = size,
                        data = path,
                        width = width,
                        height = height,
                        dateAdded = dateAdded,
                        mimeType = mimeType
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting video by ID: $videoId", e)
                }
            }
        }
        
        null
    }
    
    /**
     * Get videos by URIs
     */
    suspend fun getVideosByUris(context: Context, uris: List<String>): List<VideoFile> = 
        withContext(Dispatchers.IO) {
        val videoFiles = mutableListOf<VideoFile>()
        
        uris.forEach { uriString ->
            try {
                val uri = Uri.parse(uriString)
                val video = getVideoByUri(context, uri)
                if (video != null) {
                    videoFiles.add(video)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting video by URI: $uriString", e)
            }
        }
        
        videoFiles
    }
    
    /**
     * Get a video by URI
     */
    private fun getVideoByUri(context: Context, uri: Uri): VideoFile? {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.MIME_TYPE
        )
        
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        
        return cursor?.use { c ->
            if (c.moveToFirst()) {
                try {
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                    
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn) ?: "Unknown"
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val path = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: ""
                    
                    VideoFile(
                        id = id,
                        displayName = name,
                        uri = uri,
                        duration = duration,
                        size = size,
                        data = path,
                        width = width,
                        height = height,
                        dateAdded = dateAdded,
                        mimeType = mimeType
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing video from URI: $uri", e)
                    null
                }
            } else null
        }
    }
    
    /**
     * Check if a video file is accessible
     */
    suspend fun isVideoAccessible(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { 
                return@withContext true 
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "Video not accessible: $uri", e)
            false
        }
    }
    
    /**
     * Get video thumbnail
     */
    suspend fun getVideoThumbnail(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(0)
            retriever.release()
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Error getting thumbnail for: $uri", e)
            null
        }
    }
    
    /**
     * Format duration from milliseconds to human readable string
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Format file size to human readable string
     */
    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes < 1024) return "$sizeBytes B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }
    
    /**
     * Check if file exists at given path
     */
    fun fileExists(path: String): Boolean {
        return try {
            File(path).exists()
        } catch (e: Exception) {
            false
        }
    }
}
