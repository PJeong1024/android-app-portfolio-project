# 개발 포트폴리오 프로젝트 기획서

> 최종 업데이트: 2026-06-02

---

## 1. 프로젝트 개요
- 안드로이드 앱을 중심으로 한 다양한 기능구현을 통해 Android 개발 역량을 종합적으로 어필하는 포트폴리오 프로젝트
- 탭 기반 멀티 피처 구조로, 각 탭이 독립적인 기능 단위를 이루며 지속적으로 탭이 추가된다

### 현재 탭 구성
- **Tab 1 — Google Maps Marker**: 갤러리 GPS 파싱 + 지도 마커/클러스터 + USB AOA / TCP 통신으로 macOS Electron 연동
- **Tab 2 — Food Search**: Places API 기반 현재 위치 주변 음식점 검색 + 이미지 마커 + 바텀시트 상세 정보
- **Tab 3 — Gemini Chat**: Google Gemini AI 엔진과의 연동기반 챗봇 인터페이스
- **Tab N**: 향후 기능 탭 지속 추가 예정

### 주요 기술적 도전 과제
- USB AOA / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계 (Tab 1 전용)
- 런타임 교체 가능한 로컬 통신 모듈 구현
- 확장성을 고려한 모듈화 및 DI 설계 (Hilt)
- Android + Electron 크로스 플랫폼 연동 구현 (Tab 1 전용)
- 커스텀 바이너리 패킷 프로토콜 설계 및 양단 구현
- Google Maps SDK + Places API 연동을 통한 지도 기반 기능 구현 (Tab 1, Tab 2)
- Google Gemini AI 엔진과의 연동기반 챗봇 인터페이스 구현 (Tab 3)

---

## 2. 유저 시나리오

### 2.1 Tab 1 — Google Maps Marker
1. 안드로이드 앱에서 갤러리 사진의 GPS 메타데이터를 파싱
2. Google Maps 위에 마커/클러스터 표시 (마커 내 썸네일 포함)
3. USB 케이블(AOA) 또는 WiFi(TCP)로 macOS와 연결
4. 사용자가 마커 또는 클러스터를 클릭하면 GPS 좌표 + 이미지 목록을 패킷으로 전송
5. macOS Electron 앱이 수신하여 Google Maps에 마커 누적 표시
6. Electron에서 마커 클릭 → Android에 이미지 요청 → 썸네일 비동기 로딩

### 2.2 Tab 2 — Food Search
1. 탭 진입 → 위치 권한 확인 (없으면 자동 요청)
2. 현재 위치 기반 1km 반경 음식점 자동 검색
3. 가게별 이미지 마커 비동기 로딩 (기본 아이콘 → 이미지 마커 교체)
4. 마커 클릭 → 바텀 시트에 가게 상세 정보 표시 (영업 상태, 평점, 가격대, 영업시간, 사진, 리뷰)
5. 클러스터 클릭 → 바텀 시트 가게 목록 → 항목 선택 → 상세 정보
6. FAB(↺) → 주변 검색 재실행

### 2.3 Tab 3 — Gemini Chat
1. 탭 진입 → 챗 인터페이스 표시 (메시지 목록 + 입력창 + 전송 버튼)
2. 사용자 메시지 입력 → Gemini AI SDK 호출 → Structured Output으로 의도(intent) 분류
3. 일반 대화(`GENERAL`) → AI 응답 텍스트 버블로 표시
4. 장소 검색(`PLACE_SEARCH`) → 현재 위치 기반 Places API 검색 → 결과를 카드 UI(LazyRow)로 표시
5. 카드 클릭 → Google Maps 앱 연동 (geo: URI Intent)
6. 대화 히스토리 Room DB 저장 → 앱 재진입 시 복원

---

## 3. 시스템 구조

### 3.1 Tab 1 — Google Maps Marker
```
[안드로이드 앱]
  갤러리 로드 → GPS 파싱 → 지도 마커/클러스터 표시
  → 마커/클러스터 클릭 → PacketBuilder → CMD 0x01 전송
  → DataTransport (UsbTransport or TcpTransport)
        ↓                         ↑
        ↓ CMD 0x01 (image list)   │ CMD 0x02 (image request)
        ↓                         │ CMD 0x03 (thumbnail response)
[macOS Electron 앱]
  DataReceiver (UsbReceiver or TcpReceiver)
  → PacketParser → IPC (Main → Renderer)
  → Maps JS API 마커 누적 표시
  → 마커 클릭 → CMD 0x02 전송 → Android 응답 대기
  → CMD 0x03 수신 → InfoWindow / 모달 썸네일 표시
```

### 3.2 Tab 2 — Food Search
```
[안드로이드 앱]
  위치 권한 확인 → FusedLocationProvider → 현재 GPS 좌표
  → PlacesRepository.searchNearbyRestaurants() → Place 목록 수신
  → 기본 마커 표시 → fetchPlacePhoto() 병렬 로딩 → 이미지 마커 교체
  → 마커/클러스터 클릭 → BottomSheet 상세 정보 표시
     (영업 상태 / 평점 / 가격대 / 영업시간 / 사진 그리드 / 리뷰)
```
### 3.3 Tab 3 — Gemini Chat
```
[안드로이드 앱]
  챗 UI → 사용자 메시지 입력
  → ChatRepository.sendMessage() → Gemini AI SDK (gemini-2.5-flash)
  → Structured Output JSON 응답 파싱: GeminiIntent { intent, message, keyword, radiusMeters }

  intent == "GENERAL"
  → 텍스트 버블 DB 저장 + UI 표시

  intent == "PLACE_SEARCH"
  → FusedLocationProvider → 현재 위치
  → PlacesRepository.searchNearbyPlaces(keyword, radiusMeters)
    (keywordToPlaceTypes: 25종 매핑)
  → Place → SearchCard 변환 (title/subtitle/badge/latLng)
  → ChatItem.CardResult → CardResultRow (LazyRow 수평 스크롤)
  → 카드 썸네일 fetchPlacePhoto() 비동기 로딩
  → 카드 클릭 → geo:URI → Google Maps 앱
```

---

## 4. 개발 범위

| # | 항목 | 기술 |
|---|------|------|
| 1 | 안드로이드 앱 | Kotlin, Jetpack Compose, Hilt, Google Maps SDK, USB Accessory API (AOA), Places SDK for Android, Google Gemini AI SDK |
| 2 | macOS 앱 | Electron, Maps JS API, node-usb (AOA), net.Server (TCP) |
| 3 | 통신 연동 | USB AOA / TCP Socket, 커스텀 바이너리 패킷 프로토콜 |
| 4 | UI/UX 설계 | Figma |
| 5 | 기획/명세 문서 | Markdown |

---

## 5. 통신 방식 설계

### 추상화 인터페이스 (Android)

```kotlin
interface DataTransport {
    val connectionType: ConnectionType
    val state: StateFlow<TransportState>       // Disconnected / Connecting / Connected / Error
    val receivedData: SharedFlow<ByteArray>    // 수신 데이터 스트림
    fun connect()
    fun send(packet: ByteArray)
    fun disconnect()
}

enum class ConnectionType { USB, TCP }

// USB: USB Accessory API (AOA) — Android가 Accessory, Mac이 Host
class UsbTransport : DataTransport {
    // ACTION_USB_ACCESSORY_ATTACHED BroadcastReceiver
    // openAccessory() → FileInputStream / FileOutputStream
}

// TCP: Kotlin Coroutines Socket — Android가 Client, Electron이 Server
class TcpTransport(ip: String, port: Int) : DataTransport {
    // Dispatchers.IO 기반 connect / read loop / send
}
```

### TransportManager

```kotlin
class TransportManager {
    fun sendToAll(packet: ByteArray)                    // 연결된 모든 transport에 브로드캐스트
    fun sendTo(type: ConnectionType, packet: ByteArray) // 특정 transport에만 송신
    val receivedData: Flow<Pair<ConnectionType, ByteArray>> // 양쪽 수신 데이터 merge
}
```

### 연결 방식별 UX

| 방식 | 연결 방법 | 사용자 액션 |
|------|----------|------------|
| USB AOA | 앱 바 설정 → USB 카드 "감지 시작" → 케이블 연결 → 자동 연결 | "감지 시작" 버튼 클릭 |
| TCP/IP | 앱 바 설정 → TCP 카드 → IP/Port 입력 후 연결 | IP + Port 입력 후 연결 |

### Electron 수신 (macOS)

```javascript
// USB AOA 모드 (node-usb)
// 1. Android 장치 감지 (ANDROID_VIDS로 필터)
// 2. AOA 핸드셰이크: ACCESSORY_GET_PROTOCOL → SEND_STRING × 6 → ACCESSORY_START
// 3. Android 재연결 (VID 0x18D1, PID 0x2D00/0x2D01)
// 4. interface(0) claim → bulk IN/OUT startPoll
const { usb } = require('usb')

// TCP 모드 (net.Server)
// Electron이 서버, Android가 클라이언트
const net = require('net')
const server = net.createServer((socket) => {
  socket.on('data', (data) => packetParser.feed(data))
})
server.listen(8080, '0.0.0.0')
```

---

## 6. 패킷 프로토콜

### 패킷 구조

```
[STX 1byte][CMD 1byte][LENGTH 4byte][PAYLOAD N byte][CHECKSUM 1byte][ETX 1byte]
- STX: 0x02
- CMD: 패킷 종류 구분
- LENGTH: PAYLOAD 길이 (4 byte, big-endian)
- PAYLOAD: JSON 직렬화 바이트 배열
- CHECKSUM: (CMD + LENGTH bytes + PAYLOAD bytes) % 256
- ETX: 0x03
```

### Packet CMD

| CMD | 설명 | 방향 | 구현 상태 |
|-----|------|------|-----------|
| 0x01 | image list (imageID + imageDisplayName + imageLat + imageLong) | Android → Electron | ✅ 완료 |
| 0x02 | image data request (imageID 단독 또는 리스트) | Electron → Android | ✅ 완료 |
| 0x03 | thumbnail image data response (imageID + thumbnailData Base64) | Android → Electron | ✅ 완료 |
| 0x04 | raw image data response (imageID + imageData Base64) | Android → Electron | ✅ 완료 |

### 동작 흐름

```
① Android 마커/클러스터 클릭
   → PacketBuilder.buildImageList() → CMD 0x01 → Electron
   → Electron: DB 저장 + 지도 마커 누적

② Electron 마커 클릭
   → CMD 0x02 (imageID 리스트) → Android
   → Android: 이미지 로드 → PacketBuilder.buildThumbnailResponse() → CMD 0x03
   → Electron: InfoWindow / 모달에 썸네일 표시
```

---

## 7. Android 앱 구조

### 탭 구성

```
MainActivity
├── Tab 1 : Google Maps Marker ✅ 완료
│           ├── 갤러리 GPS 파싱 + DB 저장
│           ├── 마커/클러스터 표시 (UserImgClusterItem + Clustering)
│           ├── 마커/클러스터 클릭 → CMD 0x01 전송 (USB AOA / TCP)
│           └── CMD 0x02 수신 → CMD 0x03/0x04 이미지 응답
├── Tab 2 : Food Search ✅ 완료
│           ├── 위치 권한 확인 + 현재 위치 기반 자동 검색
│           ├── Places API 음식점 마커/클러스터 표시
│           ├── 이미지 마커 비동기 로딩 (기본 아이콘 → 음식 사진으로 교체)
│           ├── 마커 클릭 → 바텀 시트 상세 (영업상태·평점·가격대·영업시간·사진·리뷰)
│           ├── 클러스터 클릭 → 바텀 시트 목록 → 항목 클릭 → 상세
│           └── FAB(↺) 리프레시 버튼
├── 앱 바 설정 아이콘(⚙) → 드롭다운
│           └── Googlemap 설정 → ConnectionSettingsScreen 전체화면
│                   ├── TCP 카드: IP/Port 입력 + 연결/해제
│                   └── USB 카드: 감지 시작/중지 + 상태 표시
└── Tab N : 기타 기능 (Chat, Weather 등) — 향후 추가 예정
```

### 실제 모듈 구조

```
app/
├── data/
│   ├── transport/
│   │   ├── ConnectionType.kt       # enum: USB, TCP
│   │   ├── TransportState.kt       # sealed: Disconnected/Connecting/Connected/Error
│   │   └── DataTransport.kt        # 추상화 인터페이스
│   └── model/packet/
│       ├── PacketCommand.kt        # CMD enum (0x01~0x04)
│       ├── ImageListPayload.kt     # CMD 0x01 페이로드
│       ├── ImageRequestPayload.kt  # CMD 0x02 페이로드
│       ├── ThumbnailPayload.kt     # CMD 0x03 페이로드
│       └── RawImagePayload.kt      # CMD 0x04 페이로드
├── transport/
│   ├── TcpTransport.kt             # TCP 구현체 (@Singleton)
│   ├── UsbTransport.kt             # USB AOA 구현체 (@Singleton)
│   └── TransportManager.kt         # 이중 transport 관리 (@Singleton)
├── packet/
│   ├── PacketBuilder.kt            # 패킷 프레이밍 (buildImageList/Thumbnail/RawImage)
│   └── PacketParser.kt             # 스트리밍 파서 (ArrayDeque 버퍼, 체크섬 검증)
├── repository/
│   ├── GoogleMapsRepository.kt     # 이미지 로드 (loadThumbnailBytes/loadRawImageBytes)
│   └── PlacesRepository.kt         # Places API Nearby Search + fetchPlacePhoto
├── di/
│   ├── AppScope.kt                 # @ApplicationScope qualifier
│   └── AppModule.kt                # Hilt 모듈 (PlacesClient + FusedLocationClient 포함)
└── screens/
    ├── GoogleMapsScreen.kt          # Tab 1: 지도 UI + 마커 클릭 → 전송
    ├── FoodSearchScreen.kt          # Tab 2: 음식점 검색 지도 + 클러스터 + 바텀시트
    ├── GeminiChatRoomScreen.kt      # Tab 3: 챗 UI (버블 + 카드 + 입력창)
    ├── ConnectionSettingsScreen.kt  # 연결 설정 UI
    └── viewmodel/
        ├── GoogleMapScreenViewModel.kt    # 마커 데이터 전송, CMD 0x02 수신 → 0x03 응답
        ├── FoodSearchViewModel.kt         # 위치 획득 + Nearby Search + 사진 비동기 로딩
        ├── GeminiChatRoomViewModel.kt     # Gemini API 호출, Structured Output 파싱, 카드 관리
        └── ConnectionSettingsViewModel.kt  # TCP/USB 연결 상태 관리
```

---

## 8. Electron 앱 구조

### 실제 파일 구조

```
electron-app/
├── main.js              # Main Process (USB/TCP 수신, IPC, DB)
├── preload.js           # 보안 브릿지 (contextIsolation)
├── index.html           # Renderer (지도 UI + 설정 패널 + 클러스터 패널 + 모달)
├── renderer.js          # Maps JS API + IPC 수신 + 클러스터링 + DB 복원
├── transport/
│   ├── DataReceiver.js  # 추상 기반 클래스 (EventEmitter)
│   ├── TcpReceiver.js   # net.Server 기반 (Electron 서버, Android 클라이언트)
│   └── UsbReceiver.js   # node-usb + AOA 핸드셰이크 + bulk IN/OUT
├── packet/
│   └── PacketParser.js  # 스트리밍 파서 + 패킷 빌더 (parseRawImage 포함)
├── db/
│   └── MarkerDatabase.js # SQLite (better-sqlite3) — 마커 저장, 썸네일 캐시
├── .env.local           # API 키 (gitignore)
└── package.json
```

### 프로세스 역할

| 프로세스 | 역할 |
|---------|------|
| Main Process | USB AOA/TCP 수신, 패킷 파싱, IPC 송신, SQLite DB |
| Renderer Process | Google Maps 표시, 마커 클러스터링, 썸네일 UI, 설정 패널 |

### IPC 채널

| 채널 | 방향 | 역할 |
|------|------|------|
| `get-config` | Renderer → Main | Maps API 키 |
| `get-local-addresses` | Renderer → Main | 로컬 IP 목록 |
| `get-all-markers` | Renderer → Main | DB 마커 전체 조회 |
| `tcp-start/stop` | Renderer → Main | TCP 서버 제어 |
| `usb-list-devices` | Renderer → Main | AOA 감지 Android 장치 목록 |
| `usb-start/stop` | Renderer → Main | USB AOA 연결 제어 |
| `request-images` | Renderer → Main | CMD 0x02 전송 |
| `transport-status` | Main → Renderer | 연결 상태 이벤트 |
| `image-list` | Main → Renderer | CMD 0x01 수신 |
| `thumbnail` | Main → Renderer | CMD 0x03 수신 |
| `raw-image` | Main → Renderer | CMD 0x04 수신 |

---

## 9. 개발 환경

### IDE
- Android Studio — 안드로이드 앱
- VS Code — Electron 앱

### 주요 패키지 / 라이브러리

| 플랫폼 | 패키지 |
|--------|--------|
| Android | Kotlin, Jetpack Compose, Hilt, Google Maps SDK, Places SDK for Android, Kotlin Coroutines, ExifInterface, Google Gemini AI SDK (generative-ai 0.9.0) |
| Electron | electron ^33, dotenv ^16, better-sqlite3, @googlemaps/markerclusterer, usb ^2.x (node-usb) |
| 네이티브 빌드 | `npx electron-rebuild` — better-sqlite3 · usb 모두 Electron ABI 재컴파일 필요 |

---

## 10. 개발 Phase

### Phase 1 — Android 앱

- [x] DataTransport 인터페이스 설계 (StateFlow / SharedFlow 기반)
- [x] TcpTransport 구현 (Coroutines, 설정 UI 연동)
- [x] UsbTransport 구현 (USB AOA — Accessory API, BroadcastReceiver, FileStream I/O)
- [x] TransportManager 구현 (sendToAll / sendTo / receivedData merge)
- [x] PacketBuilder / PacketParser 구현 (CMD 0x01~0x04, 스트리밍 파서)
- [x] 갤러리 GPS 파싱 + 마커/클러스터 표시
- [x] 마커/클러스터 클릭 → CMD 0x01 패킷 전송
- [x] CMD 0x02 수신 → CMD 0x03 썸네일 응답 (GoogleMapScreenViewModel)
- [x] 연결 설정 UI (앱 바 드롭다운 → TCP/USB 카드, 상태 표시)
- [x] AOA accessory_filter.xml 등록 (GPSMarkerViewer)

### Phase 2 — Electron 앱

- [x] 프로젝트 초기 셋업 (electron, dotenv, better-sqlite3, node-usb)
- [x] Google Maps JS API 지도 표시 (동적 스크립트 주입)
- [x] TcpReceiver 구현 (net.Server)
- [x] UsbReceiver 구현 (node-usb + AOA 핸드셰이크 + bulk IN/OUT 스트리밍)
- [x] PacketParser 구현 (스트리밍 파서 + 빌더, 0x01~0x04)
- [x] SQLite DB (마커 저장, 썸네일 캐시, 앱 재시작 시 복원)
- [x] CMD 0x01 수신 → 마커 누적 + 지도 자동 이동
- [x] CMD 0x02 전송 → CMD 0x03 수신 → InfoWindow/모달 썸네일 표시
- [x] CMD 0x04 수신 경로 준비 (Android 응답 시 자동 표시)
- [x] 마커 클러스터링 + 클러스터 클릭 → 이미지 목록 패널
- [x] TCP/USB 모드 전환 설정 UI

### Phase 3 — 양단 연동 및 검증

- [x] TCP 실기기 연동 테스트 완료
- [x] USB AOA 연동 테스트 완료
- [x] CMD 0x04 원본 이미지 전송 E2E 검증
- [x] E2E 전체 시나리오 최종 검증

### Phase 4 — Food Search (Android Tab 2)

- [x] Places SDK for Android 3.5.0 의존성 추가 (build.gradle)
- [x] PlacesClient / FusedLocationProviderClient DI 구성 (AppModule)
- [x] PlacesRepository 구현 (searchNearbyRestaurants + fetchPlacePhoto)
- [x] FoodSearchViewModel 구현 (위치 획득 + Nearby Search + 사진 비동기 로딩)
- [x] FoodSearchScreen Google Maps 기반 UI 구현 (지도 + 클러스터 + 바텀시트)
- [x] 이미지 마커 비동기 로딩 (기본 아이콘 → 음식 사진으로 교체)
- [x] 바텀시트 상세: 영업 상태·평점·가격대·영업시간·사진 그리드·리뷰
- [x] 클러스터 클릭 → 바텀시트 목록 → 항목 클릭 → 상세 정보
- [x] FAB(↺) 리프레시 버튼
- [x] Food Search 탭 추가 (Tab 2, Icons.Filled.Restaurant)
- [x] 실기기 동작 검증 완료

### Phase 5 — Gemini Chat (Android Tab 3)

- [x] Google Gemini AI SDK (generative-ai 0.9.0) 의존성 확인 + 모델 업그레이드 (→ gemini-2.5-flash)
- [x] Gemini API 키 local.properties 등록 (`GEMINI_API_KEY=`)
- [x] GeminiChatRoomScreen UI 전면 개선 (입력창 pill 형태, 메시지 버블 Material3, 아바타, 타이핑 인디케이터)
- [x] 예외 처리 (try-catch-finally, 에러 메시지 챗 UI 표시)
- [x] Structured Output 기반 의도 분류 (systemInstruction, GeminiIntent JSON 파싱)
- [x] PLACE_SEARCH → FusedLocation + PlacesRepository.searchNearbyPlaces() 연동
- [x] SearchCard 공용 카드 모델 (title/subtitle/badge/latLng/actionUrl)
- [x] ChatItem.CardResult → CardResultRow (LazyRow 수평 스크롤 카드 UI)
- [x] 카드 썸네일 비동기 로딩 + 실시간 업데이트
- [x] 장소 타입 매핑 25종 (음식/쇼핑/생활/시설/서비스)
- [x] 카드 클릭 → Google Maps geo: URI Intent
- [x] Room DB 대화 히스토리 저장/복원
- [x] LazyColumn key 중복 버그 수정
- [x] 실기기 동작 검증 완료 (일반 대화 + 장소 검색 + 카드 클릭)

---

## 11. 레포지토리 구조

```
portfolio-project/
├── android-app/              # 안드로이드 프로젝트
├── electron-app/             # Electron 프로젝트
├── docs/
│   ├── architecture.md
│   └── packet-protocol.md
├── .github/
│   └── workflows/
│       ├── android-build.yml
│       └── electron-build.yml
├── README.md
└── CLAUDE.md
```

---

## 12. 환경 셋업 체크리스트

### 맥북 환경
- [ ] Node.js v18+ 설치 확인 (`node -v`)
- [ ] VS Code 설치
- [ ] `npm install` → `npx electron-rebuild` (better-sqlite3, usb 네이티브 빌드)
- [ ] `.env.local`에 `GOOGLE_MAPS_API_KEY=` 등록

### 안드로이드 환경
- [ ] Android Studio 설치 및 프로젝트 열기
- [ ] USB 디버깅 활성화
- [ ] `local.properties`에 `MAPS_API_KEY=` 등록
- [ ] `local.properties`에 `GEMINI_API_KEY=` 등록

### API / 서비스
- [ ] Google Cloud 프로젝트에서 Maps JavaScript API + Maps Android SDK + Places API (New) 활성화
- [ ] Google AI Studio에서 Gemini API 키 발급
- [ ] API 키 발급 및 제한 설정
- [ ] GitHub Secrets에 MAPS_API_KEY, GEMINI_API_KEY 등록

---

## 13. 이력서 어필 포인트

> USB AOA (Android Open Accessory) / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계하여 런타임 교체 가능한 로컬 통신 모듈 구현

> Android ↔ macOS 간 커스텀 바이너리 패킷 프로토콜 설계 및 양단 구현 (Kotlin + Node.js)

> Electron(Node.js + Chromium) 기반 데스크탑 앱에서 Google Maps JS API 연동 및 실시간 마커 누적 표시 구현

> node-usb를 활용한 AOA 핸드셰이크 구현 — Android를 USB Accessory 모드로 전환하여 Bulk IN/OUT 스트리밍 통신 구현

> Google Places API 연동을 통한 현재 위치 기반 음식점 검색, 이미지 마커 비동기 로딩 및 바텀시트 상세 정보 표시 구현

> Google Gemini AI SDK Structured Output을 활용한 자연어 의도 분류 구현 — systemInstruction으로 JSON 응답을 강제하여 PLACE_SEARCH / GENERAL 의도를 파싱하고, Places API 검색 결과를 채팅 UI 내 카드로 표시

> 확장 가능한 AI 챗봇 아키텍처 설계 — SearchCard / ChatItem.CardResult 공용 구조로 새 검색 타입 추가 시 카드 UI 재사용 가능 (intent 케이스 + handleXxxSearch() 추가만으로 확장)
