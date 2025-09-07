package com.videowidgetplayer.adapter

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
import com.videowidgetplayer.R
import com.videowidgetplayer.data.VideoFile

/**
 * RecyclerView adapter for video selection
 * Following the spec: Multi-selection for widget videos (≤60 seconds only)
 */
class VideoSelectionAdapter(
    private val onVideoSelectionChanged: (VideoFile, Boolean) -> Unit
) : ListAdapter<VideoFile, VideoSelectionAdapter.VideoViewHolder>(VideoDiffCallback()) {

    private val selectedVideos = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    /**
     * Get list of selected video files
     */
    fun getSelectedVideos(): List<VideoFile> {
        return currentList.filter { selectedVideos.contains(it.id) }
    }

    /**
     * Clear all selections
     */
    fun clearSelections() {
        selectedVideos.clear()
        notifyDataSetChanged()
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val nameTextView: TextView = itemView.findViewById(R.id.videoName)
        private val durationTextView: TextView = itemView.findViewById(R.id.videoDuration)
        private val checkBox: CheckBox = itemView.findViewById(R.id.videoCheckbox)

        fun bind(video: VideoFile) {
            nameTextView.text = video.name
            durationTextView.text = "${video.getDurationFormatted()} • ${video.getSizeFormatted()}"
            
            // Load thumbnail using Glide
            Glide.with(itemView.context)
                .load(video.thumbnailUri ?: video.uri)
                .placeholder(R.drawable.widget_preview)
                .error(R.drawable.widget_preview)
                .centerCrop()
                .into(thumbnailImageView)
            
            // Set checkbox state
            checkBox.isChecked = selectedVideos.contains(video.id)
            
            // Handle checkbox changes
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedVideos.add(video.id)
                } else {
                    selectedVideos.remove(video.id)
                }
                onVideoSelectionChanged(video, isChecked)
            }
            
            // Handle item click
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
            
            // Disable items that don't meet widget requirements
            val isValidForWidget = video.isValidForWidget()
            itemView.alpha = if (isValidForWidget) 1.0f else 0.5f
            checkBox.isEnabled = isValidForWidget
            
            if (!isValidForWidget) {
                durationTextView.text = "${video.getDurationFormatted()} • Too long for widget"
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
