package com.videowidgetplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.videowidgetplayer.R
import com.videowidgetplayer.data.VideoFile

/**
 * Adapter for displaying selected videos with View and Remove actions
 */
class SelectedVideoAdapter(
    private val onViewVideo: (VideoFile) -> Unit,
    private val onRemoveVideo: (VideoFile) -> Unit
) : ListAdapter<VideoFile, SelectedVideoAdapter.SelectedVideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedVideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_video, parent, false)
        return SelectedVideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectedVideoViewHolder, position: Int) {
        val video = getItem(position)
        holder.bind(video)
    }

    inner class SelectedVideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImageView: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val nameTextView: TextView = itemView.findViewById(R.id.videoName)
        private val durationTextView: TextView = itemView.findViewById(R.id.videoDuration)
        private val viewButton: Button = itemView.findViewById(R.id.viewButton)
        private val removeButton: Button = itemView.findViewById(R.id.removeButton)

        fun bind(video: VideoFile) {
            nameTextView.text = video.name
            durationTextView.text = "${video.getDurationFormatted()} • ${video.getSizeFormatted()}"
            
            // Load thumbnail using Glide with better error handling
            Glide.with(itemView.context)
                .load(video.uri) // Use video URI directly instead of thumbnail URI
                .placeholder(R.drawable.widget_preview)
                .error(R.drawable.widget_preview)
                .centerCrop()
                .into(thumbnailImageView)
            
            // Set button click listeners
            viewButton.setOnClickListener {
                onViewVideo(video)
            }
            
            removeButton.setOnClickListener {
                onRemoveVideo(video)
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
