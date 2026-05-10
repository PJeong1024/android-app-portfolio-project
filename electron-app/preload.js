const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
  // Config
  getConfig: () => ipcRenderer.invoke('get-config'),
  getLocalAddresses: () => ipcRenderer.invoke('get-local-addresses'),

  // DB
  getAllMarkers: () => ipcRenderer.invoke('get-all-markers'),

  // TCP server control
  tcpStart: (port) => ipcRenderer.invoke('tcp-start', port),
  tcpStop: () => ipcRenderer.invoke('tcp-stop'),

  // USB (AOA) control
  usbListDevices: () => ipcRenderer.invoke('usb-list-devices'),
  usbStart: () => ipcRenderer.invoke('usb-start'),
  usbStop: () => ipcRenderer.invoke('usb-stop'),

  // Send image request to Android
  requestImages: (imageIDs) => ipcRenderer.invoke('request-images', imageIDs),

  // Events from main process
  onTransportStatus: (cb) => ipcRenderer.on('transport-status', (_e, d) => cb(d)),
  onImageList: (cb) => ipcRenderer.on('image-list', (_e, d) => cb(d)),
  onThumbnail: (cb) => ipcRenderer.on('thumbnail', (_e, d) => cb(d)),
  onRawImage: (cb) => ipcRenderer.on('raw-image', (_e, d) => cb(d)),
})
