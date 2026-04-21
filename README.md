# Android ↔ Electron 로컬 통신 포트폴리오

![Android CI](https://github.com/USERNAME/REPO/actions/workflows/android-build.yml/badge.svg)
![Electron CI](https://github.com/USERNAME/REPO/actions/workflows/electron-build.yml/badge.svg)

> USB CDC / TCP Socket 이중 통신으로 Android 앱과 macOS Electron 앱을 연동하는 포트폴리오 프로젝트

---

## 동작 영상

> 추후 demo.gif 추가 예정

---

## 프로젝트 소개

안드로이드 앱에서 사진의 GPS 정보를 지도에 마커로 표시하고, 마커 클릭 시 해당 데이터를 USB CDC 또는 TCP/IP Socket을 통해 macOS Electron 앱으로 실시간 전송합니다. Electron 앱은 수신된 데이터를 Google Maps에 마커로 누적 표시합니다.

---

## 시스템 구조

```
[Android App]
  갤러리 → GPS 파싱 → 지도 마커 표시
  → 마커 클릭 → 패킷 구성
  → DataTransport (USB or TCP)
        ↓
[macOS Electron App]
  DataReceiver (serialport or net.Server)
  → 패킷 파싱 → IPC
  → Maps JS API 마커 누적 표시
```

---

## 기술 스택

### Android App
- Kotlin
- Google Maps SDK
- USB Host API
- TCP/IP Socket

### Electron App
- Electron (Node.js + Chromium)
- Google Maps JavaScript API
- serialport (USB CDC)
- net.Server (TCP)

### 통신
- USB CDC (자동 연결)
- TCP/IP Socket (IP/Port 설정)
- 커스텀 바이너리 패킷 프로토콜

---

## 핵심 구현 내용

- USB CDC / TCP Socket 이중 통신을 추상화 인터페이스(`DataTransport`)로 설계하여 런타임 교체 가능
- 커스텀 바이너리 패킷 프로토콜 설계 및 구현 (GPS + 이미지 데이터)
- Electron Main/Renderer 프로세스 간 IPC 통신
- Google Maps JS API 실시간 마커 누적 표시

---

## UI/UX 설계

> Figma 프로토타입 링크 추가 예정

---

## 프로젝트 구조

```
portfolio-project/
├── android-app/              # 안드로이드 프로젝트
├── electron-app/             # Electron 프로젝트
├── docs/
│   ├── architecture.md       # 시스템 구조
│   ├── packet-protocol.md    # 패킷 명세
│   └── demo.gif              # 동작 영상
├── .github/
│   └── workflows/
│       ├── android-build.yml
│       └── electron-build.yml
├── CLAUDE.md
└── README.md
```

---

## 개발 환경

- Android Studio
- VS Code
- Node.js v18+
- macOS

---

## 실행 방법

### Android App
```bash
# Android Studio에서 프로젝트 열기
# local.properties에 API 키 설정
MAPS_API_KEY=your_api_key
```

### Electron App
```bash
cd electron-app
npm install
cp .env.example .env   # API 키 설정
npm start
```
