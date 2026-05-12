# System Architecture

## Overview

```
[Android App]
  Gallery → GPS Parsing → Map Marker / Cluster Display
  → Marker / Cluster Click → PacketBuilder → CMD 0x01
  → DataTransport (UsbTransport or TcpTransport)
        │                              ▲
        │  CMD 0x01 (image list)       │ CMD 0x02 (image request)
        ▼                              │ CMD 0x03 (thumbnail response)
[macOS Electron App]
  DataReceiver (UsbReceiver or TcpReceiver)
  → PacketParser → IPC (Main → Renderer)
  → Maps JS API — Marker accumulation + Clustering
  → Marker Click → CMD 0x02 → Android → CMD 0x03 → Thumbnail display
```

---

## Android App

### Module Structure

```
app/
├── data/
│   ├── transport/
│   │   ├── ConnectionType.kt       # enum: USB, TCP
│   │   ├── TransportState.kt       # sealed: Disconnected / Connecting / Connected / Error
│   │   └── DataTransport.kt        # abstraction interface
│   └── model/packet/
│       ├── PacketCommand.kt        # CMD enum (0x01 ~ 0x04)
│       ├── ImageListPayload.kt     # CMD 0x01 payload
│       ├── ImageRequestPayload.kt  # CMD 0x02 payload
│       ├── ThumbnailPayload.kt     # CMD 0x03 payload
│       └── RawImagePayload.kt      # CMD 0x04 payload
├── transport/
│   ├── TcpTransport.kt             # TCP implementation (@Singleton)
│   ├── UsbTransport.kt             # USB AOA implementation (@Singleton)
│   └── TransportManager.kt         # dual transport manager (@Singleton)
├── packet/
│   ├── PacketBuilder.kt            # packet framing
│   └── PacketParser.kt             # streaming parser (ArrayDeque buffer, checksum)
├── repository/
│   └── GoogleMapsRepository.kt     # image loading (thumbnail / raw)
├── di/
│   ├── AppScope.kt                 # @ApplicationScope qualifier
│   └── AppModule.kt                # Hilt module (CoroutineScope, etc.)
└── screens/
    ├── GoogleMapsScreen.kt          # map UI + marker click → send
    ├── ConnectionSettingsScreen.kt  # connection settings UI
    └── viewmodel/
        ├── GoogleMapScreenViewModel.kt    # CMD 0x02 receive → CMD 0x03 respond
        └── ConnectionSettingsViewModel.kt  # TCP / USB connection state
```

### DataTransport Interface

```kotlin
interface DataTransport {
    val connectionType: ConnectionType
    val state: StateFlow<TransportState>    // Disconnected / Connecting / Connected / Error
    val receivedData: SharedFlow<ByteArray> // inbound data stream
    fun connect()
    fun send(packet: ByteArray)
    fun disconnect()
}

enum class ConnectionType { USB, TCP }
```

### TransportManager

```kotlin
class TransportManager {
    fun sendToAll(packet: ByteArray)                     // broadcast to all active transports
    fun sendTo(type: ConnectionType, packet: ByteArray)  // send to specific transport
    val receivedData: Flow<Pair<ConnectionType, ByteArray>> // merged inbound stream
}
```

### USB AOA Connection Flow

```
① Android: tap "Start Detection" → UsbTransport registers BroadcastReceiver
② Electron (Mac): node-usb detects Android device → AOA handshake
     ACCESSORY_GET_PROTOCOL → SEND_STRING ×6 → ACCESSORY_START
③ Android: restarts in Accessory mode → ACTION_USB_ACCESSORY_ATTACHED
④ Android: permission granted → openAccessory() → FileStream I/O → Connected
⑤ Packet exchange identical to TCP from this point
   Cable unplugged: ACTION_USB_ACCESSORY_DETACHED → auto Disconnected
```

### TCP Connection Flow

```
① Electron: open Settings → TCP → enter port → click "Start Server" (net.Server listens)
② Android: open Settings → TCP card → enter Mac's IP and port → Connect
③ TcpTransport establishes Socket connection on Dispatchers.IO
④ Read loop runs on a dedicated coroutine; send() writes directly
```

---

## Electron App (macOS)

### Module Structure

```
electron-app/
├── main.js              # Main Process (USB/TCP receive, IPC, DB)
├── preload.js           # security bridge (contextIsolation)
├── index.html           # Renderer (map UI + settings panel + cluster panel + modal)
├── renderer.js          # Maps JS API + IPC receive + clustering + DB restore
├── transport/
│   ├── DataReceiver.js  # abstract base class (EventEmitter)
│   ├── TcpReceiver.js   # net.Server — Electron server, Android client
│   └── UsbReceiver.js   # node-usb + AOA handshake + bulk IN/OUT streaming
├── packet/
│   └── PacketParser.js  # streaming parser + packet builder (0x01 ~ 0x04)
├── db/
│   └── MarkerDatabase.js # SQLite (better-sqlite3) — marker storage, thumbnail cache
├── .env.local           # API key (gitignored)
└── package.json
```

### Process Roles

| Process | Responsibility |
|---------|----------------|
| Main Process | USB AOA / TCP receive, packet parsing, IPC send, SQLite DB |
| Renderer Process | Google Maps display, marker clustering, thumbnail UI, settings panel |

### IPC Channels

| Channel | Direction | Role |
|---------|-----------|------|
| `get-config` | Renderer → Main | Maps API key |
| `get-local-addresses` | Renderer → Main | local IP list |
| `get-all-markers` | Renderer → Main | full marker query from DB |
| `tcp-start/stop` | Renderer → Main | TCP server control |
| `usb-list-devices` | Renderer → Main | list detectable Android devices |
| `usb-start/stop` | Renderer → Main | USB AOA connection control |
| `request-images` | Renderer → Main | send CMD 0x02 |
| `transport-status` | Main → Renderer | connection state events |
| `image-list` | Main → Renderer | CMD 0x01 received |
| `thumbnail` | Main → Renderer | CMD 0x03 received |
| `raw-image` | Main → Renderer | CMD 0x04 received |

---

## Communication Modes

| Mode | Role | Protocol |
|------|------|----------|
| USB AOA | Android = Accessory, Mac = Host | USB Bulk IN/OUT |
| TCP/IP | Electron = Server, Android = Client | TCP Socket |

Both modes share the same packet protocol and DataTransport abstraction — transport type is switchable at runtime without changing upper-layer logic.
