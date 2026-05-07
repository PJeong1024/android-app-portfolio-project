# Android 앱 명세 (Google Maps 마커 송신)

> 최종 업데이트: 2026-04-21  
> 원본 프로젝트: GoogleMap 기반 Android ↔ macOS 크로스 플랫폼 연동

---

## 1. 앱 개요

갤러리 사진의 GPS 메타데이터를 파싱하여 Google Maps 위에 마커를 표시하고,  
마커 클릭 시 GPS 좌표 + 이미지 데이터를 USB CDC 또는 TCP/IP로 macOS Electron 앱에 전송한다.

### 핵심 기능
- 갤러리 사진 GPS 메타데이터 파싱
- Google Maps SDK로 마커 + 썸네일 표시
- USB (USB Host API) / TCP (Socket) 이중 송신 지원
- 런타임 교체 가능한 DataTransport 추상화 인터페이스 설계
- 확장성을 고려한 모듈화 및 DI 설계

---

## 2. 유저 시나리오 (Android 쪽)

1. 앱 실행 → 갤러리 사진 로드 → GPS 메타데이터 파싱
2. Google Maps 위에 마커 + 썸네일 표시
3. 설정 탭에서 연결 방식 선택 (USB 자동 감지 or TCP IP/Port 입력)
4. 마커 클릭 → GPS 좌표 + 이미지를 패킷으로 구성
5. DataTransport(USB or TCP)로 macOS에 전송
6. 마커 클릭마다 반복 송신

---

## 3. 시스템에서 Android의 위치

```
[Android 앱]  ← 여기
  갤러리 로드 → GPS 파싱 → 지도 마커 표시
  → 마커 클릭 → 패킷 구성
  → DataTransport (USB or TCP)
        ↓
[macOS Electron 앱]
  DataReceiver → 패킷 파싱 → 마커 누적 표시
```

---

## 4. 탭 구성

```
MainActivity
├── Tab 1 : Google Maps (핵심 기능)
│           ├── 사진 GPS 파싱
│           ├── 마커 + 썸네일 표시
│           └── 마커 클릭 → 패킷 전송
├── Tab 2 : 연결 설정
│           ├── 연결 방식 선택 (USB / TCP)
│           ├── TCP 설정 (IP, Port) ← TCP 선택 시만 활성화
│           └── 연결 상태 표시
└── Tab N : 기타 기능 (추후 확장)
```

---

## 5. 모듈 구조

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

## 6. 통신 추상화 인터페이스

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

---

## 7. 송신 패킷 프로토콜

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

## 8. 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Kotlin |
| 지도 | Google Maps SDK for Android |
| 통신 | USB Host API, TCP Socket |
| IDE | Android Studio |
| API 키 관리 | `local.properties` → `MAPS_API_KEY` |

---

## 9. 개발 Phase (Android 파트)

- [ ] DataTransport 인터페이스 설계
- [ ] UsbTransport 구현 (자동 연결)
- [ ] TcpTransport 구현 (설정 UI 연동)
- [ ] 기존 Google Maps 코드 리팩토링
- [ ] 마커 클릭 → 패킷 전송 구현
- [ ] 탭 구조 DI 정리

**완료 기준**
- DataTransport를 Mock으로 교체해도 앱 정상 동작
- 마커 클릭 시 패킷 송신 로직이 UI 코드에서 완전 분리

---

## 10. 개발 환경 체크리스트

- [ ] Android Studio 설치 및 프로젝트 열기
- [ ] Android Studio Copilot 플러그인 설치
- [ ] USB 디버깅 활성화
- [ ] Google Cloud 프로젝트에서 Maps Android SDK 활성화
- [ ] API 키 발급 → `local.properties`에 `MAPS_API_KEY=` 등록
- [ ] `.gitignore`에 `local.properties` 추가 확인

---

## 11. GitHub Actions (Android 자동 빌드)

```yaml
# .github/workflows/android-build.yml
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

---

## 12. 이력서 어필 포인트

> USB CDC / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계하여 런타임 교체 가능한 로컬 통신 모듈 구현

> Android ↔ macOS 간 커스텀 바이너리 패킷 프로토콜 설계 및 구현

> Google Maps SDK를 활용한 GPS 메타데이터 파싱 및 마커 + 썸네일 표시 구현