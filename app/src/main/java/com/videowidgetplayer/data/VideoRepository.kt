package com.videowidgetplayer.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for accessing video files from device gallery
 * Following the spec: Gallery integration with 60-second filtering
 */
class VideoRepository(private val context: Context) {

    /**
     * Get all videos from device gallery, filtered for widget compatibility (≤60 seconds)
     */
    suspend fun getVideosFromGallery(): List<VideoFile> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoFile>()
        
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        )
        
        val selection = "${MediaStore.Video.Media.DURATION} <= ?" // Only videos ≤60 seconds
        val selectionArgs = arrayOf("60000") // 60 seconds in milliseconds
        
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                
                // Only include videos that meet widget requirements
                if (duration <= 60_000 && duration > 0) {
                    videoList.add(
                        VideoFile(
                            id = id,
                            name = name,
                            uri = contentUri,
                            duration = duration,
                            size = size,
                            thumbnailUri = getThumbnailUri(id)
                        )
                    )
                }
            }
        }
        
        videoList
    }
    
    /**
     * Get thumbnail URI for a video
     */
    private fun getThumbnailUri(videoId: Long): Uri {
        return Uri.withAppendedPath(
            MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
            videoId.toString()
        )
    }
    
    /**
     * Get video file by URI
     */
    suspend fun getVideoByUri(uri: Uri): VideoFile? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val duration = it.getLong(durationColumn)
                val size = it.getLong(sizeColumn)
                
                // Only return if video meets widget requirements
                if (duration <= 60_000 && duration > 0) {
                    return@withContext VideoFile(
                        id = id,
                        name = name,
                        uri = uri,
                        duration = duration,
                        size = size,
                        thumbnailUri = getThumbnailUri(id)
                    )
                }
            }
        }
        
        null
    }
}
