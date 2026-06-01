package com.jdw.skillstestapp.screens.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdw.skillstestapp.data.model.UserImg
import com.jdw.skillstestapp.data.model.packet.ImageListItem
import com.jdw.skillstestapp.data.model.packet.PacketCommand
import com.jdw.skillstestapp.data.transport.ConnectionType
import com.jdw.skillstestapp.packet.PacketBuilder
import com.jdw.skillstestapp.packet.PacketParser
import com.jdw.skillstestapp.repository.GoogleMapsRepository
import com.jdw.skillstestapp.transport.TransportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoogleMapScreenViewModel @Inject constructor(
    private val mapsRepository: GoogleMapsRepository,
    private val transportManager: TransportManager
) : ViewModel() {

    companion object {
        private const val TAG = "GoogleMapScreenVM"
    }

    private val _userImages: MutableStateFlow<List<UserImg>> = MutableStateFlow(emptyList())
    val userImages: StateFlow<List<UserImg>> = _userImages.asStateFlow()

    // Stateful parsers per transport source — preserves partial packet state across chunks
    private val packetParsers = mapOf(
        ConnectionType.USB to PacketParser(),
        ConnectionType.TCP to PacketParser()
    )

    init {
        viewModelScope.launch {
            mapsRepository.getAllImages().collect { _userImages.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            mapsRepository.syncDeletedImages()
            mapsRepository.fetchImages()
        }
        viewModelScope.launch {
            transportManager.receivedData.collect { (type, bytes) ->
                val parser = packetParsers[type] ?: return@collect
                parser.feed(bytes).forEach { packet ->
                    when (packet.command) {
                        PacketCommand.IMAGE_DATA_REQUEST -> {
                            val request = parser.parseImageRequest(packet.payload) ?: return@forEach
                            Log.d(TAG, "recv IMAGE_DATA_REQUEST: ids=${request.imageIDs} via $type")
                            request.imageIDs.forEach { imageID ->
                                viewModelScope.launch(Dispatchers.IO) {
                                    respondWithThumbnail(imageID, type)
                                }
                            }
                        }
                        else -> Log.w(TAG, "recv: unhandled cmd=${packet.command.name} from $type")
                    }
                }
            }
        }
    }

    /**
     * Sends image metadata for clicked marker(s) to all connected transports.
     * Single marker click → list of 1 item. Cluster click → full cluster list.
     */
    fun sendMarkerData(images: List<UserImg>) {
        val items = images.map { img ->
            ImageListItem(
                imageID = img.imageID,
                imageDisplayName = img.imageDisplayName,
                imageLat = img.imageLat,
                imageLong = img.imageLong
            )
        }
        val packet = PacketBuilder.buildImageList(items)
        transportManager.sendToAll(packet)
    }

    /** CMD 0x03: loads scaled-down JPEG and sends THUMBNAIL_RESPONSE to the originating transport. */
    private suspend fun respondWithThumbnail(imageID: Int, sourceType: ConnectionType) {
        val image = _userImages.value.find { it.imageID == imageID }
        if (image == null) {
            Log.w(TAG, "respondWithThumbnail: imageID=$imageID not found in cache")
            return
        }
        val bytes = mapsRepository.loadThumbnailBytes(image.imageDataPath)
        if (bytes == null) {
            Log.e(TAG, "respondWithThumbnail: failed to load imageID=$imageID")
            return
        }
        val packet = PacketBuilder.buildThumbnailResponse(imageID, bytes)
        transportManager.sendTo(sourceType, packet)
        Log.d(TAG, "respondWithThumbnail: sent ${bytes.size}B for imageID=$imageID via $sourceType")
    }

    /** CMD 0x04: loads full-size JPEG and sends RAW_IMAGE_RESPONSE to the originating transport. */
    @Suppress("unused")
    private suspend fun respondWithRawImage(imageID: Int, sourceType: ConnectionType) {
        val image = _userImages.value.find { it.imageID == imageID }
        if (image == null) {
            Log.w(TAG, "respondWithRawImage: imageID=$imageID not found in cache")
            return
        }
        val bytes = mapsRepository.loadRawImageBytes(image.imageDataPath)
        if (bytes == null) {
            Log.e(TAG, "respondWithRawImage: failed to load imageID=$imageID")
            return
        }
        val packet = PacketBuilder.buildRawImageResponse(imageID, bytes)
        transportManager.sendTo(sourceType, packet)
        Log.d(TAG, "respondWithRawImage: sent ${bytes.size}B for imageID=$imageID via $sourceType")
    }
}
