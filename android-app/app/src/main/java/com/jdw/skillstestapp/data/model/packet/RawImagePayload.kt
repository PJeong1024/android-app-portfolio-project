package com.jdw.skillstestapp.data.model.packet

data class RawImagePayload(
    val imageID: Int,
    val imageData: String  // Base64-encoded JPEG bytes
)
