package com.videowidgetplayer.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.videowidgetplayer.R
import com.videowidgetplayer.utils.MediaUtils

class ConfigVideoAdapter(
    private val onVideoPlay: (String) -> Unit,
    private val onVideoRemove: (String) -> Unit
) : RecyclerView.Adapter<ConfigVideoAdapter.VideoViewHolder>() {
    
    private val videoUris = mutableListOf<String>()
    
    fun updateVideos(newVideos: List<String>) {
        videoUris.clear()
        videoUris.addAll(newVideos)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_config_video, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(videoUris[position], position)
    }
    
    override fun getItemCount(): Int = videoUris.size
    
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val titleTextView: TextView = itemView.findViewById(R.id.video_title)
        private val durationTextView: TextView = itemView.findViewById(R.id.video_duration)
        private val playButton: ImageView = itemView.findViewById(R.id.play_button)
        private val removeButton: ImageView = itemView.findViewById(R.id.remove_button)
        private val indexTextView: TextView = itemView.findViewById(R.id.video_index)
        
        fun bind(videoUri: String, position: Int) {
            val context = itemView.context
            
            // Set video index
            indexTextView.text = "${position + 1}"
            
            // Get video info
            val videoInfo = MediaUtils.getVideoInfo(context, Uri.parse(videoUri))
            titleTextView.text = videoInfo.displayName ?: "Unknown Video"
            durationTextView.text = formatDuration(videoInfo.duration)
            
            // Load thumbnail
            loadThumbnail(context, videoUri)
            
            // Set click listeners
            playButton.setOnClickListener {
                onVideoPlay(videoUri)
            }
            
            removeButton.setOnClickListener {
                onVideoRemove(videoUri)
            }
            
            itemView.setOnClickListener {
                onVideoPlay(videoUri)
            }
        }
        
        private fun loadThumbnail(context: Context, videoUri: String) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
            
            Glide.with(context)
                .load(Uri.parse(videoUri))
                .apply(requestOptions)
                .into(thumbnailImageView)
        }
        
        private fun formatDuration(durationMs: Long): String {
            val seconds = (durationMs / 1000).toInt()
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return String.format("%d:%02d", minutes, remainingSeconds)
        }
    }
}
