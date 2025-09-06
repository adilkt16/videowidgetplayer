package com.videowidgetplayer.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.videowidgetplayer.data.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaUtils {
    
    companion object {
        suspend fun getVideoFiles(context: Context): List<VideoFile> = withContext(Dispatchers.IO) {
            val videoFiles = mutableListOf<VideoFile>()
            
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
            )
            
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                
                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn)
                    val duration = c.getLong(durationColumn)
                    val size = c.getLong(sizeColumn)
                    val path = c.getString(dataColumn)
                    
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    
                    videoFiles.add(
                        VideoFile(
                            id = id,
                            name = name,
                            uri = contentUri,
                            duration = duration,
                            size = size,
                            path = path
                        )
                    )
                }
            }
            
            videoFiles
        }
        
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
        
        fun formatFileSize(sizeBytes: Long): String {
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.1f GB", gb)
        }
    }
}
