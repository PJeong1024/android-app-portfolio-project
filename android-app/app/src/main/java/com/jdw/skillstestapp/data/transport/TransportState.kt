package com.jdw.skillstestapp.data.transport

sealed class TransportState {
    object Disconnected : TransportState()
    object Connecting : TransportState()
    object Connected : TransportState()
    data class Error(val message: String) : TransportState()
}
