package com.videowidgetplayer.service

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/**
 * Advanced frame extractor for optimal video performance
 * Uses multiple extraction methods for best quality
 */
class AdvancedFrameExtractor {
    
    companion object {
        private const val TAG = "AdvancedFrameExtractor"
        private const val MAX_EXTRACTION_TIME_MS = 15000L // 15 seconds max
        private const val PREFERRED_WIDTH = 400
        private const val PREFERRED_HEIGHT = 300
    }
    
    /**
     * Extract frames using multiple techniques for optimal quality
     */
    suspend fun extractHighQualityFrames(
        context: android.content.Context,
        videoUri: Uri,
        targetFrameCount: Int,
        maxDimension: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        
        val frames = mutableListOf<Bitmap>()
        var retriever: MediaMetadataRetriever? = null
        
        try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // Get comprehensive video metadata
            val metadata = extractVideoMetadata(retriever)
            if (metadata.duration <= 0) {
                Log.w(TAG, "Invalid video duration")
                return@withContext frames
            }
            
            // Calculate optimal extraction parameters
            val extractionParams = calculateOptimalExtractionParams(
                metadata, targetFrameCount, maxDimension
            )
            
            Log.d(TAG, "Extracting ${extractionParams.frameCount} frames from ${metadata.duration}ms video")
            Log.d(TAG, "Target dimensions: ${extractionParams.targetWidth}x${extractionParams.targetHeight}")
            
            // Extract frames with multiple quality options
            frames.addAll(
                extractFramesWithMultipleOptions(
                    retriever, 
                    metadata, 
                    extractionParams
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in high-quality frame extraction", e)
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing retriever", e)
            }
        }
        
        Log.d(TAG, "Successfully extracted ${frames.size} high-quality frames")
        frames
    }
    
    private fun extractVideoMetadata(retriever: MediaMetadataRetriever): VideoMetadata {
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 640
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 480
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
        val frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull() ?: 24f
        
        return VideoMetadata(duration, width, height, rotation, bitrate, frameRate)
    }
    
    private fun calculateOptimalExtractionParams(
        metadata: VideoMetadata,
        targetFrameCount: Int,
        maxDimension: Int
    ): ExtractionParams {
        
        // Limit extraction time for performance
        val maxDuration = minOf(metadata.duration, MAX_EXTRACTION_TIME_MS)
        
        // Calculate frame count based on video length and target
        val optimalFrameCount = minOf(
            targetFrameCount,
            (maxDuration * metadata.frameRate / 1000f).toInt(),
            300 // Hard limit for performance
        ).coerceAtLeast(10) // Minimum frames
        
        // Calculate interval between frames
        val frameIntervalMs = maxDuration / optimalFrameCount.coerceAtLeast(1)
        
        // Calculate optimal dimensions maintaining aspect ratio
        val aspectRatio = metadata.width.toFloat() / metadata.height.toFloat()
        val (targetWidth, targetHeight) = when {
            aspectRatio > 1.5f -> {
                // Wide video
                val width = minOf(maxDimension, PREFERRED_WIDTH)
                val height = (width / aspectRatio).toInt()
                Pair(width, height)
            }
            aspectRatio < 0.75f -> {
                // Tall video
                val height = minOf(maxDimension, PREFERRED_HEIGHT)
                val width = (height * aspectRatio).toInt()
                Pair(width, height)
            }
            else -> {
                // Normal ratio
                val scale = minOf(
                    maxDimension.toFloat() / metadata.width,
                    maxDimension.toFloat() / metadata.height
                )
                val width = (metadata.width * scale).toInt()
                val height = (metadata.height * scale).toInt()
                Pair(width, height)
            }
        }
        
        return ExtractionParams(
            frameCount = optimalFrameCount,
            frameIntervalMs = frameIntervalMs,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            maxDuration = maxDuration
        )
    }
    
    private suspend fun extractFramesWithMultipleOptions(
        retriever: MediaMetadataRetriever,
        metadata: VideoMetadata,
        params: ExtractionParams
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        
        val frames = mutableListOf<Bitmap>()
        
        // Try different extraction options for best quality
        val extractionOptions = listOf(
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            MediaMetadataRetriever.OPTION_CLOSEST,
            MediaMetadataRetriever.OPTION_NEXT_SYNC,
            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
        )
        
        for (i in 0 until params.frameCount) {
            val timeMs = (i * params.frameIntervalMs).coerceAtMost(params.maxDuration - 100)
            val timeUs = timeMs * 1000
            
            var extractedBitmap: Bitmap? = null
            
            // Try each extraction option until we get a good frame
            for (option in extractionOptions) {
                try {
                    extractedBitmap = retriever.getFrameAtTime(timeUs, option)
                    if (extractedBitmap != null) {
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Extraction failed with option $option at ${timeMs}ms", e)
                }
            }
            
            extractedBitmap?.let { bitmap ->
                try {
                    val optimizedFrame = createOptimizedFrame(
                        bitmap, 
                        params.targetWidth, 
                        params.targetHeight,
                        metadata.rotation
                    )
                    
                    if (isFrameValid(optimizedFrame)) {
                        frames.add(optimizedFrame)
                    } else {
                        optimizedFrame.recycle()
                    }
                    
                    bitmap.recycle()
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Error processing frame at ${timeMs}ms", e)
                    bitmap.recycle()
                }
            }
            
            // Stop if we're taking too long
            if (i % 20 == 0 && System.currentTimeMillis() % 1000 < 100) {
                // Check periodically if we should continue
                if (frames.size > 0 && i > params.frameCount / 2) {
                    Log.d(TAG, "Early termination: extracted ${frames.size} frames")
                    break
                }
            }
        }
        
        frames
    }
    
    private fun createOptimizedFrame(
        originalBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        rotation: Int
    ): Bitmap {
        // Use high-quality scaling
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap, targetWidth, targetHeight, true
        )
        
        // Apply rotation if needed
        val rotatedBitmap = if (rotation != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotation.toFloat())
            try {
                Bitmap.createBitmap(scaledBitmap, 0, 0, targetWidth, targetHeight, matrix, true)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to rotate bitmap, using original")
                scaledBitmap
            }
        } else {
            scaledBitmap
        }
        
        // Create final optimized bitmap
        val finalBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, false)
        
        // Clean up intermediate bitmaps
        if (rotatedBitmap != scaledBitmap && !rotatedBitmap.isRecycled) {
            rotatedBitmap.recycle()
        }
        if (scaledBitmap != finalBitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }
        
        return finalBitmap
    }
    
    private fun isFrameValid(bitmap: Bitmap): Boolean {
        // Check if frame is valid (not completely black or corrupted)
        try {
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                return false
            }
            
            // Sample a few pixels to check if frame has content
            val centerX = bitmap.width / 2
            val centerY = bitmap.height / 2
            val samplePixels = listOf(
                bitmap.getPixel(centerX, centerY),
                bitmap.getPixel(centerX / 2, centerY / 2),
                bitmap.getPixel(centerX + centerX / 2, centerY + centerY / 2)
            )
            
            // Check if all sampled pixels are not black
            return samplePixels.any { pixel ->
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                r > 10 || g > 10 || b > 10 // Not completely black
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error validating frame", e)
            return false
        }
    }
    
    // Data classes for organization
    private data class VideoMetadata(
        val duration: Long,
        val width: Int,
        val height: Int,
        val rotation: Int,
        val bitrate: Int,
        val frameRate: Float
    )
    
    private data class ExtractionParams(
        val frameCount: Int,
        val frameIntervalMs: Long,
        val targetWidth: Int,
        val targetHeight: Int,
        val maxDuration: Long
    )
}
