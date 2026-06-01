package com.jdw.skillstestapp.transport

import android.util.Log
import com.jdw.skillstestapp.data.transport.ConnectionType
import com.jdw.skillstestapp.data.transport.DataTransport
import com.jdw.skillstestapp.data.transport.TransportState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages USB and TCP transports simultaneously.
 *
 * Both transports can be independently connected/disconnected.
 * Outbound packets can be sent to all connected transports or a specific one.
 * Inbound data is merged from both transports and tagged with its source [ConnectionType].
 */
@Singleton
class TransportManager @Inject constructor(
    val usbTransport: UsbTransport,
    val tcpTransport: TcpTransport
) {
    companion object {
        private const val TAG = "TransportManager"
    }

    val allTransports: List<DataTransport> = listOf(usbTransport, tcpTransport)

    /**
     * Merged stream of received raw bytes from all active transports.
     * Each emission is tagged with the source [ConnectionType].
     */
    val receivedData: Flow<Pair<ConnectionType, ByteArray>> = merge(
        usbTransport.receivedData.map { ConnectionType.USB to it },
        tcpTransport.receivedData.map { ConnectionType.TCP to it }
    )

    /** Sends [packet] to every transport that is currently connected. */
    fun sendToAll(packet: ByteArray) {
        val targets = allTransports.filter { it.state.value is TransportState.Connected }
        if (targets.isEmpty()) {
            Log.w(TAG, "sendToAll: no connected transports, ${packet.size}B dropped")
            return
        }
        val names = targets.joinToString { it.connectionType.name }
        Log.d(TAG, "sendToAll: ${packet.size}B → [$names]")
        targets.forEach { it.send(packet) }
    }

    /** Sends [packet] to a specific transport. No-op if that transport is not connected. */
    fun sendTo(type: ConnectionType, packet: ByteArray) {
        val target = allTransports.firstOrNull {
            it.connectionType == type && it.state.value is TransportState.Connected
        }
        if (target == null) {
            Log.w(TAG, "sendTo ${type.name}: not connected, ${packet.size}B dropped")
            return
        }
        Log.d(TAG, "sendTo ${type.name}: ${packet.size}B")
        target.send(packet)
    }

    /** Returns true if at least one transport is currently connected. */
    val isAnyConnected: Boolean
        get() = allTransports.any { it.state.value is TransportState.Connected }
}
