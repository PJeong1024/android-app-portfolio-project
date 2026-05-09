'use strict'
const { DataReceiver } = require('./DataReceiver')

/**
 * USB CDC receiver — requires `serialport` package (npm install serialport).
 * Auto-detects /dev/tty.usbmodem* on macOS.
 *
 * Not yet implemented — emits 'error' gracefully so the rest of the app works
 * while only TCP is connected.
 */
class UsbReceiver extends DataReceiver {
  constructor() {
    super()
    this._port = null
    this._parser = null
  }

  start() {
    // TODO: implement with serialport
    // 1. SerialPort.list() → filter /dev/tty.usbmodem*
    // 2. Open port with PacketParser piped in
    // 3. Emit 'client-connected' / 'packet' / 'client-disconnected'
    process.nextTick(() => {
      this.emit('error', new Error('USB receiver not yet implemented — serialport package required'))
    })
  }

  stop() {
    this._port?.close()
    this._port = null
  }

  send(_buffer) {
    if (!this._port) return false
    // TODO: this._port.write(_buffer)
    return false
  }

  get isConnected() { return this._port !== null }
}

module.exports = { UsbReceiver }
