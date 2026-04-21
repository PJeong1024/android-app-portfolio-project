# SkillPortfolioApp Application

## Important Notice
- This app is still being implemented.
- This app requires the following API KEY list.
  - GoogleMap (Android SDK)
  - GoogleMap (JavaScript API)
- This app requires the following connection environment.
  - USB CDC connection (Auto detect)
  - TCP/IP Socket connection (Manual IP/Port configuration required)

## Introduce app
This app demonstrates local communication between an Android device and a macOS Electron app via USB CDC or TCP/IP Socket. It parses GPS metadata from photos, displays markers on Google Maps, and transmits selected marker data to the Electron receiver app in real time.

## Screenshots
#### Google Map - Marker View
> Screenshot coming soon

#### Settings - Connection Mode
> Screenshot coming soon

## Electron Receiver App
#### Map View - Marker Accumulation
> Screenshot coming soon

## UI/UX Design
> Figma Prototype Link coming soon

## Purpose
- Demonstrate local communication implementation using USB CDC and TCP/IP Socket
- Show abstracted transport layer design allowing runtime switching between connection types
- Implement custom binary packet protocol for GPS and image data transmission
- Built as a multi-tab portfolio app showcasing various Android features and libraries

## Specific SDK / Library / Function List

### Android App
- Jetpack Compose
- Dagger-Hilt
- Kotlin Coroutines
- MVVM architecture
- Google Maps SDK
- USB Host API (USB CDC communication)
- TCP/IP Socket
- ExifInterface (GPS metadata parsing)

### Electron App (macOS Receiver)
- Electron (Node.js + Chromium)
- Google Maps JavaScript API
- serialport (USB CDC receive)
- net.Server (TCP/IP receive)
- IPC (Main ↔ Renderer communication)

### Communication
- Custom binary packet protocol (GPS + Image data)
- Dual transport interface (USB / TCP switchable at runtime)

## System Architecture
```
[Android App]
  Gallery → GPS Parsing → Map Marker Display
  → Marker Click → Packet Build
  → DataTransport (USB or TCP)
        ↓
[macOS Electron App]
  DataReceiver (serialport or net.Server)
  → Packet Parse → IPC
  → Maps JS API Marker Accumulation
```

## Tab Structure
| Tab | Feature |
|-----|---------|
| Map | Google Maps marker display + marker click transmission |
| Settings | USB / TCP connection mode switch + TCP IP/Port configuration |

## CI/CD
![Android CI](https://github.com/Pjeong1024/repo/actions/workflows/android-build.yml/badge.svg)
![Electron CI](https://github.com/Pjeong1024/repo/actions/workflows/electron-build.yml/badge.svg)
