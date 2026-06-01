# Android 앱 명세

> 최종 업데이트: 2026-06-01  
> 원본 프로젝트: Google Maps + Places API 기반 포트폴리오 Android 앱

---

## 작업간 주요 공지사항
- 작업 범위는 /android-app 디렉토리 내 Android 앱 개발로 한정
- **[중요] 세션 시작 시 이 파일(`android-app/CLAUDE.md`)만 주 참조로 사용할 것. 상위 폴더(`portfolio-project/CLAUDE.md`)는 읽지 않아도 됨. 내용 충돌 시 이 파일이 항상 우선.**
- 필요시, 다른 폴더의 md 파일 참조가 가능하지만, md 파일 수정은 불가함
- 작업간 모든 내역은 히스토리 섹션에 기록하여 향후 작업간 참조

## 1. 앱 개요

Google Maps SDK와 Places API를 중심으로 두 가지 독립적인 지도 기반 기능을 제공하는 포트폴리오 Android 앱.

### Tab 1 — Google Maps Marker
갤러리 사진의 GPS 메타데이터를 파싱하여 Google Maps 위에 마커/클러스터를 표시하고,  
마커 클릭 시 이미지 데이터를 USB AOA 또는 TCP/IP로 macOS Electron 앱에 전송한다.

### Tab 2 — Food Search
Google Places API를 활용하여 현재 위치 기반 주변 음식점을 검색하고,  
이미지 마커로 지도에 표시하며 가게 상세 정보(영업시간·평점·리뷰·사진)를 제공한다.

### Tab N — 추후 확장
기능 탭은 지속적으로 추가될 예정. 각 탭은 독립적인 기능 단위로 구성되며 현재 구조(Hilt DI, ViewModel, Compose)를 그대로 활용한다.

### 공통 기술 요소
- USB AOA / TCP Socket 이중 통신, DataTransport 추상화 인터페이스 (Tab 1 전용)
- 확장성을 고려한 모듈화 및 DI 설계 (Hilt)
- Jetpack Compose 기반 UI, ViewModel + StateFlow 아키텍처

---

## 2. 유저 시나리오

### Tab 1 — Google Maps Marker
1. 앱 실행 → 갤러리 사진 로드 → GPS 메타데이터 파싱
2. Google Maps 위에 마커/클러스터 표시
3. 앱 바 설정(⚙) → 연결 방식 선택 (USB 자동 감지 or TCP IP/Port 입력)
4. 마커 클릭 → GPS 좌표 + 이미지 목록을 패킷으로 구성 → macOS 전송
5. macOS Electron 앱이 수신하여 지도에 마커 표시, 이미지 요청 시 썸네일 응답

### Tab 2 — Food Search
1. 탭 진입 → 위치 권한 확인 (없으면 자동 요청)
2. 현재 위치 기반 1km 반경 음식점 자동 검색
3. 가게별 이미지 마커 비동기 로딩 (기본 아이콘 → 이미지 마커 교체)
4. 마커 클릭 → 바텀 시트에 가게 상세 정보 표시 (영업 상태, 평점, 가격대, 영업시간, 사진, 리뷰)
5. 클러스터 클릭 → 바텀 시트 가게 목록 → 항목 선택 → 상세 정보
6. FAB(↺) → 주변 검색 재실행

---

## 3. 시스템 구조

### Tab 1 — Google Maps Marker
```
[Android 앱]
  갤러리 로드 → GPS 파싱 → 지도 마커/클러스터 표시
  → 마커 클릭 → PacketBuilder → CMD 0x01 전송
  → DataTransport (UsbTransport or TcpTransport)
        ↓                         ↑
        ↓ CMD 0x01 (image list)   │ CMD 0x02 (image request)
        ↓                         │ CMD 0x03 (thumbnail response)
[macOS Electron 앱]
  DataReceiver → PacketParser → 마커 누적 표시
  → 마커 클릭 → CMD 0x02 전송 → Android 응답 대기 → 썸네일 표시
```

### Tab 2 — Food Search
```
[Android 앱]
  위치 권한 확인 → FusedLocationProvider → 현재 GPS 좌표
  → PlacesRepository.searchNearbyRestaurants() → Place 목록 수신
  → 기본 마커 표시 → fetchPlacePhoto() 병렬 로딩 → 이미지 마커 교체
  → 마커/클러스터 클릭 → BottomSheet 상세 정보 표시
     (영업 상태 / 평점 / 가격대 / 영업시간 / 사진 그리드 / 리뷰)
```

---

## 4. 탭 구성

> 탭 공식 명칭: **Tab 1 = "Google Maps Marker"**, **Tab 2 = "Food Search"**  
> (BottomNaviBarScreen enum label 기준, 앱 하단 탭 표시 텍스트와 동일)

```
MainActivity
├── Tab 1 : Google Maps Marker ✅ 완료
│           ├── 갤러리 GPS 파싱 + DB 저장
│           ├── 마커/클러스터 표시 (UserImgClusterItem + Clustering)
│           ├── 마커 클릭 → CMD 0x01 패킷 전송 (USB AOA / TCP)
│           └── CMD 0x02 수신 → CMD 0x03/0x04 이미지 응답
├── Tab 2 : Food Search ✅ 완료
│           ├── 위치 권한 확인 + 현재 위치 기반 자동 검색
│           ├── Places API 음식점 마커/클러스터 표시
│           ├── 이미지 마커 비동기 로딩 (기본 아이콘 → 음식 사진으로 교체)
│           ├── 마커 클릭 → 바텀 시트 상세 (영업상태·평점·가격대·영업시간·사진·리뷰)
│           ├── 클러스터 클릭 → 바텀 시트 목록 → 항목 클릭 → 상세
│           └── FAB(↺) 리프레시 버튼
├── 앱 바 설정 아이콘(⚙) → 드롭다운 ✅ 완료
│           └── Googlemap 설정 → ConnectionSettingsScreen 전체화면
│                   ├── TCP 카드: IP/Port 입력 + 연결/해제
│                   └── USB 카드: 감지 시작/중지 + 상태 표시
└── Tab N : 기타 기능 (Chat, Weather, Firebase Auth)
```

---

## 5. 모듈 구조

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
│   ├── PacketBuilder.kt            # 패킷 프레이밍
│   └── PacketParser.kt             # 스트리밍 파서
├── repository/
│   ├── GoogleMapsRepository.kt     # 이미지 로드
│   └── PlacesRepository.kt         # Places API Nearby Search (searchNearbyRestaurants)
├── di/
│   ├── AppScope.kt                 # @ApplicationScope qualifier
│   └── AppModule.kt                # Hilt 모듈 (PlacesClient + FusedLocationClient 포함)
└── screens/
    ├── GoogleMapsScreen.kt          # Tab 1: Google Maps Marker
    ├── FoodSearchScreen.kt          # Tab 2: Food Search (지도 + 클러스터 + 바텀시트 + 이미지 그리드 + 리뷰)
    ├── ConnectionSettingsScreen.kt  # 연결 설정 UI (TCP/USB)
    └── viewmodel/
        ├── GoogleMapScreenViewModel.kt    # 마커 전송, CMD 0x02/0x03/0x04 처리
        ├── FoodSearchViewModel.kt         # 위치 획득 + Nearby Search + 사진 비동기 로딩
        └── ConnectionSettingsViewModel.kt  # TCP/USB 연결 상태 관리
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
| USB AOA | 앱 바 설정 → USB 카드 "감지 시작" → 케이블 연결 → 자동 연결 | "감지 시작" 버튼 클릭 |
| TCP/IP | 설정 UI에서 IP/Port 입력 | IP + Port 입력 후 연결 |

---

## 7. 패킷 프로토콜


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

## 9. 개발 Phase (Android 파트)

### Phase 1~3 — GPS Marker Image Viewer + 통신 연동 ✅ 완료

- [x] DataTransport 인터페이스 설계 (StateFlow / SharedFlow 기반)
- [x] TcpTransport 구현 (Coroutines, 설정 UI 연동)
- [x] UsbTransport 구현 (USB AOA — Accessory API, BroadcastReceiver, FileStream I/O)
- [x] TransportManager 구현 (sendToAll / sendTo / receivedData merge)
- [x] PacketBuilder / PacketParser 구현 (CMD 0x01~0x04, 스트리밍 파서)
- [x] 갤러리 GPS 파싱 + 마커/클러스터 표시
- [x] 마커/클러스터 클릭 → CMD 0x01 패킷 전송
- [x] CMD 0x02 수신 → CMD 0x03 썸네일 응답
- [x] CMD 0x04 원본 이미지 응답 구현
- [x] 연결 설정 UI (앱 바 드롭다운 → TCP/USB 카드)
- [x] AOA accessory_filter.xml 등록 (GPSMarkerViewer)
- [x] TCP / USB AOA 실기기 연동 테스트 완료
- [x] E2E 전체 시나리오 최종 검증 완료

### Phase 4 — Food Image Search Viewer (진행 중)

- [x] Places SDK for Android 3.5.0 의존성 추가 (libs.versions.toml + build.gradle)
- [x] PlacesClient DI 구성 (AppModule — initializeWithNewPlacesApiEnabled + createClient)
- [x] FusedLocationProviderClient DI 구성 (AppModule)
- [x] PlacesRepository 구현 (searchNearbyRestaurants — SearchNearbyRequest, CircularBounds 1km)
- [x] FoodSearchViewModel 구현 (위치 획득 + Nearby Search + StateFlow)
- [x] FoodSearchScreen 기본 UI 구현 (권한 체크 + 검색 버튼 + 결과 리스트 카드)
- [x] FoodSearch 탭 추가 (Tab 2, Icons.Filled.Restaurant)
- [x] Google Cloud Console에서 "Places API (New)" 활성화 (사용자 직접 수행)
- [x] Places API 실기기 동작 검증 (음식점 리스트 수신 확인)
- [x] FoodSearchScreen Google Maps 기반 UI 전면 재작성
  - [x] 탭 진입 시 자동 위치 획득 + 자동 주변 검색 (버튼 제거)
  - [x] 내 위치 파란점 표시 (MapProperties.isMyLocationEnabled)
  - [x] 카메라 자동 이동 (LaunchedEffect + animate)
  - [x] 음식점 마커 클러스터링 (PlaceClusterItem + Clustering)
  - [x] 단독 마커 클릭 → 바텀바 PlaceDetail (이름/평점/주소 스크롤)
  - [x] 클러스터 클릭 → 바텀바 PlaceList LazyColumn → 항목 클릭 → PlaceDetail
  - [x] FAB 리프레시 버튼 (Refresh 아이콘, 재검색 트리거)
  - [x] 에러 시 상단 Card 표시
- [x] PlaceClusterItem 신규 생성 (map/PlaceClusterItem.kt)
- [x] 이미지 마커 비동기 로딩 (PlacesRepository.fetchPlacePhoto, 기본 아이콘 → 이미지 마커 교체)
- [x] 클러스터 리스트 썸네일 표시 (FoodBottomBarPlaceList)
- [x] 가게 상세 사진 3열 그리드 (FoodBottomBarPlaceDetail + PhotoGrid)
- [x] 영업 상태 chip 표시 (BusinessStatus: 폐업/임시휴업, isOpen(): 영업 종료)
- [x] 가격대 표시 (PRICE_LEVEL → ₩~₩₩₩₩)
- [x] 영업시간 표시 (OPENING_HOURS.weekdayText)
- [x] 리뷰 목록 표시 (REVIEWS: 별점 + 작성자 + 시간 + 본문)
- [x] 탭 공식 명칭 확정: Tab 1 = "Google Maps Marker", Tab 2 = "Food Search"
- [x] BottomNaviBarScreen.GoogleMaps label 변경 ("Google Maps" → "Google Maps Marker")
- [x] CLAUDE.md 문서 전면 업데이트 (앱 개요/시나리오/시스템 구조/탭 구성 재작성)

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

> USB AOA / TCP Socket 이중 통신 방식을 추상화 인터페이스로 설계하여 런타임 교체 가능한 로컬 통신 모듈 구현

> Android ↔ macOS 간 커스텀 바이너리 패킷 프로토콜 설계 및 구현 (CMD 0x01~0x04)

> Google Maps SDK를 활용한 GPS 메타데이터 파싱 및 마커/클러스터 + 썸네일 표시 구현

> Google Places API 연동을 통한 현재 위치 기반 음식점 검색 및 이미지 마커 표시 구현

## 13. 작업 히스토리

### 사전 전제사항 
- 수행한 작업들은 작업의 경중을 떠나서 모든 작업들이 해당 항목에 기록이 저장되어야한다. 
- 이를 통하여 작업의 진행상황과 진행여부 그리고 향후 진행계획 까지 파악 및 수립이 가능하다.
- 신규 구현/수정/삭제등 의 코드작업이 진행되는간에 반드시 모든 작업들은 히스토리 기록이 자동으로 이루어져야한다. 

### 2026-06-01 — 문서 전면 개편 + 탭 명칭 확정 (Claude Code)

#### 변경 내용

- **`utils/Constants.kt`** — `BottomNaviBarScreen.GoogleMaps` label `"Google Maps"` → `"Google Maps Marker"`
- **`CLAUDE.md`** 전면 개편
  - 제목: "Google Maps 마커 송신" 중심 → 멀티 피처 앱으로 재포지셔닝
  - Section 1 (앱 개요): Tab 1 / Tab 2 동등 병렬 소개 구조로 재작성
  - Section 2 (유저 시나리오): Tab 1 / Tab 2 시나리오 분리 기술
  - Section 3: "시스템에서 Android의 위치" → "시스템 구조" 로 확장 (두 탭 모두 다이어그램 포함)
  - Section 4 (탭 구성): 공식 명칭 명시, 완료 상태 반영
  - Section 5 (모듈 구조): FoodSearchScreen 설명 현행화
  - Phase 4 체크리스트: 완료 항목 전부 체크

#### 탭 공식 명칭 (확정)
- Tab 1: **Google Maps Marker** — 갤러리 GPS 파싱 + 마커 표시 + USB/TCP Electron 연동
- Tab 2: **Food Search** — Places API 음식점 검색 + 이미지 마커 + 상세 정보

---

### 2026-06-01 — Phase 4 가게 상세 정보 확장 (Claude Code)

#### 변경 내용

- **`repository/PlacesRepository.kt`** — `placeFields`에 5개 추가: `BUSINESS_STATUS`, `CURRENT_OPENING_HOURS`, `OPENING_HOURS`, `PRICE_LEVEL`, `REVIEWS`
- **`screens/FoodSearchScreen.kt`**
  - `BusinessStatusChip`: `CLOSED_PERMANENTLY`(폐업·빨강), `CLOSED_TEMPORARILY`(임시휴업·주황), `isOpen()==false`(영업종료·회색) — 헤더 우측 표시
  - 평점 행에 가격대(₩~₩₩₩₩) 함께 표시
  - 영업시간 섹션: `openingHours.weekdayText` 7일 전체 표시
  - 사진 그리드 (기존 유지)
  - 리뷰 섹션: 별점 5개 표시 + 작성자 + 상대 시간 + 본문 3줄

---

### 2026-06-01 — Phase 4 이미지 마커 + 사진 그리드 구현 (Claude Code)

#### 배경
마커에 음식 이미지 표시, 클러스터 리스트 썸네일, 가게 상세 사진 그리드 구현.
Places API 구조상 `searchNearby`에서 이미지 실데이터는 불가 (메타데이터만 수신) → 마커 먼저 표시 후 비동기 이미지 로딩 방식 채택.

#### 수정 파일

- **`repository/PlacesRepository.kt`** — `fetchPlacePhoto(photoMetadata, maxWidth, maxHeight): Result<Bitmap>` 추가
  - `FetchPhotoRequest.builder(photoMetadata)` → `placesClient.fetchPhoto()` → Bitmap 반환

- **`screens/viewmodel/FoodSearchViewModel.kt`**
  - `_placePhotoThumbnails: MutableStateFlow<Map<String, Bitmap>>` — placeId → 썸네일 (마커/리스트용)
  - `_selectedPlacePhotos: MutableStateFlow<List<Bitmap>>` — 현재 선택된 가게 전체 사진 (상세용)
  - `loadThumbnailsAsync(places)`: `searchNearby` 완료 후 각 가게 첫 번째 사진 병렬 로딩 (200×200)
  - `loadPhotosForPlace(place)`: 상세 진입 시 모든 사진 병렬 로딩 (600×600)
  - `searchNearby()`: 리프레시 시 thumbnails/selectedPhotos 초기화

- **`screens/FoodSearchScreen.kt`**
  - `PlaceMarkerContent(bitmap)`: 로딩 완료 → 이미지 원형 마커, 미완료 → Restaurant 아이콘
  - `Clustering.clusterItemContent` → `PlaceMarkerContent` 적용 (State 변경 시 자동 재렌더)
  - `FoodBottomBarPlaceList`: 썸네일 56dp 라운드 박스 + 가게명/주소/평점
  - `FoodBottomBarPlaceDetail`: 텍스트 정보 + `PhotoGrid` (사진 로딩 중 `CircularProgressIndicator`)
  - `PhotoGrid`: 3열 고정 그리드 (LazyVerticalGrid 미사용 — verticalScroll 중첩 불가)
  - 마커/리스트 항목 클릭 시 `viewModel.loadPhotosForPlace(place)` 호출

#### 동작 흐름
```
searchNearby() 완료
  → _places 업데이트 → 기본 마커(Restaurant 아이콘) 즉시 표시
  → loadThumbnailsAsync() 병렬 실행
    → 각 가게 fetchPhoto() 완료 → _placePhotoThumbnails 업데이트
    → clusterItemContent 재구성 → 이미지 마커로 자동 교체

마커/클러스터 항목 클릭
  → loadPhotosForPlace() 병렬 실행
    → 각 사진 fetchPhoto() 완료 → _selectedPlacePhotos에 순차 추가
    → PhotoGrid 실시간 업데이트
```

---

### 2026-06-01 — Phase 4 FoodSearchScreen Google Maps UI 전면 재작성 (Claude Code)

#### 배경
기존 FoodSearchScreen은 버튼 클릭 + 리스트 표시 방식이었음. 탭 진입 시 자동 시작 + 지도 기반 마커/클러스터 UI로 전면 재작성.

#### 신규 파일

- **`map/PlaceClusterItem.kt`** — Place → ClusterItem 래퍼
  - `getPosition()`: `place.latLng` 기반
  - `getTitle()`: `place.name`, `getSnippet()`: `place.address`

#### 수정 파일

- **`screens/viewmodel/FoodSearchViewModel.kt`**
  - `_myLocation: MutableStateFlow<LatLng?>` + `myLocation` StateFlow 추가
  - `searchNearby()`: 위치 획득 후 `_myLocation.value = location` 설정

- **`screens/FoodSearchScreen.kt`** — 전면 재작성
  - 탭 진입 시 권한 없으면 자동 권한 팝업, 있으면 `LaunchedEffect(Unit)`로 자동 `searchNearby()`
  - `GoogleMap` + `MapProperties(isMyLocationEnabled = true)` → 내 위치 파란점
  - `LaunchedEffect(myLocation)` → `cameraPositionState.animate()` 자동 카메라 이동
  - `Clustering(PlaceClusterItem)` → 마커/클러스터 표시
  - `FoodBottomBarState` 3상태: Empty / PlaceList / PlaceDetail
    - 단독 마커 클릭 → `PlaceDetailState` (이름+평점+주소, verticalScroll)
    - 클러스터 클릭 → `PlaceListState` LazyColumn → 항목 클릭 → `PlaceDetailState`
    - 뒤로가기: 클러스터 출신이면 PlaceList, 단독이면 Empty
  - FAB `Refresh` 버튼 → `viewModel.searchNearby()` 재호출
  - 로딩 중 `CircularProgressIndicator`, 에러 시 상단 `Card` 표시

#### 빌드 결과
BUILD SUCCESSFUL

---

### 2026-06-01 — Places SDK 필드명 수정 (Claude Code)

#### 배경
Places SDK 3.5.0의 `Place.Field` enum에는 `DISPLAY_NAME`, `LOCATION`, `FORMATTED_ADDRESS`가 없음. 구버전 필드명으로 수정.

#### 수정 내용

| 잘못된 필드 | 올바른 필드 (3.5.0) |
|---|---|
| `Place.Field.DISPLAY_NAME` | `Place.Field.NAME` |
| `Place.Field.LOCATION` | `Place.Field.LAT_LNG` |
| `Place.Field.FORMATTED_ADDRESS` | `Place.Field.ADDRESS` |
| `place.displayName?.text` | `place.name` |
| `place.formattedAddress` | `place.address` |

- **`repository/PlacesRepository.kt`** — `placeFields` 리스트 수정
- **`screens/FoodSearchScreen.kt`** — `PlaceCard` 프로퍼티 접근 수정

---

### 2026-05-28 — Phase 4 준비: Places SDK 추가 + FoodSearch 탭 신규 생성 (Claude Code)

#### 배경
Phase 4 (Food Image Search Viewer) 구현을 위한 기반 준비. Places API 연동 테스트를 위한 별도 탭 및 기본 스캐폴딩 구성.

#### 신규 파일

- **`repository/PlacesRepository.kt`** — Places API 연동 Repository
  - `searchNearbyRestaurants(location, radiusMeters, maxCount)`: 현재 위치 기반 음식점 Nearby Search
  - `SearchNearbyRequest` + `CircularBounds` 기반, `Place.Field` 7개 요청 (ID, DISPLAY_NAME, LOCATION, RATING, FORMATTED_ADDRESS, TYPES, PHOTO_METADATAS)
  - `suspendCancellableCoroutine`으로 콜백 → suspend 변환
- **`screens/viewmodel/FoodSearchViewModel.kt`** — FoodSearch 전용 HiltViewModel
  - `searchNearby()`: FusedLocationProviderClient로 현재 위치 획득 → PlacesRepository.searchNearbyRestaurants() 호출
  - `places`, `isLoading`, `error` StateFlow 노출
- **`screens/FoodSearchScreen.kt`** — Food Search 테스트 탭 UI
  - `accompanist-permissions`로 위치 권한 체크 (권한 없으면 요청 뷰 표시)
  - "주변 음식점 검색 (1km 반경)" 버튼 → ViewModel.searchNearby() 호출
  - 검색 결과 LazyColumn: 음식점 이름 + 평점 + 주소 카드

#### 수정 파일

- **`gradle/libs.versions.toml`** — `places = "3.5.0"` 버전 추가, `places` 라이브러리 항목 추가
- **`app/build.gradle.kts`** — `implementation(libs.places)` 의존성 추가
- **`di/AppModule.kt`** — 2개 Provider 추가
  - `provideFusedLocationClient()`: `LocationServices.getFusedLocationProviderClient(context)`
  - `providePlacesClient()`: `Places.initializeWithNewPlacesApiEnabled()` + `Places.createClient()` (기존 MAPS_API_KEY 재사용)
- **`utils/Constants.kt`** — `FoodSearch` 탭 추가 (`Icons.Filled.Restaurant`, Tab 2 위치)
- **`screens/MainScreen.kt`** — `CurrentScreen`에 `FoodSearch → FoodSearchScreen` 케이스 추가

#### 외부 설정 필요 사항
- Google Cloud Console에서 해당 API 키에 **"Places API (New)"** 활성화 필요 (사용자에게 안내)

---

### 2026-05-10 — CMD 0x02 수신 핸들러 + 0x03/0x04 응답 구현 (Claude Code) ✅ 동작 검증 완료

#### 배경
`TransportManager.receivedData`를 아무도 구독하지 않아 Electron → Android CMD 0x02(이미지 요청)를 수신해도 아무 처리 없이 버려지던 상태. 0x04 페이로드/빌더/파서도 미구현.

#### 신규 파일

- **`data/model/packet/RawImagePayload.kt`** — CMD 0x04 페이로드 data class (`imageID`, `imageData: String` Base64)

#### 수정 파일

- **`packet/PacketBuilder.kt`** — `buildRawImageResponse(imageID, imageBytes)` 추가
- **`packet/PacketParser.kt`** — `parseRawImage(payload)` 추가 (RawImagePayload 역직렬화)
- **`repository/GoogleMapsRepository.kt`** — 이미지 로딩 메서드 2개 추가
  - `loadThumbnailBytes(imagePath)`: 최장변 ≤ 512px로 다운스케일, JPEG 75% 품질
  - `loadRawImageBytes(imagePath)`: 원본 크기, JPEG 90% 품질
  - BitmapFactory 기반, inSampleSize로 메모리 효율적 축소
- **`GoogleMapScreenViewModel.kt`** — 핵심 변경
  - `packetParsers: Map<ConnectionType, PacketParser>` — 소스별 독립 파서 (스트림 버퍼 상태 유지)
  - `init` 블록에 `transportManager.receivedData` 구독 추가
  - CMD 0x02 수신 → `respondWithThumbnail()` 호출 (Dispatchers.IO)
  - `respondWithThumbnail(imageID, sourceType)`: 0x03 응답 → 요청 온 transport로만 `sendTo()`
  - `respondWithRawImage(imageID, sourceType)`: 0x04 응답 구현 완료, 현재 미호출 (`@Suppress("unused")`)

#### 동작 흐름 (현재 활성)

```
Electron → CMD 0x02 (imageID 리스트) → TCP/USB 수신
  → TransportManager.receivedData 방출
  → GoogleMapScreenViewModel 구독
  → PacketParser.feed() → IMAGE_DATA_REQUEST 파싱
  → imageID별 respondWithThumbnail() (IO 디스패처)
    → _userImages에서 imageID 조회
    → GoogleMapsRepository.loadThumbnailBytes()
    → PacketBuilder.buildThumbnailResponse()
    → TransportManager.sendTo(sourceType, packet)
  → Electron 수신 CMD 0x03 → InfoWindow 썸네일 표시
```

---

### 2026-05-10 — AOA accessory_filter 등록 (Claude Code)

- **`res/xml/accessory_filter.xml`** — 신규 생성
  - Electron 측이 AOA 핸드셰이크에서 전송하는 문자열과 매칭: `manufacturer="GPSMarkerViewer" model="GPSMarkerViewer"`
- **`AndroidManifest.xml`** — MainActivity에 추가
  - `USB_ACCESSORY_ATTACHED` intent-filter 등록 (USB 케이블 연결 시 앱 자동 연결 트리거)
  - `android.hardware.usb.action.USB_ACCESSORY_ATTACHED` meta-data로 `accessory_filter.xml` 연결

---

### 2026-05-10 — UsbTransport AOA 방식으로 전환 (Claude Code)

#### 배경
USB Host API(Android가 주변기기 탐색)로는 Android↔Mac 직접 통신 불가.
Android가 Mac에 연결될 때 Android는 USB Device 역할이므로 USB Accessory(AOA) API가 올바른 구현.
Electron 측도 `serialport` → `node-usb` + AOA 핸드셰이크로 변경 필요(Electron 쪽은 별도 진행).

#### 변경 내용

- **`transport/UsbTransport.kt`** — USB Host API 전면 교체 → USB Accessory API
  - `UsbDevice` / `bulkTransfer` 방식 제거
  - `UsbAccessory` / `ParcelFileDescriptor` 기반으로 재작성
  - `usbManager.accessoryList` 로 이미 연결된 액세서리 탐지
  - `openAccessory()` → `FileInputStream` / `FileOutputStream` 스트림 I/O (TCP와 동일 구조)
  - read loop: `inputStream.read()` 블로킹 — -1이면 호스트 종료로 Disconnected 전환
  - `send()`: `outputStream.write()` + `flush()`
  - BroadcastReceiver: `ACTION_USB_ACCESSORY_ATTACHED` / `ACTION_USB_ACCESSORY_DETACHED` / 권한 응답
  - `isDisconnecting` 플래그로 의도적 해제 시 Error 전환 억제

- **`AndroidManifest.xml`** — `android.hardware.usb.host` → `android.hardware.usb.accessory`

#### AOA 연결 흐름

```
① Android: "감지 시작" 클릭 → UsbTransport.connect() → BroadcastReceiver 등록
② Electron(Mac): node-usb로 Android 장치 감지 → AOA 핸드셰이크(제조사·모델 문자열 전송)
③ Android: 액세서리 모드로 재시작 → ACTION_USB_ACCESSORY_ATTACHED 수신
④ Android: 권한 요청 → 승인 → openAccessory() → Connected
⑤ 이후 패킷 송수신은 TCP와 동일한 스트림 구조
케이블 분리: ACTION_USB_ACCESSORY_DETACHED → Disconnected 자동 전환
```

---

### 2026-05-10 — USB Transport 전체 구현 (Claude Code)

#### UsbTransport.kt 전체 구현 (스켈레톤 → 실 동작)

- **`AndroidManifest.xml`** — `android.hardware.usb.host` feature 선언 추가 (`required=false`)
- **`transport/UsbTransport.kt`** — USB Host API 전체 구현
  - `BroadcastReceiver` 동적 등록/해제: `ACTION_USB_PERMISSION` / `ACTION_USB_DEVICE_ATTACHED` / `ACTION_USB_DEVICE_DETACHED`
  - `connect()`: 이미 연결된 디바이스 있으면 즉시 permission 요청 → 없으면 Connecting 상태로 attach 대기
  - `requestOrOpen()`: 권한 있으면 `openDevice()`, 없으면 `UsbManager.requestPermission()` 호출
  - `openDevice()`: CDC Data 인터페이스 탐색 → `claimInterface()` → `Connected` 상태 전환 → read loop 시작
  - `findBulkEndpoints()`: 1차 CDC Data class(0x0A) 우선 탐색, 없으면 bulk 엔드포인트 pair가 있는 인터페이스 fallback
  - read loop: `bulkTransfer(inEp, buffer, size, 200ms)` 폴링, `>0` 수신/`0` timeout 재시도/`<0` 오류 처리
  - `send()`: `bulkTransfer(outEp, packet, size, 3000ms)` → IO 디스패처
  - `disconnect()`: read job cancel → connection close → BroadcastReceiver unregister → Disconnected 전환
  - `isDisconnecting` 플래그로 detach 이벤트/오류 시 의도적 해제와 구분
  - API 33+ `RECEIVER_NOT_EXPORTED` 대응

- **`ConnectionSettingsViewModel.kt`** — USB connect/disconnect 메서드 추가
  - `connectUsb()` / `disconnectUsb()` 추가
  - `isUsbConnectEnabled`: `Disconnected || Error` 상태일 때 활성화

- **`ConnectionSettingsScreen.kt`** — USB 카드 UI 업데이트
  - "감지 시작" 버튼 (Disconnected/Error 상태) → `connectUsb()` 호출
  - "감지 중지" 버튼 (Connected/Connecting 상태) → `disconnectUsb()` 호출
  - Connecting 상태 안내 문구: "USB 디바이스 연결을 기다리는 중..."

#### 동작 흐름 (TCP와 동일 추상화 레벨)

```
감지 시작 버튼 클릭
  → UsbTransport.connect()
  → 디바이스 없음: Connecting 대기 (BroadcastReceiver 등록)
  → 디바이스 꽂힘: ACTION_USB_DEVICE_ATTACHED
  → permission 요청 → 승인
  → openDevice() → Connected
  → bulkTransfer read loop 시작
마커 클릭
  → TransportManager.sendToAll() → UsbTransport.send()
  → bulkTransfer(outEp, packet)
케이블 뽑힘: ACTION_USB_DEVICE_DETACHED → Disconnected 자동 전환
```

---

### 2026-05-10 — 설정 UI 재배치 + 재연결 버그 수정 (Claude Code)

#### 1. 연결 설정 진입 경로 변경 (탭 → 상단 앱 바 드롭다운)

- **`Constants.kt`** — `ConnectionSettings` enum 항목 및 `items` 리스트에서 제거, `Icons.Filled.Settings` import 제거
- **`Components.kt`** — `SkillsTestAppBar`에 `additionalActions: @Composable RowScope.() -> Unit` 슬롯 추가, `navigationIcon` 블록 추가 (icon 파라미터 실제 사용)
- **`MainScreen.kt`** — 설정 드롭다운 상태(`showSettingsMenu`, `showConnectionSettings`) 추가
  - 상단 앱 바 오른쪽에 설정 아이콘(⚙) 추가
  - 클릭 시 드롭다운: "Googlemap 설정" / "Chat 설정" / "Weather 설정"
  - "Googlemap 설정" 클릭 → 기존 `ConnectionSettingsScreen` 전체 화면으로 진입
  - 설정 화면 진입 시 하단 탭 숨김, 앱 바에 뒤로가기 화살표(←) 표시
  - `CurrentScreen`에서 `ConnectionSettings` case 제거
  - Chat / Weather 설정은 리스트 항목만 구현 (향후 확장용)

#### 2. TCP 재연결 버그 수정

- **`ConnectionSettingsViewModel.kt`** — `isTcpConnectEnabled` 조건 수정
  - 기존: `tcpState is Disconnected` 일 때만 연결 버튼 활성화
  - 수정: `tcpState is Disconnected || tcpState is Error` 일 때 활성화
  - 효과: 연결 해제 후 Error 상태가 되어도 재연결 버튼 활성화

---

### 2026-05-09 — 통신 레이어 전체 구현 + 로그 추가 (Claude Code)

#### 1. 패킷 프로토콜 레이어 신규 구현

- **`data/transport/ConnectionType.kt`** — `enum class ConnectionType { USB, TCP }`
- **`data/transport/TransportState.kt`** — `sealed class`: `Disconnected` / `Connecting` / `Connected` / `Error(message)`
- **`data/transport/DataTransport.kt`** — 추상화 인터페이스 (`StateFlow<TransportState>` + `SharedFlow<ByteArray>`)
- **`data/model/packet/PacketCommand.kt`** — CMD enum (0x01~0x04)
- **`data/model/packet/ImageListPayload.kt`** — CMD 0x01 페이로드
- **`data/model/packet/ImageRequestPayload.kt`** — CMD 0x02 페이로드
- **`data/model/packet/ThumbnailPayload.kt`** — CMD 0x03 페이로드
- **`packet/PacketBuilder.kt`** — 패킷 프레이밍 object (`build`, `buildImageList`, `buildImageRequest`, `buildThumbnailResponse`)
- **`packet/PacketParser.kt`** — 스트리밍 파서 class (`feed` → 청크 수신 대응, 체크섬 검증)

#### 2. Transport 구현체 신규 구현

- **`transport/TcpTransport.kt`** — TCP 소켓 구현체 (`@Singleton`)
  - `configure(ip, port)` 런타임 설정
  - `Dispatchers.IO` 기반 connect / read loop / send
  - `isDisconnecting` 플래그로 의도적 해제 시 Error 상태 방지
- **`transport/UsbTransport.kt`** — USB 구현체 스켈레톤 (USB Host API 구현 예정)
- **`transport/TransportManager.kt`** — USB + TCP 동시 관리
  - `sendToAll(packet)` / `sendTo(type, packet)` / `isAnyConnected`
  - `receivedData`: 양쪽 merge + ConnectionType 태깅
- **`di/AppScope.kt`** — `@ApplicationScope` qualifier
- **`di/AppModule.kt`** — `provideApplicationScope()` 추가

#### 3. 마커 클릭 → 패킷 전송 연결

- **`GoogleMapScreenViewModel.kt`** — `TransportManager` 주입, `sendMarkerData(List<UserImg>)` 추가
- **`GoogleMapsScreen.kt`** — `onClusterClick` / `onClusterItemClick` 에서 `viewModel.sendMarkerData()` 호출

#### 4. 연결 설정 탭 (Tab 2) 신규 구현

- **`ConnectionSettingsViewModel.kt`** — TCP IP/Port 상태 관리, `connectTcp()` / `disconnectTcp()`, port 유효성 검사(1~65535)
- **`ConnectionSettingsScreen.kt`** — TCP / USB 카드 UI, 연결 상태 색상 표시(초록/노랑/회색/빨강), 연결 중 입력 필드 disabled
- **`Constants.kt`** — `ConnectionSettings` 탭 추가 (2번째, `Icons.Filled.Settings`)
- **`MainScreen.kt`** — `CurrentScreen`에 ConnectionSettings 케이스 추가

#### 5. 로그 추가 (에러 분석용)

| 파일 | 로그 포인트 |
|------|------------|
| `PacketBuilder` | build 시 cmd / payload크기 / 체크섬, 각 helper 메서드 호출 내용 |
| `PacketParser` | feed 수신 바이트 수, STX 이전 garbage 바이트 경고, 불완전 패킷 대기, ETX 불일치·체크섬 오류·미지원 CMD 경고, 파싱 성공/실패 |
| `TcpTransport` | configure / connect 시도·성공·실패, recv 바이트 수, send 바이트 수·실패, 상태 전환, disconnect |
| `TransportManager` | sendToAll 대상 transport 목록, sendTo 타겟, 연결 없을 때 dropped 경고 |

- 레벨 기준: `Log.d` 정상흐름 / `Log.i` 상태전환 / `Log.w` 복구가능 이상 / `Log.e` 연결·파싱 오류

#### 6. 동작 검증

- Android ↔ Electron 앱 간 TCP 통신 동작 확인 완료

---

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

### 2026-05-09 — 마커 클릭 → 패킷 전송 + 연결 설정 탭 구현 (Claude Code)

#### 핵심 기능: 마커 클릭 → Electron 전송

- **`GoogleMapScreenViewModel.kt`** — `TransportManager` 주입, `sendMarkerData(List<UserImg>)` 추가
  - 단독 마커 클릭 → `listOf(image)` 1개짜리 리스트로 전송
  - 클러스터 클릭 → 클러스터 전체 이미지 리스트 전송
  - 내부적으로 `UserImg → ImageListItem` 변환 후 `PacketBuilder.buildImageList()` → `transportManager.sendToAll(packet)`

- **`GoogleMapsScreen.kt`** — 클릭 핸들러에 `viewModel.sendMarkerData()` 연결
  - `onClusterClick` → `sendMarkerData(images)` 호출
  - `onClusterItemClick` → `sendMarkerData(listOf(image))` 호출
  - 기존 UI 상태 변경(`bottomBarState`, `selectedCluster` 등) 유지하면서 전송 추가

#### 연결 설정 탭 (Tab 2)

- **`ConnectionSettingsViewModel.kt`** — 신규 `@HiltViewModel`
  - `tcpState`, `usbState`: `TransportManager`에서 각 transport의 `StateFlow` 직접 노출
  - `tcpIp`, `tcpPort`: 입력 상태 관리 (port는 숫자만, 최대 5자리 validation)
  - `connectTcp()`: `configure(ip, port)` → `connect()` 순서 보장
  - `isTcpConnectEnabled`: IP 비어있지 않음 + port 유효(1~65535) + Disconnected 상태 일 때만 true

- **`ConnectionSettingsScreen.kt`** — 신규 Composable
  - TCP 카드: IP/Port 입력 필드 + 연결/해제 버튼 + 상태 표시
  - USB 카드: 상태 표시 + 자동 감지 안내 문구
  - 상태 색상: Connected=초록 / Connecting=노랑 / Disconnected=회색 / Error=빨강
  - 연결 중·연결됨 상태에서는 IP/Port 필드 disabled 처리

#### 탭 구조 업데이트

- **`Constants.kt`** — `ConnectionSettings` 탭 추가 (`Icons.Filled.Settings`, 2번째 위치)
- **`MainScreen.kt`** — `CurrentScreen`에 `ConnectionSettings` → `ConnectionSettingsScreen` 케이스 추가

---

### 2026-05-09 — TcpTransport / UsbTransport / TransportManager 구현 (Claude Code)

#### transport/ 구현체 신규 생성

- **`TcpTransport.kt`** — `DataTransport` TCP 구현체 (`@Singleton`)
  - `configure(ip, port)` 런타임 설정 메서드 (설정 UI 연동 포인트)
  - `connect()` → `Dispatchers.IO`에서 소켓 연결 후 read loop 진입
  - read loop: 4KB 버퍼 단위로 수신, `_receivedData.emit()` 으로 방출
  - `isDisconnecting` 플래그로 의도적 연결 해제 시 `Error` 상태 전환 억제
  - `send()` → `Dispatchers.IO`에서 `OutputStream.write()` + `flush()`

- **`UsbTransport.kt`** — `DataTransport` USB 구현체 스켈레톤 (`@Singleton`)
  - 인터페이스 구조 완성, 내부는 TODO (USB Host API: UsbManager, BroadcastReceiver, bulkTransfer)
  - `connect()` 호출 시 "USB not yet implemented" Error 상태 반환

- **`TransportManager.kt`** — USB + TCP 동시 관리 (`@Singleton`)
  - `sendToAll(packet)` — 현재 Connected 상태인 모든 transport에 브로드캐스트
  - `sendTo(ConnectionType, packet)` — 특정 transport에만 송신
  - `receivedData: Flow<Pair<ConnectionType, ByteArray>>` — 양쪽 receivedData merge, 출처 태깅
  - `isAnyConnected` — 하나라도 연결됐는지 확인

#### DI 업데이트

- **`di/AppScope.kt`** — `@ApplicationScope` qualifier annotation 신규 생성
- **`di/AppModule.kt`** — `provideApplicationScope()` 추가
  - `CoroutineScope(SupervisorJob() + Dispatchers.Default)` → TcpTransport/UsbTransport에 주입
  - `TcpTransport`, `UsbTransport`, `TransportManager` 는 `@Singleton @Inject constructor` 방식으로 Hilt 자동 제공

#### 동시 활성화 지원 설계

- USB, TCP 각각 독립적인 `StateFlow<TransportState>` 관리 → 서로 영향 없음
- `TransportManager.sendToAll()` 로 양쪽 동시 송신 가능
- `TransportManager.receivedData` merge로 양쪽 수신 데이터 단일 스트림 처리

---

### 2026-05-09 — 통신 프로토콜 레이어 구현 (Claude Code)

#### transport/ 패키지 신규 생성

- **`ConnectionType.kt`** — `enum class ConnectionType { USB, TCP }`
- **`TransportState.kt`** — `sealed class TransportState`: `Disconnected` / `Connecting` / `Connected` / `Error(message)`
- **`DataTransport.kt`** — 통신 추상화 인터페이스
  - `val state: StateFlow<TransportState>` (MD 초안의 `isConnected: Boolean`을 StateFlow로 격상 → UI 옵저빙 + 에러 상태 표현)
  - `val receivedData: SharedFlow<ByteArray>` (MD 초안의 콜백 대신 SharedFlow → 여러 collector 지원, 코루틴 친화적)
  - `fun connect()` / `fun send(packet: ByteArray)` / `fun disconnect()`

#### packet/ 패키지 신규 생성

- **`PacketCommand.kt`** — CMD 코드 enum (`IMAGE_LIST 0x01`, `IMAGE_DATA_REQUEST 0x02`, `THUMBNAIL_RESPONSE 0x03`, `RAW_IMAGE_RESPONSE 0x04`)
- **`payload/ImageListPayload.kt`** — CMD 0x01 페이로드 (`ImageListItem` + `ImageListPayload`)
- **`payload/ImageRequestPayload.kt`** — CMD 0x02 페이로드 (`imageIDs: List<Int>`)
- **`payload/ThumbnailPayload.kt`** — CMD 0x03 페이로드 (`imageID` + Base64 `thumbnailData`)
- **`PacketBuilder.kt`** — 패킷 프레이밍 object
  - `build(command, payload)` — `[STX][CMD][LENGTH 4B][PAYLOAD][CHECKSUM][ETX]` 조립
  - `buildImageList()` / `buildImageRequest()` / `buildThumbnailResponse()` 헬퍼
  - CHECKSUM = `(CMD + LENGTH bytes + PAYLOAD bytes) % 256`
- **`PacketParser.kt`** — 스트리밍 파서 class
  - `ArrayDeque<Byte>` 내부 버퍼로 청크 수신 대응 (패킷이 여러 조각에 걸쳐 와도 안전)
  - `feed(ByteArray): List<ParsedPacket>` — 완성된 패킷만 반환, 불완전한 패킷은 버퍼에 유지
  - STX 탐색 → 길이 파싱 → 체크섬 검증 → ETX 확인 순서로 파싱, 불량 패킷은 스킵 후 재탐색
  - 페이로드 역직렬화 헬퍼: `parseImageList()` / `parseImageRequest()` / `parseThumbnail()`
  - `reset()` — 버퍼 초기화 (재연결 시 사용)

#### 설계 원칙 적용 사항

| 원칙 | 적용 내용 |
|------|----------|
| USB/TCP 가변 | `DataTransport` 인터페이스로 완전 추상화, 구현체 런타임 교체 가능 |
| 모듈 독립성 | `packet/` 레이어는 Android 의존성 최소화, payload 모델은 순수 Kotlin data class |
| 확장성 | `PacketCommand`에 CMD 추가만으로 새 패킷 타입 지원 |
| 안전성 | 체크섬 검증 실패 / ETX 불일치 / 길이 이상(>10MB) 패킷은 자동 스킵 |

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