'use strict'
const { EventEmitter } = require('events')

/**
 * Abstract base class for transport receivers.
 *
 * Events:
 *   'packet'(cmd, payload)      — complete, checksum-verified packet received
 *   'client-connected'(detail)  — a client has connected
 *   'client-disconnected'       — client disconnected
 *   'error'(err)                — transport error
 *   'listening'(port)           — server is listening (TCP only)
 *
 * Subclasses must implement: start(), stop(), send()
 */
class DataReceiver extends EventEmitter {
  start(_options) { throw new Error('start() not implemented') }
  stop() { throw new Error('stop() not implemented') }
  send(_buffer) { throw new Error('send() not implemented') }
}

module.exports = { DataReceiver }
