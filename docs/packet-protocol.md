# Packet Protocol Specification

## Frame Structure

```
[STX 1B][CMD 1B][LENGTH 4B][PAYLOAD NB][CHECKSUM 1B][ETX 1B]
```

| Field | Size | Value | Description |
|-------|------|-------|-------------|
| STX | 1 byte | `0x02` | packet start |
| CMD | 1 byte | see below | packet type |
| LENGTH | 4 bytes | N (big-endian) | payload length in bytes |
| PAYLOAD | N bytes | UTF-8 JSON | serialized data |
| CHECKSUM | 1 byte | `(CMD + LENGTH bytes + PAYLOAD bytes) % 256` | integrity check |
| ETX | 1 byte | `0x03` | packet end |

---

## CMD Types

| CMD | Name | Direction | Status |
|-----|------|-----------|--------|
| `0x01` | Image List | Android → Electron | ✅ implemented |
| `0x02` | Image Request | Electron → Android | ✅ implemented |
| `0x03` | Thumbnail Response | Android → Electron | ✅ implemented |
| `0x04` | Raw Image Response | Android → Electron | path ready, send not yet triggered |

---

## Payload Schemas (JSON)

### CMD 0x01 — Image List

Sent when the user taps a marker or cluster on the Android map.

```json
{
  "images": [
    {
      "imageId": "string",
      "imageDisplayName": "string",
      "imageLat": 37.1234,
      "imageLong": 127.5678
    }
  ]
}
```

### CMD 0x02 — Image Request

Sent from Electron when the user clicks a marker on the macOS map.

```json
{
  "imageIds": ["string", "string"]
}
```

### CMD 0x03 — Thumbnail Response

Sent from Android in response to CMD 0x02. One packet per image.

```json
{
  "imageId": "string",
  "thumbnailData": "<Base64-encoded JPEG>"
}
```

### CMD 0x04 — Raw Image Response

Full-resolution image response (receive path implemented; send not yet triggered).

```json
{
  "imageId": "string",
  "imageData": "<Base64-encoded JPEG>"
}
```

---

## Checksum Calculation

```kotlin
// Android (Kotlin)
fun checksum(cmd: Byte, lengthBytes: ByteArray, payload: ByteArray): Byte {
    var sum = cmd.toInt() and 0xFF
    for (b in lengthBytes) sum += b.toInt() and 0xFF
    for (b in payload) sum += b.toInt() and 0xFF
    return (sum % 256).toByte()
}
```

```javascript
// Electron (Node.js)
function checksum(cmd, lengthBuf, payload) {
  let sum = cmd;
  for (const b of lengthBuf) sum += b;
  for (const b of payload) sum += b;
  return sum % 256;
}
```

---

## Packet Flow

```
① Android marker/cluster tap
   → PacketBuilder.buildImageList() → CMD 0x01 → Electron
   → Electron: save to SQLite + accumulate on map

② Electron marker click
   → CMD 0x02 (imageId list) → Android
   → Android: load image → PacketBuilder.buildThumbnailResponse() → CMD 0x03
   → Electron: display in InfoWindow / modal
```

---

## Parser Notes

- Both sides use a **streaming parser** with an internal byte buffer (`ArrayDeque` on Android, `Buffer` on Node.js).
- Incomplete frames are held in the buffer until the remaining bytes arrive.
- A frame is accepted only if `CHECKSUM` matches and `ETX` is `0x03`; otherwise it is discarded.
- `LENGTH` is read as a 4-byte big-endian unsigned integer.
