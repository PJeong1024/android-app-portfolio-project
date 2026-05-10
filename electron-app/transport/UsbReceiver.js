'use strict'
const { usb } = require('usb')   // named export — has on/off/getDeviceList
const { DataReceiver } = require('./DataReceiver')
const { PacketParser } = require('../packet/PacketParser')

// Android Open Accessory constants
const AOA_VID = 0x18D1
const AOA_PID_ACCESSORY     = 0x2D00  // accessory only
const AOA_PID_ACCESSORY_ADB = 0x2D01  // accessory + ADB

const ACCESSORY_GET_PROTOCOL = 51  // bRequest: read AOA protocol version
const ACCESSORY_SEND_STRING  = 52  // bRequest: send identification string
const ACCESSORY_START        = 53  // bRequest: switch to accessory mode

// Identification strings sent during handshake (must match Android accessory_filter.xml)
const AOA_STRINGS = [
  'GPSMarkerViewer',                // 0: manufacturer
  'GPSMarkerViewer',                // 1: model
  'GPS Marker Viewer',              // 2: description
  '1.0',                            // 3: version
  'https://github.com/pjeong1024', // 4: URI
  '00000001',                       // 5: serial
]

// Known Android USB vendor IDs for initial device detection
const ANDROID_VIDS = new Set([
  0x18D1, // Google
  0x22B8, // Motorola
  0x04E8, // Samsung
  0x0BB4, // HTC
  0x2717, // Xiaomi
  0x1004, // LG
  0x0E8D, // MediaTek
  0x04D9, // Holtek (some OEMs)
  0x19D2, // ZTE
  0x12D1, // Huawei
])

class UsbReceiver extends DataReceiver {
  constructor() {
    super()
    this._device     = null
    this._iface      = null
    this._inEndpoint = null
    this._outEndpoint = null
    this._parser     = new PacketParser()
    this._onAttach   = null
    this._onDetach   = null

    this._parser.on('packet', (cmd, payload) => {
      this.emit('packet', cmd, payload)
    })
  }

  // Returns list of detected Android / AOA devices (no open required)
  static listDevices() {
    return usb.getDeviceList()
      .filter((d) => {
        const vid = d.deviceDescriptor.idVendor
        const pid = d.deviceDescriptor.idProduct
        return ANDROID_VIDS.has(vid) ||
          (vid === AOA_VID && (pid === AOA_PID_ACCESSORY || pid === AOA_PID_ACCESSORY_ADB))
      })
      .map((d) => ({
        vendorId:    d.deviceDescriptor.idVendor,
        productId:   d.deviceDescriptor.idProduct,
        isAccessory: d.deviceDescriptor.idVendor === AOA_VID &&
          (d.deviceDescriptor.idProduct === AOA_PID_ACCESSORY ||
           d.deviceDescriptor.idProduct === AOA_PID_ACCESSORY_ADB),
      }))
  }

  start() {
    this._parser.reset()

    // Register hotplug listeners BEFORE handshake so we catch the reconnect
    this._onAttach = (device) => {
      if (!this._device && this._isAccessoryDevice(device)) {
        this._openAccessoryDevice(device)
      }
    }
    this._onDetach = (device) => {
      if (device === this._device) {
        this._cleanup()
        this.emit('client-disconnected')
      }
    }
    usb.on('attach', this._onAttach)
    usb.on('detach', this._onDetach)

    // Already in accessory mode?
    const accessory = usb.getDeviceList().find((d) => this._isAccessoryDevice(d))
    if (accessory) {
      this._openAccessoryDevice(accessory)
      return
    }

    // Find regular Android device and start AOA handshake
    const android = usb.getDeviceList().find(
      (d) => ANDROID_VIDS.has(d.deviceDescriptor.idVendor)
    )
    if (!android) {
      process.nextTick(() => {
        this.emit('error', new Error('Android 장치를 찾을 수 없습니다. USB 연결 후 다시 시도하세요.'))
      })
      return
    }

    this._performAoaHandshake(android).catch((err) => this.emit('error', err))
  }

  _isAccessoryDevice(device) {
    const vid = device.deviceDescriptor.idVendor
    const pid = device.deviceDescriptor.idProduct
    return vid === AOA_VID && (pid === AOA_PID_ACCESSORY || pid === AOA_PID_ACCESSORY_ADB)
  }

  // AOA handshake: control transfers → Android switches mode and reconnects
  async _performAoaHandshake(device) {
    await new Promise((resolve, reject) => {
      try { device.open() } catch (e) {
        reject(new Error(`USB 장치 열기 실패: ${e.message}`)); return
      }

      // Step 1: verify AOA support (protocol version > 0)
      device.controlTransfer(0xC0, ACCESSORY_GET_PROTOCOL, 0, 0, 2, (err, data) => {
        if (err || !data) {
          try { device.close() } catch {}
          reject(new Error(`AOA 프로토콜 확인 실패: ${err?.message ?? '응답 없음'}`)); return
        }
        const version = data.readUInt16LE(0)
        if (version === 0) {
          try { device.close() } catch {}
          reject(new Error('이 장치는 AOA를 지원하지 않습니다')); return
        }

        // Step 2: send identification strings (index 0-5)
        const sendString = (idx) => {
          if (idx >= AOA_STRINGS.length) { startAccessory(); return }
          const buf = Buffer.from(AOA_STRINGS[idx] + '\0', 'utf8')
          device.controlTransfer(0x40, ACCESSORY_SEND_STRING, 0, idx, buf, (e) => {
            if (e) {
              try { device.close() } catch {}
              reject(new Error(`문자열[${idx}] 전송 실패: ${e.message}`)); return
            }
            sendString(idx + 1)
          })
        }

        // Step 3: trigger accessory mode switch
        const startAccessory = () => {
          device.controlTransfer(0x40, ACCESSORY_START, 0, 0, Buffer.alloc(0), (_e) => {
            try { device.close() } catch {}  // device will disconnect immediately
            this.emit('handshake-done')      // notify UI to show "waiting for reconnect"
            resolve()
          })
        }

        sendString(0)
      })
    })
  }

  // Opens an already-accessory-mode device and starts bulk IN/OUT
  _openAccessoryDevice(device) {
    try { device.open() } catch (e) {
      this.emit('error', new Error(`액세서리 장치 열기 실패: ${e.message}`)); return
    }

    const iface = device.interface(0)
    try { iface.claim() } catch (e) {
      try { device.close() } catch {}
      this.emit('error', new Error(`인터페이스 클레임 실패: ${e.message}`)); return
    }

    let inEndpoint = null
    let outEndpoint = null
    for (const ep of iface.endpoints) {
      if (ep.direction === 'in')  inEndpoint  = ep
      else                        outEndpoint = ep
    }

    if (!inEndpoint || !outEndpoint) {
      iface.release(true, () => { try { device.close() } catch {} })
      this.emit('error', new Error('Bulk 엔드포인트를 찾을 수 없습니다')); return
    }

    this._device      = device
    this._iface       = iface
    this._inEndpoint  = inEndpoint
    this._outEndpoint = outEndpoint

    // Continuous polling — feeds raw bytes into PacketParser
    inEndpoint.startPoll(4, 16384)
    inEndpoint.on('data', (data) => { this._parser.feed(data) })
    inEndpoint.on('error', (err) => {
      if (this._device) { this._cleanup(); this.emit('error', err) }
    })

    const pid = `0x${device.deviceDescriptor.idProduct.toString(16).toUpperCase().padStart(4, '0')}`
    this.emit('client-connected', `AOA (PID ${pid})`)
  }

  _cleanup() {
    this._parser.reset()
    const { _device: device, _iface: iface, _inEndpoint: inEp } = this
    this._device = this._iface = this._inEndpoint = this._outEndpoint = null

    try { inEp?.stopPoll() } catch {}
    try {
      iface?.release(true, () => { try { device?.close() } catch {} })
    } catch {
      try { device?.close() } catch {}
    }
  }

  stop() {
    if (this._onAttach) { usb.removeListener('attach', this._onAttach); this._onAttach = null }
    if (this._onDetach) { usb.removeListener('detach', this._onDetach); this._onDetach = null }

    if (this._device) {
      this._cleanup()
      this.emit('client-disconnected')
    }
  }

  send(buffer) {
    if (!this._outEndpoint) return false
    try {
      this._outEndpoint.transfer(buffer, (err) => {
        if (err && this._device) this.emit('error', err)
      })
      return true
    } catch { return false }
  }

  get isConnected() { return this._device !== null }
}

module.exports = { UsbReceiver }
