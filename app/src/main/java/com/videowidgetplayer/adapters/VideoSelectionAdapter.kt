package com.videowidgetplayer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.videowidgetplayer.R
import com.videowidgetplayer.data.VideoFile

class VideoSelectionAdapter(
    private val onVideoClick: (VideoFile) -> Unit,
    private val onVideoLongClick: (VideoFile) -> Boolean
) : ListAdapter<VideoFile, VideoSelectionAdapter.VideoViewHolder>(VideoDiffCallback()) {
    
    private val selectedVideos = mutableSetOf<VideoFile>()
    
    fun updateSelection(newSelection: Set<VideoFile>) {
        val oldSelection = selectedVideos.toSet()
        selectedVideos.clear()
        selectedVideos.addAll(newSelection)
        
        // Update only changed items for better performance
        currentList.forEachIndexed { index, video ->
            val wasSelected = oldSelection.contains(video)
            val isSelected = selectedVideos.contains(video)
            
            if (wasSelected != isSelected) {
                notifyItemChanged(index)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_selection, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.video_thumbnail)
        private val titleTextView: TextView = itemView.findViewById(R.id.video_title)
        private val durationTextView: TextView = itemView.findViewById(R.id.video_duration)
        private val sizeTextView: TextView = itemView.findViewById(R.id.video_size)
        private val selectionCheckBox: CheckBox = itemView.findViewById(R.id.selection_checkbox)
        private val selectionOverlay: View = itemView.findViewById(R.id.selection_overlay)
        
        fun bind(video: VideoFile) {
            // Set video info
            titleTextView.text = video.displayName
            durationTextView.text = formatDuration(video.duration)
            sizeTextView.text = formatFileSize(video.size)
            
            // Load thumbnail
            loadThumbnail(video)
            
            // Update selection state
            val isSelected = selectedVideos.contains(video)
            selectionCheckBox.isChecked = isSelected
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            // Set click listeners
            itemView.setOnClickListener {
                onVideoClick(video)
            }
            
            itemView.setOnLongClickListener {
                onVideoLongClick(video)
            }
            
            selectionCheckBox.setOnClickListener {
                onVideoClick(video)
            }
        }
        
        private fun loadThumbnail(video: VideoFile) {
            val requestOptions = RequestOptions()
                .placeholder(R.drawable.ic_video_placeholder)
                .error(R.drawable.ic_video_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
            
            Glide.with(itemView.context)
                .load(video.uri)
                .apply(requestOptions)
                .into(thumbnailImageView)
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
    }
    
    private class VideoDiffCallback : DiffUtil.ItemCallback<VideoFile>() {
        override fun areItemsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: VideoFile, newItem: VideoFile): Boolean {
            return oldItem == newItem
        }
    }
}
