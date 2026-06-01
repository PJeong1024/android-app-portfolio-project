# Android Portfolio App

![Android CI](https://github.com/Pjeong1024/android-app-portfolio-project/actions/workflows/android-build.yml/badge.svg)
![Electron CI](https://github.com/Pjeong1024/android-app-portfolio-project/actions/workflows/electron-build.yml/badge.svg)

A portfolio Android app built around **Google Maps SDK** and **Places API**, demonstrating multiple independent features as tab-based screens. Each tab showcases a distinct technical challenge — from cross-platform USB/TCP communication to real-time location-based search. New tabs are continuously added as the project grows.

---

## App Structure

| Tab | Name | Description |
|-----|------|-------------|
| Tab 1 | **Google Maps Marker** | Parses GPS metadata from gallery photos, displays markers/clusters on Google Maps, and transmits selected marker data to a macOS Electron app via USB AOA or TCP/IP |
| Tab 2 | **Food Search** | Searches nearby restaurants using Google Places API, displays image markers with async photo loading, and shows detailed info (hours, rating, reviews, photos) in a bottom sheet |
| Tab N | *(planned)* | Additional feature tabs to be added |

---

## Key Features

### Tab 1 — Google Maps Marker
- **Dual transport layer** — USB AOA and TCP/IP switchable at runtime through an abstracted `DataTransport` interface
- **Custom binary packet protocol** — designed and implemented on both Android (Kotlin) and Electron (Node.js)
- **Real-time marker sync** — marker/cluster click on Android → instant marker accumulation on macOS
- **Async thumbnail loading** — Electron requests image data, Android responds with compressed thumbnails over the same transport channel
- **Persistent marker storage** — SQLite DB on Electron side restores markers across app restarts

### Tab 2 — Food Search
- **Places API Nearby Search** — fetches restaurants within 1 km of current location using `SearchNearbyRequest` + `CircularBounds`
- **Image marker async loading** — default icon displayed immediately; replaced with food photo once fetched via `fetchPlacePhoto()`
- **Marker clustering** — `PlaceClusterItem` wrapper with `Clustering` composable; cluster click → bottom sheet list
- **Place detail bottom sheet** — business status chip, rating, price level, opening hours (7-day), photo grid, and reviews

### Common
- **Modular DI architecture** — Hilt with `@Singleton` scoped repositories and `@ApplicationScope` coroutine scope
- **ViewModel + StateFlow** — unidirectional data flow throughout all screens
- **Jetpack Compose** — fully Compose-based UI with `LaunchedEffect`-driven camera animation and permission handling

---

## Screenshots

### Tab 1 — Google Maps Marker (Android)

| Map — Marker View | Map — Cluster View | Marker Click — Image |
|:-----------------:|:------------------:|:--------------------:|
| ![](screenshots/android/android_map_markers.png) | ![](screenshots/android/android_map_cluster.png) | ![](screenshots/android/android_image_view.png) |

| Settings — TCP | Settings — USB (AOA) |
|:--------------:|:--------------------:|
| ![](screenshots/android/android_settings_tcp.png) | ![](screenshots/android/android_settings_usb.png) |

### Tab 2 — Food Search (Android)

| Map — Image Markers | Bottom Sheet — Place List | Bottom Sheet — Place Detail |
|:-------------------:|:-------------------------:|:---------------------------:|
| ![](screenshots/android/android_food_map.png) | ![](screenshots/android/android_food_list.png) | ![](screenshots/android/android_food_detail.png) |

### Tab 1 — Google Maps Marker (Electron / macOS)

| Map — Marker Accumulation | Cluster Panel — Image List | Thumbnail Modal |
|:-------------------------:|:--------------------------:|:---------------:|
| ![](screenshots/electron/electron_map_markers.png) | ![](screenshots/electron/electron_cluster_panel.png) | ![](screenshots/electron/electron_thumbnail_modal.png) |

| Settings — TCP Mode | Settings — USB (AOA) Mode |
|:-------------------:|:-------------------------:|
| ![](screenshots/electron/electron_settings_tcp.png) | ![](screenshots/electron/electron_settings_usb.png) |

---

## System Architecture

### Tab 1 — Google Maps Marker

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

#### USB AOA Connection Flow

```
① Android: tap "Start Detection" → UsbTransport registers BroadcastReceiver
② Electron (Mac): node-usb detects Android device → AOA handshake
     ACCESSORY_GET_PROTOCOL → SEND_STRING ×6 → ACCESSORY_START
③ Android: restarts in Accessory mode → ACTION_USB_ACCESSORY_ATTACHED
④ Android: permission granted → openAccessory() → FileStream I/O → Connected
⑤ Packet exchange identical to TCP from this point
   Cable unplugged: ACTION_USB_ACCESSORY_DETACHED → auto Disconnected
```

### Tab 2 — Food Search

```
[Android App]
  Location permission check → FusedLocationProvider → current GPS coordinates
  → PlacesRepository.searchNearbyRestaurants() (1km CircularBounds)
  → Default markers displayed immediately
  → fetchPlacePhoto() parallel loading → image markers replace default icons
  → Marker click   → BottomSheet: business status / rating / price / hours / photos / reviews
  → Cluster click  → BottomSheet: place list → item tap → place detail
  → FAB (↺)        → re-trigger searchNearby()
```

---

## Packet Protocol

```
[STX 1B][CMD 1B][LENGTH 4B big-endian][PAYLOAD NB][CHECKSUM 1B][ETX 1B]
CHECKSUM = (CMD + LENGTH bytes + PAYLOAD bytes) % 256
PAYLOAD  = UTF-8 JSON byte array
```

| CMD | Description | Direction |
|-----|-------------|-----------|
| 0x01 | Image list (imageID, displayName, lat, lng) | Android → Electron |
| 0x02 | Image data request (imageID list) | Electron → Android |
| 0x03 | Thumbnail response (imageID + Base64 JPEG) | Android → Electron |
| 0x04 | Raw image response (imageID + Base64 JPEG) | Android → Electron |

---

## Tech Stack

### Android App
| Category | Library / API |
|----------|---------------|
| Language | Kotlin |
| UI | Jetpack Compose |
| DI | Dagger-Hilt |
| Async | Kotlin Coroutines + Flow |
| Map | Google Maps SDK for Android |
| Places | Places SDK for Android 3.5.0 |
| Location | FusedLocationProviderClient |
| USB | USB Accessory API (AOA) |
| Network | TCP Socket (java.net.Socket) |
| Image metadata | ExifInterface |

### Electron App (macOS)
| Category | Library / Package |
|----------|------------------|
| Framework | Electron ^33 |
| Map | Google Maps JavaScript API |
| USB | node-usb ^2.x (AOA + bulk transfer) |
| Network | Node.js net.Server (TCP) |
| Database | better-sqlite3 (SQLite) |
| Clustering | @googlemaps/markerclusterer |
| Config | dotenv |

---

## Project Structure

```
portfolio-project/
├── android-app/          # Android Studio project (Kotlin + Compose)
├── electron-app/         # Electron project (Node.js)
│   ├── transport/        # UsbReceiver (AOA), TcpReceiver
│   ├── packet/           # PacketParser + builder
│   └── db/               # SQLite marker storage
├── screenshots/
│   ├── android/          # Android app screenshots
│   └── electron/         # Electron app screenshots
└── docs/
    ├── architecture.md
    └── packet-protocol.md
```

---

## Setup

### Prerequisites
- Node.js v18+
- Android Studio (for Android build)
- Google Cloud project with the following APIs enabled:
  - **Maps JavaScript API**
  - **Maps SDK for Android**
  - **Places API (New)**

### Electron App

```bash
cd electron-app
npm install
npx electron-rebuild          # rebuild native modules (usb, better-sqlite3)
cp .env.local.example .env.local
# edit .env.local → GOOGLE_MAPS_API_KEY=your_key
npm start
```

### Android App

```
1. Open android-app/ in Android Studio
2. Add to local.properties:
   MAPS_API_KEY=your_key
3. Run on device (USB debugging enabled)
```

### Connecting the Apps (Tab 1)

**TCP mode**
1. Electron: open Settings panel → TCP → enter port → click "Start Server"
2. Android: open Settings → TCP card → enter Mac's IP and port → Connect

**USB AOA mode**
1. Connect Android to Mac via USB cable
2. Android: open Settings → USB card → tap "Start Detection"
3. Electron: open Settings panel → USB → click "Connect"
4. Electron performs AOA handshake → Android switches to Accessory mode → auto-connects

---

## API Keys

This project requires API keys managed via local config files (`.gitignore`'d):

| Key | File | Variable | Used by |
|-----|------|----------|---------|
| Maps JavaScript API | `electron-app/.env.local` | `GOOGLE_MAPS_API_KEY` | Electron map display |
| Maps Android SDK + Places API (New) | `android-app/local.properties` | `MAPS_API_KEY` | Android map + Food Search |

> **Note:** The same `MAPS_API_KEY` is reused for both Maps SDK and Places API (New). Ensure **Places API (New)** is enabled on this key in Google Cloud Console.

---

## CI/CD

| Workflow | Trigger | Artifact |
|----------|---------|----------|
| `android-build.yml` | push / PR to `main` | `app-debug.apk` |
| `electron-build.yml` | push / PR to `main` | Electron build |

---

## Documentation

- [System Architecture](docs/architecture.md) — module structure, DataTransport interface, IPC channels, communication flow
- [Packet Protocol](docs/packet-protocol.md) — frame format, CMD types, payload schemas, checksum calculation

---

## License

MIT
