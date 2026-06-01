package com.jdw.skillstestapp.screens.viewmodel

import androidx.lifecycle.ViewModel
import com.jdw.skillstestapp.data.transport.TransportState
import com.jdw.skillstestapp.transport.TransportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ConnectionSettingsViewModel @Inject constructor(
    private val transportManager: TransportManager
) : ViewModel() {

    val tcpState: StateFlow<TransportState> = transportManager.tcpTransport.state
    val usbState: StateFlow<TransportState> = transportManager.usbTransport.state

    private val _tcpIp = MutableStateFlow("")
    val tcpIp: StateFlow<String> = _tcpIp.asStateFlow()

    private val _tcpPort = MutableStateFlow("")
    val tcpPort: StateFlow<String> = _tcpPort.asStateFlow()

    fun onTcpIpChanged(value: String) { _tcpIp.value = value }

    fun onTcpPortChanged(value: String) {
        // Accept only digits up to 5 characters (max port 65535)
        if (value.all { it.isDigit() } && value.length <= 5) {
            _tcpPort.value = value
        }
    }

    fun connectTcp() {
        val port = _tcpPort.value.toIntOrNull()?.takeIf { it in 1..65535 } ?: return
        val ip = _tcpIp.value.trim().takeIf { it.isNotEmpty() } ?: return
        transportManager.tcpTransport.configure(ip, port)
        transportManager.tcpTransport.connect()
    }

    fun disconnectTcp() {
        transportManager.tcpTransport.disconnect()
    }

    val isTcpConnectEnabled: Boolean
        get() = _tcpIp.value.isNotBlank() &&
                _tcpPort.value.toIntOrNull()?.let { it in 1..65535 } == true &&
                (tcpState.value is TransportState.Disconnected || tcpState.value is TransportState.Error)

    fun connectUsb() {
        transportManager.usbTransport.connect()
    }

    fun disconnectUsb() {
        transportManager.usbTransport.disconnect()
    }

    val isUsbConnectEnabled: Boolean
        get() = usbState.value is TransportState.Disconnected || usbState.value is TransportState.Error
}
