package com.jdw.skillstestapp.packet

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.jdw.skillstestapp.data.model.packet.ImageListPayload
import com.jdw.skillstestapp.data.model.packet.ImageRequestPayload
import com.jdw.skillstestapp.data.model.packet.PacketCommand
import com.jdw.skillstestapp.data.model.packet.RawImagePayload
import com.jdw.skillstestapp.data.model.packet.ThumbnailPayload

/**
 * Stateful streaming parser — safe to call feed() with partial or multi-packet chunks.
 * Maintains an internal buffer and emits complete, checksum-validated packets.
 *
 * Not thread-safe: access from a single thread or synchronize externally.
 */
class PacketParser {

    companion object {
        private const val TAG = "PacketParser"
        private const val STX: Byte = 0x02
        private const val ETX: Byte = 0x03
        private const val HEADER_SIZE = 6   // STX(1) + CMD(1) + LENGTH(4)
        private const val FOOTER_SIZE = 2   // CHECKSUM(1) + ETX(1)
        private const val MIN_PACKET_SIZE = HEADER_SIZE + FOOTER_SIZE
        private const val MAX_PAYLOAD_SIZE = 10 * 1024 * 1024  // 10 MB sanity cap
    }

    data class ParsedPacket(val command: PacketCommand, val payload: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ParsedPacket) return false
            return command == other.command && payload.contentEquals(other.payload)
        }
        override fun hashCode() = 31 * command.hashCode() + payload.contentHashCode()
    }

    private val buffer = ArrayDeque<Byte>()
    private val gson = Gson()

    /** Feed raw bytes from the transport layer. Returns all complete packets extracted. */
    fun feed(data: ByteArray): List<ParsedPacket> {
        buffer.addAll(data.asIterable())
        Log.d(TAG, "feed: +${data.size}B, buffer=${buffer.size}B")

        return buildList {
            var packet = tryExtract()
            while (packet != null) {
                add(packet)
                packet = tryExtract()
            }
        }
    }

    private fun tryExtract(): ParsedPacket? {
        // Discard bytes before the next STX and warn if any were dropped
        var discarded = 0
        while (buffer.isNotEmpty() && buffer.first() != STX) {
            buffer.removeFirst()
            discarded++
        }
        if (discarded > 0) Log.w(TAG, "discarded $discarded garbage bytes before STX")

        if (buffer.size < MIN_PACKET_SIZE) return null

        val cmd = buffer[1]
        val length = ((buffer[2].toInt() and 0xFF) shl 24) or
                ((buffer[3].toInt() and 0xFF) shl 16) or
                ((buffer[4].toInt() and 0xFF) shl 8) or
                (buffer[5].toInt() and 0xFF)

        if (length < 0 || length > MAX_PAYLOAD_SIZE) {
            Log.w(TAG, "invalid payload length=$length (max=$MAX_PAYLOAD_SIZE), skipping STX")
            buffer.removeFirst()
            return null
        }

        val totalSize = HEADER_SIZE + length + FOOTER_SIZE
        if (buffer.size < totalSize) {
            Log.d(TAG, "incomplete packet: have ${buffer.size}B, need ${totalSize}B, waiting")
            return null
        }

        val checksumPos = HEADER_SIZE + length
        val etxPos = checksumPos + 1

        if (buffer[etxPos] != ETX) {
            Log.w(TAG, "ETX mismatch at pos=$etxPos (got=0x%02X), skipping STX".format(buffer[etxPos].toInt() and 0xFF))
            buffer.removeFirst()
            return null
        }

        val payload = ByteArray(length) { buffer[HEADER_SIZE + it] }
        val receivedChecksum = buffer[checksumPos]

        var sum = cmd.toInt() and 0xFF
        for (i in 2..5) sum += buffer[i].toInt() and 0xFF
        sum += payload.sumOf { it.toInt() and 0xFF }
        val expectedChecksum = (sum % 256).toByte()

        repeat(totalSize) { buffer.removeFirst() }

        if (receivedChecksum != expectedChecksum) {
            Log.w(TAG, "checksum mismatch: expected=0x%02X got=0x%02X, dropping packet"
                .format(expectedChecksum.toInt() and 0xFF, receivedChecksum.toInt() and 0xFF))
            return null
        }

        val command = PacketCommand.fromCode(cmd)
        if (command == null) {
            Log.w(TAG, "unknown cmd=0x%02X, dropping packet".format(cmd.toInt() and 0xFF))
            return null
        }

        Log.d(TAG, "extracted: cmd=${command.name} payload=${payload.size}B")
        return ParsedPacket(command, payload)
    }

    // --- Payload deserialization helpers ---

    fun parseImageList(payload: ByteArray): ImageListPayload? = runCatching {
        gson.fromJson(payload.toString(Charsets.UTF_8), ImageListPayload::class.java)
            .also { Log.d(TAG, "parseImageList: ${it.items.size} item(s)") }
    }.getOrElse {
        Log.e(TAG, "parseImageList failed: ${it.message}")
        null
    }

    fun parseImageRequest(payload: ByteArray): ImageRequestPayload? = runCatching {
        gson.fromJson(payload.toString(Charsets.UTF_8), ImageRequestPayload::class.java)
            .also { Log.d(TAG, "parseImageRequest: ids=${it.imageIDs}") }
    }.getOrElse {
        Log.e(TAG, "parseImageRequest failed: ${it.message}")
        null
    }

    fun parseThumbnail(payload: ByteArray): Pair<Int, ByteArray>? = runCatching {
        val dto = gson.fromJson(payload.toString(Charsets.UTF_8), ThumbnailPayload::class.java)
        val bytes = Base64.decode(dto.thumbnailData, Base64.NO_WRAP)
        Log.d(TAG, "parseThumbnail: imageID=${dto.imageID} decoded=${bytes.size}B")
        dto.imageID to bytes
    }.getOrElse {
        Log.e(TAG, "parseThumbnail failed: ${it.message}")
        null
    }

    fun parseRawImage(payload: ByteArray): Pair<Int, ByteArray>? = runCatching {
        val dto = gson.fromJson(payload.toString(Charsets.UTF_8), RawImagePayload::class.java)
        val bytes = Base64.decode(dto.imageData, Base64.NO_WRAP)
        Log.d(TAG, "parseRawImage: imageID=${dto.imageID} decoded=${bytes.size}B")
        dto.imageID to bytes
    }.getOrElse {
        Log.e(TAG, "parseRawImage failed: ${it.message}")
        null
    }

    fun reset() {
        Log.d(TAG, "reset: clearing ${buffer.size}B from buffer")
        buffer.clear()
    }
}
