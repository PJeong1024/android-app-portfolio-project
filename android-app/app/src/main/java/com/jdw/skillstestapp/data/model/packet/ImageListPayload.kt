package com.jdw.skillstestapp.data.model.packet

data class ImageListItem(
    val imageID: Int,
    val imageDisplayName: String,
    val imageLat: Double?,
    val imageLong: Double?
)

data class ImageListPayload(
    val items: List<ImageListItem>
)
