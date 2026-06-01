package com.jdw.skillstestapp.packet

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.jdw.skillstestapp.data.model.packet.ImageListItem
import com.jdw.skillstestapp.data.model.packet.ImageListPayload
import com.jdw.skillstestapp.data.model.packet.ImageRequestPayload
import com.jdw.skillstestapp.data.model.packet.PacketCommand
import com.jdw.skillstestapp.data.model.packet.RawImagePayload
import com.jdw.skillstestapp.data.model.packet.ThumbnailPayload
import java.nio.ByteBuffer

object PacketBuilder {

    private const val TAG = "PacketBuilder"
    private const val STX: Byte = 0x02
    private const val ETX: Byte = 0x03
    private val gson = Gson()

    /**
     * Builds a framed packet:
     * [STX][CMD][LENGTH 4B big-endian][PAYLOAD N B][CHECKSUM][ETX]
     * CHECKSUM = (CMD + LENGTH bytes + PAYLOAD bytes) % 256
     */
    fun build(command: PacketCommand, payload: ByteArray): ByteArray {
        val lengthBytes = ByteBuffer.allocate(4).putInt(payload.size).array()

        var sum = command.code.toInt() and 0xFF
        sum += lengthBytes.sumOf { it.toInt() and 0xFF }
        sum += payload.sumOf { it.toInt() and 0xFF }
        val checksum = (sum % 256).toByte()

        val packet = byteArrayOf(STX, command.code) + lengthBytes + payload + byteArrayOf(checksum, ETX)
        Log.d(TAG, "build: cmd=${command.name} payload=${payload.size}B total=${packet.size}B checksum=0x%02X".format(checksum.toInt() and 0xFF))
        return packet
    }

    fun buildImageList(images: List<ImageListItem>): ByteArray {
        Log.d(TAG, "buildImageList: ${images.size} item(s)")
        val json = gson.toJson(ImageListPayload(images))
        return build(PacketCommand.IMAGE_LIST, json.toByteArray(Charsets.UTF_8))
    }

    fun buildImageRequest(imageIDs: List<Int>): ByteArray {
        Log.d(TAG, "buildImageRequest: ids=$imageIDs")
        val json = gson.toJson(ImageRequestPayload(imageIDs))
        return build(PacketCommand.IMAGE_DATA_REQUEST, json.toByteArray(Charsets.UTF_8))
    }

    fun buildThumbnailResponse(imageID: Int, thumbnailBytes: ByteArray): ByteArray {
        Log.d(TAG, "buildThumbnailResponse: imageID=$imageID size=${thumbnailBytes.size}B")
        val base64 = Base64.encodeToString(thumbnailBytes, Base64.NO_WRAP)
        val json = gson.toJson(ThumbnailPayload(imageID, base64))
        return build(PacketCommand.THUMBNAIL_RESPONSE, json.toByteArray(Charsets.UTF_8))
    }

    fun buildRawImageResponse(imageID: Int, imageBytes: ByteArray): ByteArray {
        Log.d(TAG, "buildRawImageResponse: imageID=$imageID size=${imageBytes.size}B")
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val json = gson.toJson(RawImagePayload(imageID, base64))
        return build(PacketCommand.RAW_IMAGE_RESPONSE, json.toByteArray(Charsets.UTF_8))
    }
}
