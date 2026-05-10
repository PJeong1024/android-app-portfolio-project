'use strict'
const { EventEmitter } = require('events')

const CMD = {
  IMAGE_LIST: 0x01,        // Android → Electron
  IMAGE_DATA_REQUEST: 0x02, // Electron → Android
  THUMBNAIL_RESPONSE: 0x03, // Android → Electron
  RAW_IMAGE_RESPONSE: 0x04  // Android → Electron (future)
}

const STX = 0x02
const ETX = 0x03
const HEADER_SIZE = 6  // STX(1) + CMD(1) + LENGTH(4)
const FOOTER_SIZE = 2  // CHECKSUM(1) + ETX(1)
const MIN_PACKET_SIZE = HEADER_SIZE + FOOTER_SIZE
const MAX_PAYLOAD_SIZE = 10 * 1024 * 1024  // 10 MB sanity cap

/**
 * Stateful streaming parser — safe to call feed() with partial or multi-packet chunks.
 * Emits 'packet'(cmd, payload) for each valid, checksum-verified packet.
 *
 * Packet format (matches Android PacketBuilder.kt):
 *   [STX 0x02][CMD 1B][LENGTH 4B big-endian][PAYLOAD NB][CHECKSUM 1B][ETX 0x03]
 *   CHECKSUM = (CMD + LENGTH bytes + PAYLOAD bytes) % 256
 */
class PacketParser extends EventEmitter {
  constructor() {
    super()
    this._buf = Buffer.alloc(0)
  }

  feed(data) {
    this._buf = Buffer.concat([this._buf, data])
    let packet
    while ((packet = this._tryExtract()) !== null) {
      this.emit('packet', packet.cmd, packet.payload)
    }
  }

  _tryExtract() {
    // Discard bytes before next STX
    let start = 0
    while (start < this._buf.length && this._buf[start] !== STX) start++
    if (start > 0) this._buf = this._buf.slice(start)

    if (this._buf.length < MIN_PACKET_SIZE) return null

    const cmd = this._buf[1]
    const length = this._buf.readUInt32BE(2)

    if (length > MAX_PAYLOAD_SIZE) {
      this._buf = this._buf.slice(1)
      return null
    }

    const totalSize = HEADER_SIZE + length + FOOTER_SIZE
    if (this._buf.length < totalSize) return null

    const checksumPos = HEADER_SIZE + length
    const etxPos = checksumPos + 1

    if (this._buf[etxPos] !== ETX) {
      this._buf = this._buf.slice(1)
      return null
    }

    const payload = this._buf.slice(HEADER_SIZE, HEADER_SIZE + length)

    let sum = cmd
    for (let i = 2; i <= 5; i++) sum += this._buf[i]
    for (let i = 0; i < payload.length; i++) sum += payload[i]
    const expectedChecksum = sum % 256

    if (this._buf[checksumPos] !== expectedChecksum) {
      this._buf = this._buf.slice(1)
      return null
    }

    this._buf = this._buf.slice(totalSize)
    return { cmd, payload }
  }

  reset() {
    this._buf = Buffer.alloc(0)
  }

  // --- Static builders (Electron → Android) ---

  static build(cmd, payloadStr) {
    const payload = Buffer.from(payloadStr, 'utf8')
    const lengthBuf = Buffer.alloc(4)
    lengthBuf.writeUInt32BE(payload.length, 0)

    let sum = cmd
    for (const b of lengthBuf) sum += b
    for (const b of payload) sum += b
    const checksum = sum % 256

    return Buffer.concat([
      Buffer.from([STX, cmd]),
      lengthBuf,
      payload,
      Buffer.from([checksum, ETX])
    ])
  }

  static buildImageRequest(imageIDs) {
    return PacketParser.build(CMD.IMAGE_DATA_REQUEST, JSON.stringify({ imageIDs }))
  }

  // --- Static parsers (Android → Electron) ---

  static parseImageList(payload) {
    try {
      return JSON.parse(payload.toString('utf8'))  // { items: [{imageID, imageDisplayName, imageLat, imageLong}] }
    } catch { return null }
  }

  static parseThumbnail(payload) {
    try {
      return JSON.parse(payload.toString('utf8'))  // { imageID, thumbnailData: "base64..." }
    } catch { return null }
  }

  static parseRawImage(payload) {
    try {
      return JSON.parse(payload.toString('utf8'))  // { imageID, imageData: "base64..." }
    } catch { return null }
  }
}

module.exports = { PacketParser, CMD }
