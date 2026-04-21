# 패킷 프로토콜 명세

## 기본 구조

```
┌──────┬──────┬────────┬───────────┬──────┬──────────┐
│ STX  │ TYPE │ LENGTH │  PAYLOAD  │ ETX  │ CHECKSUM │
│ 1byte│ 1byte│ 4byte  │  N byte   │ 1byte│  1byte   │
└──────┴──────┴────────┴───────────┴──────┴──────────┘
```

| 필드 | 크기 | 값 | 설명 |
|------|------|----|------|
| STX | 1 byte | 0x02 | 패킷 시작 |
| TYPE | 1 byte | 아래 참고 | 패킷 종류 |
| LENGTH | 4 byte | N | PAYLOAD 크기 (빅엔디안) |
| PAYLOAD | N byte | - | 데이터 |
| ETX | 1 byte | 0x03 | 패킷 종료 |
| CHECKSUM | 1 byte | XOR | STX~ETX XOR 합산 |

---

## 패킷 타입

| TYPE | 값 | 설명 |
|------|----|------|
| MARKER_DATA | 0x01 | 마커 데이터 (GPS + 이미지) |
| ACK | 0x02 | 수신 확인 |
| PING | 0x03 | 연결 상태 확인 |

---

## MARKER_DATA PAYLOAD 구조

```
┌──────────┬──────────┬────────────┬──────────────────┐
│ latitude │ longitude│ image_size │   image_data     │
│  8 byte  │  8 byte  │   4 byte   │     N byte       │
└──────────┴──────────┴────────────┴──────────────────┘
```

| 필드 | 타입 | 크기 | 설명 |
|------|------|------|------|
| latitude | double | 8 byte | 위도 (빅엔디안) |
| longitude | double | 8 byte | 경도 (빅엔디안) |
| image_size | int | 4 byte | 이미지 바이트 크기 |
| image_data | byte[] | N byte | JPEG 압축 이미지 |

---

## 이미지 처리 규칙

- 전송 전 512px 이하로 리사이즈
- JPEG 압축 (quality 80)
- 분할 전송 기준: 64KB 초과 시 청크 분할

---

## CHECKSUM 계산

```kotlin
// Android (Kotlin)
fun calculateChecksum(data: ByteArray): Byte {
    return data.fold(0.toByte()) { acc, byte -> (acc.toInt() xor byte.toInt()).toByte() }
}
```

```javascript
// Electron (JavaScript)
function calculateChecksum(buffer) {
  return buffer.reduce((acc, byte) => acc ^ byte, 0)
}
```

---

## 패킷 예시

```
02          <- STX
01          <- TYPE (MARKER_DATA)
00 00 00 14 <- LENGTH (20 bytes = lat 8 + lng 8 + image_size 4)
[latitude 8 bytes]
[longitude 8 bytes]
[image_size 4 bytes]
[image_data N bytes]
03          <- ETX
XX          <- CHECKSUM
```
