'use strict'
const net = require('net')
const { DataReceiver } = require('./DataReceiver')
const { PacketParser } = require('../packet/PacketParser')

/**
 * TCP server receiver — Electron listens, Android connects as client.
 *
 * Only one client connection is accepted at a time.
 * Feeds incoming bytes into PacketParser and re-emits 'packet' events.
 */
class TcpReceiver extends DataReceiver {
  constructor() {
    super()
    this._server = null
    this._socket = null
    this._parser = new PacketParser()

    this._parser.on('packet', (cmd, payload) => {
      this.emit('packet', cmd, payload)
    })
  }

  start(port = 8080) {
    if (this._server) return

    this._parser.reset()

    this._server = net.createServer((socket) => {
      // Reject additional connections while one is active
      if (this._socket) {
        socket.destroy()
        return
      }

      this._socket = socket
      this.emit('client-connected', socket.remoteAddress)

      socket.on('data', (data) => {
        this._parser.feed(data)
      })

      socket.on('end', () => {
        this._socket = null
        this._parser.reset()
        this.emit('client-disconnected')
      })

      socket.on('error', (err) => {
        this._socket = null
        this._parser.reset()
        this.emit('error', err)
      })
    })

    this._server.on('error', (err) => this.emit('error', err))

    this._server.listen(port, '0.0.0.0', () => {
      this.emit('listening', port)
    })
  }

  stop() {
    this._socket?.destroy()
    this._socket = null
    this._server?.close()
    this._server = null
    this._parser.reset()
  }

  send(buffer) {
    if (!this._socket) return false
    try {
      this._socket.write(buffer)
      return true
    } catch { return false }
  }

  get isListening() { return this._server?.listening ?? false }
  get isClientConnected() { return this._socket !== null }
}

module.exports = { TcpReceiver }
