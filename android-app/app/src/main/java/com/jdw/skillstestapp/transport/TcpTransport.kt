package com.jdw.skillstestapp.transport

import android.util.Log
import com.jdw.skillstestapp.data.transport.ConnectionType
import com.jdw.skillstestapp.data.transport.DataTransport
import com.jdw.skillstestapp.data.transport.TransportState
import com.jdw.skillstestapp.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpTransport @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) : DataTransport {

    companion object {
        private const val TAG = "TcpTransport"
    }

    override val connectionType = ConnectionType.TCP

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val receivedData: SharedFlow<ByteArray> = _receivedData.asSharedFlow()

    @Volatile private var ip: String = ""
    @Volatile private var port: Int = 0

    @Volatile private var socket: Socket? = null
    @Volatile private var outputStream: OutputStream? = null
    @Volatile private var isDisconnecting = false
    private var connectJob: Job? = null

    /** Must be called (from any thread) before connect(). Safe to call while connected — takes effect on next connect(). */
    fun configure(ip: String, port: Int) {
        Log.i(TAG, "configure: $ip:$port")
        this.ip = ip
        this.port = port
    }

    override fun connect() {
        val currentState = _state.value
        if (currentState is TransportState.Connected || currentState is TransportState.Connecting) {
            Log.d(TAG, "connect: already in state ${currentState::class.simpleName}, ignored")
            return
        }
        if (ip.isEmpty() || port == 0) {
            Log.e(TAG, "connect: IP/Port not configured")
            _state.value = TransportState.Error("IP/Port not configured")
            return
        }

        Log.i(TAG, "connect: attempting $ip:$port")
        isDisconnecting = false
        _state.value = TransportState.Connecting

        connectJob = scope.launch(Dispatchers.IO) {
            try {
                val s = Socket(ip, port)
                socket = s
                outputStream = s.getOutputStream()
                _state.value = TransportState.Connected
                Log.i(TAG, "state → Connected ($ip:$port)")

                val buffer = ByteArray(4096)
                val inputStream = s.getInputStream()
                while (isActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) {
                        Log.i(TAG, "remote closed connection")
                        break
                    }
                    Log.d(TAG, "recv: ${bytesRead}B")
                    _receivedData.emit(buffer.copyOf(bytesRead))
                }
            } catch (e: Exception) {
                if (!isDisconnecting) {
                    Log.e(TAG, "error: ${e.message}")
                    _state.value = TransportState.Error(e.message ?: "Connection error")
                }
            } finally {
                socket = null
                outputStream = null
                if (!isDisconnecting && _state.value !is TransportState.Error) {
                    Log.i(TAG, "state → Disconnected")
                    _state.value = TransportState.Disconnected
                }
            }
        }
    }

    override fun send(packet: ByteArray) {
        if (_state.value !is TransportState.Connected) {
            Log.w(TAG, "send: not connected, dropped ${packet.size}B")
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                outputStream?.write(packet)
                outputStream?.flush()
                Log.d(TAG, "send: ${packet.size}B sent")
            }.onFailure { e ->
                Log.e(TAG, "send error: ${e.message}")
                _state.value = TransportState.Error(e.message ?: "Send error")
            }
        }
    }

    override fun disconnect() {
        Log.i(TAG, "disconnect called")
        isDisconnecting = true
        connectJob?.cancel()
        connectJob = null
        runCatching { socket?.close() }
        socket = null
        outputStream = null
        _state.value = TransportState.Disconnected
        Log.i(TAG, "state → Disconnected")
        isDisconnecting = false
    }
}
