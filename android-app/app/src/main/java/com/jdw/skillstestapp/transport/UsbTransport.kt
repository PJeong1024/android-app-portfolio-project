package com.jdw.skillstestapp.transport

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.jdw.skillstestapp.data.transport.ConnectionType
import com.jdw.skillstestapp.data.transport.DataTransport
import com.jdw.skillstestapp.data.transport.TransportState
import com.jdw.skillstestapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB Accessory (AOA) transport.
 * Android acts as USB accessory; macOS Electron side acts as USB host using node-usb + AOA protocol.
 * Data I/O is stream-based via ParcelFileDescriptor (same structure as TcpTransport).
 */
@Singleton
class UsbTransport @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) : DataTransport {

    companion object {
        private const val TAG = "UsbTransport"
        private const val ACTION_USB_PERMISSION = "com.jdw.skillstestapp.USB_PERMISSION"
        private const val READ_BUFFER_SIZE = 4096
    }

    override val connectionType = ConnectionType.USB

    private val _state = MutableStateFlow<TransportState>(TransportState.Disconnected)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _receivedData = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val receivedData: SharedFlow<ByteArray> = _receivedData.asSharedFlow()

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    @Volatile private var pfd: ParcelFileDescriptor? = null
    @Volatile private var outputStream: FileOutputStream? = null
    @Volatile private var isDisconnecting = false
    private var readJob: Job? = null
    private var broadcastReceiver: BroadcastReceiver? = null

    override fun connect() {
        val current = _state.value
        if (current is TransportState.Connected || current is TransportState.Connecting) {
            Log.d(TAG, "connect: already ${current::class.simpleName}, ignored")
            return
        }
        Log.i(TAG, "connect: starting USB accessory detection")
        isDisconnecting = false
        registerReceiver()

        val accessory = usbManager.accessoryList?.firstOrNull()
        if (accessory != null) {
            Log.i(TAG, "connect: accessory already attached — ${accessory.manufacturer} / ${accessory.model}")
            requestOrOpen(accessory)
        } else {
            Log.i(TAG, "connect: no accessory attached, waiting for AOA handshake from host")
            _state.value = TransportState.Connecting
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
        readJob?.cancel()
        readJob = null
        runCatching { outputStream?.close() }
        runCatching { pfd?.close() }
        outputStream = null
        pfd = null
        unregisterReceiver()
        _state.value = TransportState.Disconnected
        Log.i(TAG, "state → Disconnected")
        isDisconnecting = false
    }

    private fun requestOrOpen(accessory: UsbAccessory) {
        if (usbManager.hasPermission(accessory)) {
            openAccessory(accessory)
        } else {
            Log.i(TAG, "requesting permission for ${accessory.model}")
            _state.value = TransportState.Connecting
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(accessory, permissionIntent)
        }
    }

    private fun openAccessory(accessory: UsbAccessory) {
        val fd = usbManager.openAccessory(accessory) ?: run {
            Log.e(TAG, "openAccessory failed for ${accessory.model}")
            _state.value = TransportState.Error("Failed to open USB accessory")
            return
        }
        pfd = fd
        outputStream = FileOutputStream(fd.fileDescriptor)
        _state.value = TransportState.Connected
        Log.i(TAG, "state → Connected (${accessory.manufacturer} / ${accessory.model})")
        startReadLoop(FileInputStream(fd.fileDescriptor))
    }

    private fun startReadLoop(inputStream: FileInputStream) {
        readJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(READ_BUFFER_SIZE)
            Log.d(TAG, "read loop started")
            try {
                while (isActive && !isDisconnecting) {
                    val bytesRead = inputStream.read(buffer)
                    when {
                        bytesRead > 0 -> {
                            Log.d(TAG, "recv: ${bytesRead}B")
                            _receivedData.emit(buffer.copyOf(bytesRead))
                        }
                        bytesRead == -1 -> {
                            if (!isDisconnecting) {
                                Log.i(TAG, "stream closed by host")
                                _state.value = TransportState.Disconnected
                            }
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                if (!isDisconnecting) {
                    Log.e(TAG, "read error: ${e.message}")
                    _state.value = TransportState.Error(e.message ?: "USB read error")
                }
            }
            Log.d(TAG, "read loop ended")
        }
    }

    private fun registerReceiver() {
        if (broadcastReceiver != null) return
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val accessory: UsbAccessory? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                }
                when (intent.action) {
                    ACTION_USB_PERMISSION -> {
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (granted && accessory != null) {
                            Log.i(TAG, "USB permission granted: ${accessory.model}")
                            openAccessory(accessory)
                        } else {
                            Log.w(TAG, "USB permission denied")
                            _state.value = TransportState.Error("USB permission denied")
                        }
                    }
                    UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                        Log.i(TAG, "USB accessory attached: ${accessory?.model}")
                        if (accessory != null && _state.value !is TransportState.Connected) {
                            requestOrOpen(accessory)
                        }
                    }
                    UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                        Log.i(TAG, "USB accessory detached")
                        if (!isDisconnecting) {
                            isDisconnecting = true
                            readJob?.cancel()
                            readJob = null
                            runCatching { outputStream?.close() }
                            runCatching { pfd?.close() }
                            outputStream = null
                            pfd = null
                            _state.value = TransportState.Disconnected
                            Log.i(TAG, "state → Disconnected (accessory detached)")
                            isDisconnecting = false
                        }
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
        broadcastReceiver = receiver
        Log.d(TAG, "BroadcastReceiver registered")
    }

    private fun unregisterReceiver() {
        broadcastReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            broadcastReceiver = null
            Log.d(TAG, "BroadcastReceiver unregistered")
        }
    }
}
