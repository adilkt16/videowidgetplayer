package com.videowidgetplayer.data

import android.net.Uri

data class VideoFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val path: String
)
