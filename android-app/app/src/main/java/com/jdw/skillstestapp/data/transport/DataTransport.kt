package com.jdw.skillstestapp.data.transport

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface DataTransport {
    val connectionType: ConnectionType
    val state: StateFlow<TransportState>

    // Emits raw packet bytes as received from the remote side.
    // SharedFlow allows multiple collectors (ViewModel, packet parser, etc.)
    val receivedData: SharedFlow<ByteArray>

    fun connect()
    fun send(packet: ByteArray)
    fun disconnect()
}
