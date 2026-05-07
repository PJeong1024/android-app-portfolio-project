const { contextBridge, ipcRenderer } = require('electron')

contextBridge.exposeInMainWorld('electronAPI', {
  getConfig: () => ipcRenderer.invoke('get-config'),
  onNewLocation: (callback) => {
    ipcRenderer.on('new-location', (_event, data) => callback(data))
  }
})
