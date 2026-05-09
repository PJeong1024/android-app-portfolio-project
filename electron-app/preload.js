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

  // Send image request to Android
  requestImages: (imageIDs) => ipcRenderer.invoke('request-images', imageIDs),

  // Events from main process
  onTransportStatus: (cb) => ipcRenderer.on('transport-status', (_e, d) => cb(d)),
  onImageList: (cb) => ipcRenderer.on('image-list', (_e, d) => cb(d)),
  onThumbnail: (cb) => ipcRenderer.on('thumbnail', (_e, d) => cb(d)),
})
