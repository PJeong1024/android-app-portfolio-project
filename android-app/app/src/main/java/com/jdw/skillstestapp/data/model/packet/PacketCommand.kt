package com.jdw.skillstestapp.data.model.packet

enum class PacketCommand(val code: Byte) {
    IMAGE_LIST(0x01),           // Android → Electron: imageID + displayName + lat + long
    IMAGE_DATA_REQUEST(0x02),   // Electron → Android: imageID list
    THUMBNAIL_RESPONSE(0x03),   // Android → Electron: imageID + thumbnail bytes (Base64)
    RAW_IMAGE_RESPONSE(0x04);   // Android → Electron: imageID + raw image bytes (Base64) — future

    companion object {
        fun fromCode(code: Byte): PacketCommand? = entries.find { it.code == code }
    }
}
