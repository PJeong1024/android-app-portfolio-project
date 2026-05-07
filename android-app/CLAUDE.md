# Android 앱 명세 (Google Maps 마커 송신)

> 최종 업데이트: 2026-05-07  
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


## 13. 작업 히스토리

### 2026-05-07 — 코드 구조 진단 및 리팩토링 (Claude Code)

#### 긴급 수정 (crash / 초기화 문제)

- **`!!` 강제 언박싱 제거** (`GoogleMapsScreen.kt`)
  - `imageLat!!` / `imageLong!!` → `?: 37.461400` / `?: 126.452702` fallback 처리
  - GPS 없는 사진이 첫 번째로 오면 crash 나던 문제 해결

- **`fetchImagesToDb()` 호출 위치 이동** (`GoogleMapScreenViewModel.kt`)
  - `onMapLoaded` UI 콜백 → ViewModel `init`으로 이동
  - `getAllImages()`(DB 캐시 즉시 표시) + `fetchImagesToDb()`(갤러리 스캔 후 갱신) 순서로 init 구성

- **기본 탭 변경** (`MainScreen.kt`)
  - `WeatherApiScreen` → `GoogleMaps`로 변경 (포트폴리오 핵심 기능 우선 표시)

#### Phase 1 정리 — Repository 분리 및 Dead code 제거

- **`GoogleMapsRepository` 신규 생성** (`repository/GoogleMapsRepository.kt`)
  - `MyAppRepository`에서 지도 관련 로직 분리: `fetchImages`, `getAllImages`, `deleteUserImage`, `imageIsExist`, `readExifData`
  - `GoogleMapScreenViewModel`이 `MyAppRepository` 대신 `GoogleMapsRepository`를 주입받도록 변경

- **`MyAppRepository` 슬림화** (`repository/MyAppRepository.kt`)
  - Gemini Chat(`getAllMessage`, `insertMessage`, `deleteMessage`) + Weather API(`getWeather`)만 잔류
  - `ContentResolver`, `UserImgDao` 의존성 제거

- **`AppModule` 업데이트** (`di/AppModule.kt`)
  - `provideGoogleMapsRepository` 추가
  - `provideMyAppRepository` 파라미터에서 `ContentResolver`, `UserImgDao` 제거

- **DB 쿼리 중복 제거** (`GoogleMapScreenViewModel.kt`)
  - `getAllImages()` 두 번 호출하던 구조 → 한 번 쿼리 후 `existing` / `deleted` 분기 처리

- **Dead code 제거**
  - `FireBaseAuthViewModel`: 사용하지 않던 `MyAppRepository` 주입 제거
  - `PhotoBroadcastReceiver`: 전체 주석 처리된 코드 제거 (빈 `onReceive`만 유지)

---

### 2026-05-07 — 전체 코드베이스 리팩토링 Round 2 (Claude Code)

#### 버그 수정

- **`GeminiChatRoomViewModel.init` 레이스 컨디션 수정** (`GeminiChatRoomViewModel.kt`)
  - `getAllMessage()` 코루틴이 끝나기 전에 `isEmpty()` 체크 → 항상 true → 매 실행마다 인사 메시지 중복 삽입되던 문제 수정
  - `init`을 단일 코루틴으로 통합: DB 조회 후 비어 있을 때만 첫 메시지 삽입

- **`BottomBarImageListView` 클릭 핸들러 수정** (`GoogleMapsScreen.kt`)
  - 클러스터 목록에서 사진 탭해도 아무 동작 없던 버그 수정
  - 콜백 타입 `(String) -> Unit` → `(UserImg) -> Unit`으로 변경
  - 클릭 시 `selectedClusterItem` 갱신 후 `ImageItemState`로 상태 전환

- **`UserImgDao.getImage()` 타입 불일치 수정** (`UserImgDao.kt`)
  - 파라미터 `String` → `Int` 로 변경 (컬럼 타입과 일치)

- **`_isLoading` 초기값 수정** (`GeminiChatRoomViewModel.kt`)
  - `MutableStateFlow(true)` → `MutableStateFlow(false)`: 앱 진입 시 불필요한 로딩 스피너 제거

#### 아키텍처 개선

- **`UserImg` 엔티티에서 `ClusterItem` 분리** (`data/model/UserImg.kt`, `map/UserImgClusterItem.kt`)
  - DB 엔티티가 Google Maps SDK 인터페이스를 직접 구현하던 의존성 제거
  - `UserImgClusterItem(val source: UserImg) : ClusterItem` 래퍼 클래스 신규 생성
  - `GoogleMapsScreen`에서 `remember(imgList) { imgList.map { UserImgClusterItem(it) } }` 로 변환

- **파일 존재 동기화 로직 Repository로 이동** (`GoogleMapsRepository.kt`, `GoogleMapScreenViewModel.kt`)
  - ViewModel에 있던 `imageIsExist` + `deleteUserImage` 분기 로직 → `syncAndGetImages()` 메서드로 Repository에 캡슐화
  - ViewModel은 `syncAndGetImages()` 호출만 담당하도록 단순화

- **`MyAppRepository` → `ChatRepository` + `WeatherRepository` 분리** (`repository/`)
  - Gemini 채팅과 날씨 API라는 무관한 두 도메인을 분리
  - `ChatRepository`, `WeatherRepository` 신규 생성 (`@Singleton @Inject constructor`)
  - `MyAppRepository.kt` 삭제
  - `GeminiChatRoomViewModel`, `WeatherApiViewModel` 각각의 Repository 주입으로 변경

- **`GoogleMapsRepository` `@Singleton` 명시 및 DI 개선** (`GoogleMapsRepository.kt`, `AppModule.kt`)
  - 클래스에 `@Singleton` 추가 → `AppModule`의 중복 `@Provides` 제거
  - `ChatRepository`, `WeatherRepository`도 `@Inject constructor` 방식으로 Hilt 자동 제공

#### 코드 품질 개선

- **JPEG 필터 대소문자 처리** (`GoogleMapsRepository.kt`)
  - `!displayName.contains(".jpg")` → `lowercase().endsWith(".jpg") || .endsWith(".jpeg")` 로 변경
  - `.JPG`, `.JPEG` 확장자 누락 방지

- **`TAG` companion object 이동** (`GoogleMapsRepository.kt`)
  - 인스턴스 변수 `private val TAG` → `companion object { private const val TAG }`

- **`UserImg` 필드 `var` → `val` 불변 처리** (`UserImg.kt`)
  - 엔티티 데이터 변경 불가로 안전성 향상
  - `imageLat`, `imageLong` 기본값 `0.0` → `null` (0.0은 실제 좌표값이므로 null이 의미론적으로 정확)

- **`WeatherContent` 내부 Scaffold 제거** (`WeatherApiScreen.kt`)
  - bottom bar 없는 화면에서 불필요한 중첩 Scaffold 제거 → 단순 Column 구조로 변경
  - `FBottomBarState`, `bottomBarVisible`, `bottomBarState` dead state 제거
  - 함수명 `MainScreen` → `WeatherContent` (같은 패키지 내 네이밍 충돌 해소)

- **`isLoading` StateFlow 일관성** (`GeminiChatRoomViewModel.kt`)
  - `val isLoading: StateFlow<Boolean> = _isLoading` → `.asStateFlow()` 명시

#### Dead code 정리

| 파일 | 제거 항목 |
|------|----------|
| `AppModule.kt` | 주석 처리된 Firebase Book, BooksApi 코드 블록 |
| `Utils.kt` | 주석 처리된 `IngredientListConverter`, 미사용 `StringListConverter` |
| `Utils.kt` | 미사용 `formatDate()`, `formatDecimals()` |
| `Constants.kt` | 미사용 `NetworkConnectionTypes` 리스트 |
| `UserImgDao.kt` | 미사용 `deleteByImageID()` |
| `repository/MyAppRepository.kt` | 파일 전체 삭제 |