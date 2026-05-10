# Electron 앱 명세 (Google Maps 마커 수신 뷰어)

> 최종 업데이트: 2026-05-09  
> 원본 프로젝트: GoogleMap 기반 Android ↔ macOS 크로스 플랫폼 연동

---


## 작업간 주요 공지사항
- 작업 범위는 /electron-app 디렉토리 내 Electron 앱 개발로 한정
- 필요시, 다른 폴더의 md 파일 참조가 가능하지만, md 파일 수정은 불가함
- Electron 앱은 macOS / Windows 크로스 플랫폼 지원을 목표. 현재는 Mac에서 개발하고 Windows에서 실행되도록 추가 작업
- 작업간 모든 내역은 히스토리 섹션에 기록하여 향후 작업간 참조


## 1. 앱 개요

macOS에서 동작하는 Electron 앱.  
Android 앱으로부터 USB CDC 또는 TCP/IP로 GPS + 이미지 데이터를 수신하여  
Google Maps JavaScript API 위에 마커를 누적 표시한다.

### 핵심 기능
- USB (serialport) / TCP (net.Server) 이중 수신 지원
- 수신된 패킷을 파싱 → IPC → Renderer로 전달
- Google Maps 위에 마커 누적 표시 (마커 내 썸네일 포함)
- API 키 `.env.local` 파일로 관리

---

## 2. 유저 시나리오 (Electron 쪽)

1. Electron 앱 실행 → Google Maps 표시
2. Android 앱과 USB 또는 WiFi로 연결 대기
3. Android에서 마커 클릭 → 패킷 수신
4. 패킷 파싱 (GPS 좌표 + 이미지) → IPC → Renderer
5. Google Maps에 마커 누적 표시
6. 마커 클릭마다 반복 → 마커가 계속 추가됨

---

## 3. 시스템에서 Electron의 위치

```
[안드로이드 앱]
  마커 클릭 → 패킷 구성
  → DataTransport (USB or TCP)
        ↓
[macOS Electron 앱]  ← 여기
  DataReceiver (serialport or net.Server)
  → 패킷 파싱
  → IPC (Main → Renderer)
  → Maps JS API 마커 누적 표시
```

---

## 4. 프로젝트 구조

```
electron-app/
├── main.js              # Main Process (USB/TCP 수신, IPC, DB)
├── preload.js           # 보안 브릿지
├── index.html           # Renderer (지도 UI + 설정 패널 + 클러스터 패널)
├── renderer.js          # Maps JS API + IPC 수신 + 클러스터링 + DB 복원
├── transport/
│   ├── DataReceiver.js  # 추상 기반 클래스 (EventEmitter)
│   ├── TcpReceiver.js   # net.Server 기반 (Electron 서버, Android 클라이언트)
│   └── UsbReceiver.js   # serialport 기반 (스텁)
├── packet/
│   └── PacketParser.js  # 스트리밍 파서 + 패킷 빌더
├── db/
│   └── MarkerDatabase.js # SQLite DB (better-sqlite3)
├── .env.local           # API 키 (gitignore 처리)
└── package.json
```

---

## 5. 프로세스 역할 분담

| 프로세스 | 역할 |
|---------|------|
| Main Process | USB/TCP 수신, 패킷 파싱, IPC 송신 |
| Renderer Process | Google Maps 표시, 마커 누적, UI |

---

## 6. 통신 수신 구현

### USB 수신 (UsbReceiver.js)

```javascript
const SerialPort = require('serialport')
// /dev/tty.usbmodem* 자동 감지
```

### TCP 수신 (TcpReceiver.js)

- Electron이 TCP **서버**, Android가 **클라이언트**로 접속
- `net.createServer()` 로 0.0.0.0 바인딩, 동시 연결은 1개로 제한
- `DataReceiver` 추상 클래스 상속 — USB와 동일 이벤트 인터페이스 (`packet`, `client-connected`, `client-disconnected`, `error`)

---

## 7. 수신 패킷 프로토콜


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

## 8. Google Maps 연동

- **API**: Maps JavaScript API
- **API 키**: `.env.local` 파일로 관리, gitignore 처리
- **기능**: 수신 마커 누적 표시, 마커 내 썸네일 표시

### API 키 설정

```
# .env.local
GOOGLE_MAPS_API_KEY=your_key_here
```

- Main Process에서 `dotenv`로 로드 후 `ipcMain.handle('get-config')` 로 Renderer에 전달
- Renderer는 `window.electronAPI.getConfig()`로 키를 받아 Maps 스크립트를 동적으로 주입

### Maps JS API 로드 방식 (현재 구현)

스크립트를 정적 HTML에 넣지 않고 `renderer.js`에서 동적으로 생성:

```javascript
// renderer.js
const script = document.createElement('script')
script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async&callback=__mapsReady`
document.head.appendChild(script)
```

---

## 9. 개발 Phase (Electron 파트)

- [x] 프로젝트 초기 셋업 (npm, electron, dotenv)
- [x] 기본 창 띄우기
- [x] Google Maps JS API 지도 표시 (동적 스크립트 주입 방식)
- [x] API 키 `.env.local` 관리 + IPC로 Renderer 전달
- [x] preload.js 보안 브릿지 (contextIsolation)
- [x] IPC 채널 구조 (`get-config`, `get-all-markers`, `image-list`, `thumbnail`, `transport-status`, `request-images`, `tcp-start/stop`, `get-local-addresses`, `usb-list-devices`, `usb-start/stop`)
- [x] transport/, packet/, db/ 디렉토리 및 파일 구현
- [x] PacketParser 구현 — 안드로이드 프로토콜과 동일 (스트리밍 파서 + 빌더)
- [x] DataReceiver 추상 기반 클래스 (`transport/DataReceiver.js`)
- [x] TcpReceiver 구현 (net.Server — Electron 서버, Android 클라이언트)
- [x] UsbReceiver 구현 (node-usb + AOA) — AOA 핸드셰이크(컨트롤 전송), 액세서리 재연결 핫플러그 감지, Bulk IN/OUT 스트리밍, PacketParser 연동
- [x] 패킷 파싱 → IPC → 마커 표시 연동
- [x] TCP/USB 모드 전환 UI — 설정 패널 내 모드 토글, TCP 섹션(IP·포트·서버 제어), USB 섹션(장치 감지·AOA 연결)
- [x] 마커 클릭 → 썸네일 요청 → 비동기 InfoWindow 표시
- [x] SQLite DB (`better-sqlite3`) — 마커 수신 시 중복 체크 후 저장, 썸네일 캐시
- [x] 앱 시작 시 DB에서 마커 복원 + 지도 자동 이동 (fitBounds)
- [x] 새 마커 수신 시 해당 위치로 지도 자동 이동
- [x] 마커 클러스터링 (`@googlemaps/markerclusterer`) — 줌 레벨 연동 자동 클러스터
- [x] 클러스터 클릭 → 좌측 이미지 목록 패널 표시 (썸네일 미리보기 포함)
- [x] 목록 이미지 선택 → 모달에서 썸네일 async 로딩

---

## 10. 개발 환경

| 항목 | 내용 |
|------|------|
| IDE | VS Code |
| Node.js | v18+ |
| 주요 패키지 | electron ^33, dotenv ^16, better-sqlite3, @googlemaps/markerclusterer, usb ^2.x (node-usb, AOA) |
| 네이티브 모듈 빌드 | `npx electron-rebuild` — better-sqlite3 · usb 모두 Electron ABI에 맞게 재컴파일 필요 |
| 테스트 도구 | socat (가상 포트 테스트, `brew install socat`) |

---

## 11. 환경 셋업 체크리스트

- [ ] Node.js v18+ 설치 확인 (`node -v`)
- [ ] Claude Code 설치 (`npm install -g @anthropic-ai/claude-code`)
- [ ] VS Code Copilot 익스텐션 확인
- [ ] socat 설치 (`brew install socat`)
- [ ] Google Cloud 프로젝트에서 Maps JavaScript API 활성화
- [x] API 키 발급 및 `.env.local` 등록
- [x] `.gitignore`에 `.env.local` 추가 확인

---

## 12. GitHub Actions (Electron 자동 빌드)

```yaml
# .github/workflows/electron-build.yml
name: Electron CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - name: 의존성 설치
        run: npm install
      - name: Electron 빌드
        run: npm run build
        env:
          GOOGLE_MAPS_API_KEY: ${{ secrets.MAPS_API_KEY }}
```

---

## 13. 이력서 어필 포인트

> Electron(Node.js + Chromium) 기반 데스크탑 앱에서 Google Maps JS API 연동 및 실시간 마커 누적 표시 구현

> USB CDC / TCP Socket 이중 수신 방식을 추상화하여 런타임 교체 가능한 로컬 통신 모듈 설계

> Android ↔ macOS 간 커스텀 바이너리 패킷 프로토콜 설계 및 파싱 구현


## 14. 작업 히스토리 내역

- 2026-05-07 - Electron 앱 초기 셋업 및 Google Maps JS API 연동 완료
- 2026-05-09 - TCP 통신 모듈 구현 및 실기기(Android) 연동 테스트 완료
  - PacketParser (스트리밍 파서 + 빌더), DataReceiver 추상 클래스, TcpReceiver, UsbReceiver 스텁 구현
  - IPC 채널 전체 구성 (transport-status, image-list, thumbnail, request-images 등)
  - TCP 설정 UI (Mac IP 표시, 포트 설정, 서버 시작/중지, 연결 상태)
  - SQLite DB (better-sqlite3) — 마커 중복 체크 저장, 썸네일 캐시, 앱 재시작 시 복원
  - 수신 마커 기준 지도 자동 이동 (fitBounds / panTo)
  - @googlemaps/markerclusterer 클러스터링 — 클러스터 클릭 시 이미지 목록 패널 + async 썸네일 로딩
  - better-sqlite3 Electron ABI 불일치 수정 (electron-rebuild)
- 2026-05-10 - USB 통신 모듈 구현 (serialport → AOA 정공법으로 전환), TCP/USB 모드 전환 UI 완성, 0x04 원본 이미지 수신 경로 준비
  - [1차] serialport 기반 CDC 구현 → 실기기 테스트에서 Android가 Host API(UsbManager) 사용 구조 확인 → AOA로 방향 전환
  - [2차 AOA] serialport 제거, node-usb(`usb` ^2.x) 설치 + electron-rebuild
  - UsbReceiver 전면 재작성 — AOA 핸드셰이크(ACCESSORY_GET_PROTOCOL/SEND_STRING/START 컨트롤 전송), handshake-done 이벤트, usb attach 핫플러그로 액세서리 재연결 감지(VID 0x18D1, PID 0x2D00/0x2D01), Bulk IN/OUT 엔드포인트 스트리밍 폴링, PacketParser 연동
  - main.js — usb-list-devices / usb-start / usb-stop IPC 핸들러, handshake-done 이벤트 IPC 전달
  - preload.js — usbListDevices / usbStart / usbStop API 노출
  - index.html — TCP/USB 모드 토글 버튼, USB 섹션(장치 감지 표시 + 새로고침 + 연결 버튼) 추가
  - renderer.js — setupTcpSection / setupUsbSection 분리, handleUsbStatus(handshake-done/connected/disconnected/error) 처리
  - 0x04 원본 이미지 수신 경로 준비 (동작은 0x03 유지) — PacketParser.parseRawImage(), main.js CMD.RAW_IMAGE_RESPONSE 분기, preload.js onRawImage, renderer.js onRawImage 핸들러 (pending 모달/InfoWindow에 원본 이미지 표시)