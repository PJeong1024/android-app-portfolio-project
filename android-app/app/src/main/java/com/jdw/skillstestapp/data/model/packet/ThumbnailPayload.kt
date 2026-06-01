package com.jdw.skillstestapp.data.model.packet

data class ThumbnailPayload(
    val imageID: Int,
    val thumbnailData: String  // Base64-encoded JPEG bytes
)
