require('dotenv').config({ path: '.env.local' })
const { app, BrowserWindow, ipcMain } = require('electron')
const os = require('os')
const path = require('path')
const { TcpReceiver } = require('./transport/TcpReceiver')
const { UsbReceiver } = require('./transport/UsbReceiver')
const { PacketParser, CMD } = require('./packet/PacketParser')
const { MarkerDatabase } = require('./db/MarkerDatabase')

let mainWindow = null
let db = null

const tcpReceiver = new TcpReceiver()
const usbReceiver = new UsbReceiver()

// --- Transport event wiring ---

function wireReceiver(receiver, type) {
  receiver.on('listening', (port) => {
    mainWindow?.webContents.send('transport-status', { type, event: 'listening', port })
  })

  receiver.on('client-connected', (address) => {
    mainWindow?.webContents.send('transport-status', { type, event: 'connected', address })
  })

  receiver.on('client-disconnected', () => {
    mainWindow?.webContents.send('transport-status', { type, event: 'disconnected' })
  })

  receiver.on('error', (err) => {
    mainWindow?.webContents.send('transport-status', { type, event: 'error', message: err.message })
  })

  // AOA-specific: Android disconnected and is switching to accessory mode
  receiver.on('handshake-done', () => {
    mainWindow?.webContents.send('transport-status', { type, event: 'handshake-done' })
  })

  receiver.on('packet', (cmd, payload) => {
    handlePacket(receiver, cmd, payload)
  })
}

function handlePacket(sourceReceiver, cmd, payload) {
  if (cmd === CMD.IMAGE_LIST) {
    const data = PacketParser.parseImageList(payload)
    if (!data?.items) return

    const newItems = db
      ? data.items.filter((item) => db.insertIfAbsent(item))
      : data.items  // DB 미초기화 시 전체를 신규로 간주

    mainWindow?.webContents.send('image-list', {
      items: data.items,
      newItems
    })

  } else if (cmd === CMD.THUMBNAIL_RESPONSE) {
    const data = PacketParser.parseThumbnail(payload)
    if (!data) return

    db?.saveThumbnailIfAbsent(data.imageID, data.thumbnailData)
    mainWindow?.webContents.send('thumbnail', data)
  }
}

wireReceiver(tcpReceiver, 'tcp')
wireReceiver(usbReceiver, 'usb')

// --- IPC handlers ---

ipcMain.handle('get-config', () => ({
  mapsApiKey: process.env.GOOGLE_MAPS_API_KEY
}))

ipcMain.handle('get-local-addresses', () => {
  const addresses = []
  const ifaces = os.networkInterfaces()
  for (const iface of Object.values(ifaces)) {
    for (const addr of iface) {
      if (addr.family === 'IPv4' && !addr.internal) addresses.push(addr.address)
    }
  }
  return addresses
})

ipcMain.handle('get-all-markers', () => {
  return db ? db.getAll() : []
})

ipcMain.handle('tcp-start', (_event, port) => {
  try {
    tcpReceiver.start(port)
    return { success: true }
  } catch (e) {
    return { success: false, error: e.message }
  }
})

ipcMain.handle('tcp-stop', () => {
  tcpReceiver.stop()
})

ipcMain.handle('usb-list-devices', () => {
  const { UsbReceiver } = require('./transport/UsbReceiver')
  return UsbReceiver.listDevices()
})

ipcMain.handle('usb-start', () => {
  try {
    usbReceiver.start()
    return { success: true }
  } catch (e) {
    return { success: false, error: e.message }
  }
})

ipcMain.handle('usb-stop', () => {
  usbReceiver.stop()
})

ipcMain.handle('request-images', (_event, imageIDs) => {
  const packet = PacketParser.buildImageRequest(imageIDs)
  const sent = tcpReceiver.send(packet) || usbReceiver.send(packet)
  return { success: sent }
})

// --- Window ---

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.loadFile('index.html')

  if (process.env.NODE_ENV === 'development') {
    mainWindow.webContents.openDevTools()
  }

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

app.whenReady().then(() => {
  try {
    db = new MarkerDatabase(app.getPath('userData'))
  } catch (e) {
    console.error('[DB] 초기화 실패 — 마커가 저장되지 않습니다:', e.message)
  }
  createWindow()
})

app.on('window-all-closed', () => {
  tcpReceiver.stop()
  usbReceiver.stop()
  db?.close()
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow()
})
