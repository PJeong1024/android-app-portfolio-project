# Electron 앱 명세 (Google Maps 마커 수신 뷰어)

> 최종 업데이트: 2026-05-07  
> 원본 프로젝트: GoogleMap 기반 Android ↔ macOS 크로스 플랫폼 연동

---

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
├── main.js              # Main Process (USB/TCP 수신, IPC)
├── preload.js           # 보안 브릿지
├── index.html           # Renderer (지도 UI)
├── renderer.js          # Maps JS API + IPC 수신
├── transport/
│   ├── UsbReceiver.js   # serialport 기반
│   └── TcpReceiver.js   # net.Server 기반
├── packet/
│   └── PacketParser.js  # 패킷 파싱
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

```javascript
const net = require('net')

const server = net.createServer((socket) => {
  socket.on('data', (data) => {
    mainWindow.webContents.send('new-location', parsePacket(data))
  })
})
server.listen(8080, '0.0.0.0')
```

---

## 7. 수신 패킷 프로토콜

### 패킷 구조

```
[STX 1byte][TYPE 1byte][LENGTH 4byte][PAYLOAD N byte][ETX 1byte][CHECKSUM 1byte]
```

### PAYLOAD 구조 (마커 데이터)

| 필드 | 타입 | 크기 |
|------|------|------|
| 위도 (latitude) | double | 8 byte |
| 경도 (longitude) | double | 8 byte |
| 이미지 크기 | int | 4 byte |
| 이미지 데이터 | byte[] | N byte (JPEG 압축) |

- 이미지는 512px 이하로 리사이즈 후 JPEG 압축 전송
- 대용량 이미지는 청크 분할 전송 검토 필요

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
- [x] IPC 채널 구조 (`get-config`, `new-location`) 뼈대
- [x] transport/, packet/ 디렉토리 및 파일 뼈대 생성
- [ ] UsbReceiver 구현 (serialport) — 미구현
- [ ] TcpReceiver 구현 (net.Server) — 미구현
- [ ] PacketParser 구현 — 미구현
- [ ] 패킷 파싱 → IPC → 마커 표시 연동

---

## 10. 개발 환경

| 항목 | 내용 |
|------|------|
| IDE | VS Code |
| Node.js | v18+ |
| 주요 패키지 | electron ^33, dotenv ^16 (serialport는 통신 구현 시 추가 예정) |
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