# 시스템 아키텍처

## 전체 구조

```
[Android App]                        [macOS Electron App]
─────────────                        ────────────────────
갤러리 로드                           Main Process (Node.js)
  └─ GPS 파싱                          └─ UsbReceiver (serialport)
  └─ 지도 마커 표시                     └─ TcpReceiver (net.Server)
  └─ 마커 클릭                          └─ 패킷 파싱
  └─ 패킷 구성                          └─ IPC 송신
  └─ DataTransport                            ↓
        ↓                             Renderer Process (Chromium)
   USB or TCP                          └─ Maps JS API
        ↓                              └─ 마커 누적 표시
  [로컬 통신]
```

---

## Android 모듈 구조

```
app/
├── transport/
│   ├── DataTransport.kt       # 인터페이스
│   ├── UsbTransport.kt        # USB CDC 구현체 (자동 연결)
│   └── TcpTransport.kt        # TCP Socket 구현체 (설정 기반)
├── map/
│   ├── MapManager.kt          # 지도 관련 로직
│   └── MarkerFactory.kt       # 마커 생성
├── packet/
│   ├── PacketBuilder.kt       # 패킷 구성
│   └── PacketParser.kt        # 패킷 파싱
└── ui/
    ├── MapFragment.kt         # 지도 화면
    └── SettingsFragment.kt    # 설정 화면 (USB/TCP 스위치)
```

### DataTransport 인터페이스

```kotlin
interface DataTransport {
    val connectionType: ConnectionType
    val isConnected: Boolean
    var onDataReceived: ((ByteArray) -> Unit)?
    fun connect()
    fun send(packet: ByteArray)
    fun disconnect()
}

enum class ConnectionType { USB, TCP }
```

---

## Electron 모듈 구조

```
electron-app/
├── main.js              # Main Process
├── preload.js           # 보안 브릿지
├── index.html           # Renderer UI
├── renderer.js          # Maps JS API + IPC 수신
├── transport/
│   ├── UsbReceiver.js   # serialport 기반
│   └── TcpReceiver.js   # net.Server 기반
├── packet/
│   └── PacketParser.js  # 패킷 파싱
└── .env                 # API 키 (gitignore)
```

### Electron 프로세스 역할

| 프로세스 | 역할 |
|---------|------|
| Main Process | USB/TCP 수신, 패킷 파싱, IPC 송신 |
| Renderer Process | Google Maps 표시, 마커 누적, UI |

---

## 통신 방식

### USB CDC (자동 연결)
```
안드로이드 케이블 연결
  → BroadcastReceiver 감지
  → UsbTransport.connect() 자동 호출
  → /dev/tty.usbmodem* 포트로 통신
```

### TCP/IP Socket (설정 기반)
```
설정 화면에서 IP / Port 입력
  → TcpTransport(ip, port) 생성
  → Socket 연결
  → Electron net.Server 수신
```

---

## 패킷 프로토콜

```
[STX 1byte][TYPE 1byte][LENGTH 4byte][PAYLOAD N byte][ETX 1byte][CHECKSUM 1byte]
```

### PAYLOAD 구조

| 필드 | 타입 | 크기 |
|------|------|------|
| 위도 | double | 8 byte |
| 경도 | double | 8 byte |
| 이미지 크기 | int | 4 byte |
| 이미지 데이터 | byte[] | N byte (JPEG 압축) |

- 이미지: 512px 이하 리사이즈 후 JPEG 압축
- 대용량: 청크 분할 전송

---

## API 키 관리

### Android
```
local.properties (gitignore)
  └─ MAPS_API_KEY=...
  └─ BuildConfig.MAPS_API_KEY 로 참조
```

### Electron
```
.env (gitignore)
  └─ MAPS_API_KEY=...
  └─ process.env.MAPS_API_KEY 로 참조
```

### GitHub Actions
```
Repository Secrets
  └─ MAPS_API_KEY
  └─ CI 빌드 시 자동 주입
```
