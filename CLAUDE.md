# 개발 포트폴리오 프로젝트 기획서

> 최종 업데이트: 2026-04-21

---

## 1. 프로젝트 개요

안드로이드 앱과 macOS Electron 앱을 로컬 통신(USB CDC / TCP/IP)으로 연동하는 포트폴리오 프로젝트.

### 핵심 어필 포인트
- USB CDC / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계
- 런타임 교체 가능한 로컬 통신 모듈 구현
- 확장성을 고려한 모듈화 및 DI 설계
- Android + Electron 크로스 플랫폼 연동 구현

---

## 2. 유저 시나리오

1. 안드로이드 앱에서 갤러리 사진의 GPS 메타데이터를 파싱
2. Google Maps 위에 마커 표시 (마커 내 썸네일 포함)
3. USB 케이블 또는 WiFi로 macOS와 연결
4. 사용자가 마커를 클릭하면 해당 데이터(GPS 좌표 + 이미지)를 패킷으로 전송
5. macOS Electron 앱이 수신하여 Google Maps에 마커 누적 표시
6. 마커 클릭마다 반복 → Electron 앱에 마커가 계속 추가됨

---

## 3. 시스템 구조

```
[안드로이드 앱]
  갤러리 로드 → GPS 파싱 → 지도 마커 표시
  → 마커 클릭 → 패킷 구성
  → DataTransport (USB or TCP)
        ↓
[macOS Electron 앱]
  DataReceiver (serialport or net.Server)
  → 패킷 파싱
  → IPC (Main → Renderer)
  → Maps JS API 마커 누적 표시
```

---

## 4. 개발 범위

| # | 항목 | 기술 |
|---|------|------|
| 1 | 안드로이드 앱 | Kotlin, Google Maps SDK, USB Host API |
| 2 | macOS 앱 | Electron, Maps JS API, serialport |
| 3 | 통신 연동 | USB CDC / TCP Socket 패킷 통신 |
| 4 | UI/UX 설계 | Figma |
| 5 | 기획/명세 문서 | Markdown |

---

## 5. 통신 방식 설계

### 추상화 인터페이스 (안드로이드)

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

class UsbTransport : DataTransport {
    // BroadcastReceiver로 자동 감지 및 연결
}

class TcpTransport(
    private val ip: String,
    private val port: Int
) : DataTransport {
    // 설정값 기반 소켓 연결
}
```

### 연결 방식별 UX

| 방식 | 연결 방법 | 사용자 액션 |
|------|----------|------------|
| USB CDC | 케이블 연결 시 자동 감지 | 없음 (자동) |
| TCP/IP | 설정 UI에서 IP/Port 입력 | IP + Port 입력 후 연결 |

### Electron 수신 (macOS)

```javascript
// USB 모드
const SerialPort = require('serialport')
// → /dev/tty.usbmodem* 자동 감지

// TCP 모드
const net = require('net')
const server = net.createServer((socket) => {
  socket.on('data', (data) => {
    mainWindow.webContents.send('new-location', parsePacket(data))
  })
})
server.listen(8080, '0.0.0.0')
```

---

## 6. 패킷 프로토콜 (초안)


### 전송간 동작(Android App <-> Electron App)

1. Android 앱에서의 기존 동작
- 폰에 있는 사진들에서 GPS 정보를 꺼내서 googlemap에서 마커/마커 클러스터 방식으로 표시
- 단독 마커를 클릭하면 이미지를 보여주고, 클러스터를 클릭하면 이미지 리스트를 보여줌
- 이미지 리스트에서 이미지를 선택하면 앞의 단독 마커 클릭처럼 이미지를 보여줌

2. Electron 앱과의 연계동작(구현 목표)
- 안드로이드 앱에서 마커나 클러스터를 클릭하면 마커 리스트 정보를 전송
  - 전송 데이터는 imageID + imageDisplayName + imageLat + imageLong 포함
  - 자세한 내용은 image data class 참조
- Electron 앱에서는 해당 데이터로 마커/클러스터 표시
- 사용자가 Electron 앱에서 해당 표시된 마커 혹은 클러스터 클릭시 Electron 앱에서는 아래와 같이 표시
  - 마커 클릭시는 이미지 1개 표시하면서 안드로이드 앱에 imageID 기반으로 이미지 데이터 전송 요청(원본). 전송되는대로 async 이미지 로딩 진행
  - 클러스터 클릭시는 해당 클러스터에 포함된 이미지 이름 리스트 표시(imageDataPath). 사용자가 리스트에서 이미지를 선택하면 해당 이미지 데이터 전송 요청(원본). 전송되는대로 async 이미지 로딩 진행

### image data sample
```kotlin
@Entity(tableName = "user_images")
data class UserImg(
    @PrimaryKey @ColumnInfo(name = "image_id") val imageID: Int = 0,
    @ColumnInfo(name = "image_data_path") val imageDataPath: String = "",
    @ColumnInfo(name = "image_display_name") val imageDisplayName: String = "",
    @ColumnInfo(name = "image_lat") val imageLat: Double? = null,
    @ColumnInfo(name = "image_long") val imageLong: Double? = null,
    @ColumnInfo(name = "image_date_taken") val imageDateTaken: Long = 0L,
    @ColumnInfo(name = "image_orientation") val imageOri: Int = 0,
    @ColumnInfo(name = "image_size") val imageSize: Long = 0L,
    @ColumnInfo(name = "image_address") val imageAddress: String = ""
)

```

### 패킷 구조

```
[STX 1byte][CMD 1byte][LENGTH 4byte][PAYLOAD N byte][CHECKSUM 1byte][ETX 1byte]
- STX (Start of Text): 0x02
- Packet Command (CMD): JSON 직렬화된 데이터 통신간 구분
- LENGTH: PAYLOAD 길이 (4 byte, big-endian)
- PAYLOAD: 전송데이터를 JSON 직렬화한 바이트 배열
- CHECKSUM: (CMD + LENGTH + PAYLOAD) % 256
- ETX (End of Text): 0x03
```

### Packet CMD 종류 (확장 가능)
| CMD | 설명 | 패킷 전송 방향|
|-----|------|---|
| 0x01 | image list (imageID + imageDisplayName + imageLat + imageLong 으로 구성된 단독 데이터 혹은 리스트) | Android → Electron |
| 0x02 | image data request (imageID 단독 혹은 리스트) | Electron → Android |
| 0x03 | thumbnail image data response (imageID + thumbnailImageData) | Android → Electron |
| 0x04 | raw image data response (imageID + imageData) -> 향후 구현  | Android → Electron |

---





## 7. 안드로이드 앱 구조

### 탭 구성 (포트폴리오 다기능 어필 목적)

```
MainActivity
├── Tab 1 : Google Maps (핵심 기능)
│           ├── 사진 GPS 파싱
│           ├── 마커 + 썸네일 표시
│           └── 마커 클릭 → 패킷 전송
├── Tab 2 : 연결 설정
│           ├── 연결 방식 선택 (USB / TCP)
│           ├── TCP 설정 (IP, Port) ← TCP 선택시만 활성화
│           └── 연결 상태 표시
└── Tab N : 기타 기능 (추후 확장)
```

### 모듈 구조

```
app/
├── transport/
│   ├── DataTransport.kt       # 인터페이스
│   ├── UsbTransport.kt        # USB 구현체
│   └── TcpTransport.kt        # TCP 구현체
├── map/
│   ├── MapManager.kt          # 지도 관련 로직
│   └── MarkerFactory.kt       # 마커 생성
├── packet/
│   ├── PacketBuilder.kt       # 패킷 구성
│   └── PacketParser.kt        # 패킷 파싱
└── ui/
    ├── MapFragment.kt
    └── SettingsFragment.kt
```

---

## 8. Electron 앱 구조

```
electron-app/
├── main.js              # Main Process (USB/TCP 수신, IPC)
├── preload.js           # 보안 브릿지
├── index.html           # Renderer (지도 UI)
├── renderer.js          # Maps JS API + IPC 수신
├── transport/
│   ├── UsbReceiver.js   # serialport 기반
│   └── TcpReceiver.js   # net.Server 기반
├── packet/
│   └── PacketParser.js  # 패킷 파싱
├── .env                 # API 키 (gitignore)
└── package.json
```

### Electron 프로세스 역할

| 프로세스 | 역할 |
|---------|------|
| Main Process | USB/TCP 수신, 패킷 파싱, IPC 송신 |
| Renderer Process | Google Maps 표시, 마커 누적, UI |

---

## 9. 개발 환경 및 AI 도구

### IDE
- Android Studio — 안드로이드 앱
- VS Code — Electron 앱

### AI 도구 역할 분담

| 작업 | GitHub Copilot | Claude |
|------|---------------|--------|
| 인라인 자동완성 | ✅ | ✅ (Claude Code) |
| 파일 단위 코드 생성 | △ | ✅ |
| 설계 · 아키텍처 논의 | △ | ✅ |
| 패킷 프로토콜 설계 | ❌ | ✅ |
| 문서 작성 | △ | ✅ |

### 추천 워크플로우
> Claude에서 설계 · 뼈대 코드 → IDE에서 Copilot으로 세부 구현 → 막히면 다시 Claude

---

## 10. 개발 Phase

### Phase 1 — 안드로이드 앱 리팩토링

- [ ] DataTransport 인터페이스 설계
- [ ] UsbTransport 구현 (자동 연결)
- [ ] TcpTransport 구현 (설정 UI 연동)
- [ ] 기존 Google Maps 코드 리팩토링
- [ ] 마커 클릭 → 패킷 전송 구현
- [ ] 탭 구조 DI 정리

**완료 기준**
- DataTransport를 Mock으로 교체해도 앱 정상 동작
- 마커 클릭 시 패킷 송신 로직이 UI 코드에서 완전 분리

### Phase 2 — Electron 앱 빌드

- [ ] 프로젝트 초기 셋업 (npm, electron, serialport)
- [ ] 기본 창 띄우기
- [ ] Google Maps JS API 지도 표시
- [ ] UsbReceiver 구현 (serialport)
- [ ] TcpReceiver 구현 (net.Server)
- [ ] 패킷 파싱 → IPC → 마커 표시 연동
- [ ] API 키 .env 관리

### Phase 3 — 양단 연동 및 검증

- [ ] 패킷 프로토콜 최종 확정
- [ ] USB 실기기 연동 테스트
- [ ] TCP 연동 테스트
- [ ] E2E 시나리오 검증 (마커 클릭 → 맥 누적 표시)

---

## 11. 레포지토리 구조

```
portfolio-project/
├── android-app/              # 안드로이드 프로젝트
├── electron-app/             # Electron 프로젝트
├── figma/                    # Figma 익스포트 or 링크
├── docs/
│   ├── architecture.md       # 시스템 구조 설명
│   ├── packet-protocol.md    # USB/TCP 패킷 명세
│   └── demo.gif              # 동작 영상 캡처
├── .github/
│   └── workflows/
│       ├── android-build.yml # 안드로이드 APK 자동빌드
│       └── electron-build.yml# Electron 앱 자동빌드
├── README.md
└── .gitignore
```

### GitHub Actions — android-build.yml

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: API 키 주입
        run: echo "MAPS_API_KEY=${{ secrets.MAPS_API_KEY }}" > local.properties
      - name: APK 빌드
        run: ./gradlew assembleDebug
      - name: APK 업로드
        uses: actions/upload-artifact@v3
        with:
          name: app-debug.apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### README 뱃지

```markdown
![Android CI](https://github.com/username/repo/actions/workflows/android-build.yml/badge.svg)
```

---

## 12. 환경 셋업 체크리스트

### 맥북 환경
- [ ] Node.js v18+ 설치 확인 (`node -v`)
- [ ] Claude Code 설치 (`npm install -g @anthropic-ai/claude-code`)
- [ ] VS Code Copilot 익스텐션 확인
- [ ] socat 설치 (`brew install socat`) — 가상 포트 테스트용

### 안드로이드 환경
- [ ] Android Studio Copilot 플러그인 설치
- [ ] USB 디버깅 활성화

### API / 서비스
- [ ] Google Cloud 프로젝트 확인
- [ ] Maps JavaScript API 활성화
- [ ] Maps Android SDK 활성화
- [ ] API 키 발급 및 제한 설정
- [ ] GitHub Secrets에 MAPS_API_KEY 등록

---

## 13. 이력서 어필 포인트 (초안)

> USB CDC / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계하여 런타임 교체 가능한 로컬 통신 모듈 구현

> Android ↔ macOS 간 커스텀 바이너리 패킷 프로토콜 설계 및 구현

> Electron(Node.js + Chromium) 기반 데스크탑 앱에서 Google Maps JS API 연동 및 실시간 마커 누적 표시 구현