'use strict'

let map = null
let clusterer = null

// imageID → { marker, displayName, thumbnailData }
const markerStore = new Map()

// imageID → { infoWindow, marker } | 'modal' — thumbnail 응답 대기 중
const pendingInfoWindows = new Map()

// --- Init ---

async function init() {
  const config = await window.electronAPI.getConfig()
  if (!config.mapsApiKey) {
    console.error('[renderer] GOOGLE_MAPS_API_KEY missing in .env.local')
    return
  }

  await loadMapsScript(config.mapsApiKey)

  map = new google.maps.Map(document.getElementById('map'), {
    center: { lat: 37.5665, lng: 126.9780 },
    zoom: 10
  })

  clusterer = new markerClusterer.MarkerClusterer({
    map,
    markers: [],
    onClusterClick: onClusterClick
  })

  await loadMarkersFromDB()
  setupSettingsPanel()
  setupTransportEvents()
  setupModalClose()
}

function loadMapsScript(apiKey) {
  return new Promise((resolve, reject) => {
    window.__mapsReady = resolve
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async&callback=__mapsReady`
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// --- DB startup load ---

async function loadMarkersFromDB() {
  const rows = await window.electronAPI.getAllMarkers()
  if (!rows.length) return

  for (const row of rows) {
    addMarkerToMap(row)
    if (row.thumbnailData) {
      const entry = markerStore.get(row.imageID)
      if (entry) entry.thumbnailData = row.thumbnailData
    }
  }

  fitMapToBounds(rows)
}

// --- Settings panel ---

async function setupSettingsPanel() {
  // Collapse toggle
  document.getElementById('toggle-btn').addEventListener('click', () => {
    const panelBody = document.getElementById('panel-body')
    const collapsed = panelBody.classList.toggle('collapsed')
    document.getElementById('toggle-btn').textContent = collapsed ? '펼치기' : '접기'
  })

  // Mode toggle
  let currentMode = 'tcp'
  document.querySelectorAll('.mode-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      if (btn.dataset.mode === currentMode) return
      currentMode = btn.dataset.mode
      document.querySelectorAll('.mode-btn').forEach((b) => b.classList.remove('active'))
      btn.classList.add('active')
      document.getElementById('tcp-section').style.display = currentMode === 'tcp' ? '' : 'none'
      document.getElementById('usb-section').style.display = currentMode === 'usb' ? '' : 'none'
      setStatus('대기 중', '')
    })
  })

  await setupTcpSection()
  await setupUsbSection()

  // Shared transport status listener
  window.electronAPI.onTransportStatus((status) => {
    if (status.type === 'tcp') handleTcpStatus(status)
    else if (status.type === 'usb') handleUsbStatus(status)
  })
}

// --- TCP section ---

async function setupTcpSection() {
  const addresses = await window.electronAPI.getLocalAddresses()
  document.getElementById('local-addresses').textContent =
    addresses.length > 0 ? addresses.join(', ') : '인식된 IP 없음'

  let serverRunning = false
  const serverBtn = document.getElementById('server-btn')

  serverBtn.addEventListener('click', async () => {
    if (!serverRunning) {
      const port = parseInt(document.getElementById('port-input').value, 10)
      const result = await window.electronAPI.tcpStart(port)
      if (!result.success) setStatus(`오류: ${result.error}`, 'error')
    } else {
      await window.electronAPI.tcpStop()
      serverRunning = false
      serverBtn.textContent = '서버 시작'
      serverBtn.classList.remove('stop')
      setStatus('대기 중', '')
    }
  })

  window._tcpSetServerRunning = (val) => { serverRunning = val }
}

function handleTcpStatus(status) {
  const serverBtn = document.getElementById('server-btn')
  if (status.event === 'listening') {
    window._tcpSetServerRunning(true)
    serverBtn.textContent = '서버 중지'
    serverBtn.classList.add('stop')
    setStatus(`대기 중 — 포트 ${status.port}\nAndroid에서 위 IP:${status.port} 로 연결하세요`, 'listening')
  } else if (status.event === 'connected') {
    setStatus(`연결됨 — ${status.address}`, 'connected')
  } else if (status.event === 'disconnected') {
    setStatus(`클라이언트 연결 해제\n서버 대기 중...`, 'listening')
  } else if (status.event === 'error') {
    window._tcpSetServerRunning(false)
    serverBtn.textContent = '서버 시작'
    serverBtn.classList.remove('stop')
    setStatus(`오류: ${status.message}`, 'error')
  }
}

// --- USB section ---

async function setupUsbSection() {
  const connectBtn = document.getElementById('usb-connect-btn')
  let usbConnected = false

  await refreshUsbDevices()

  document.getElementById('usb-refresh-btn').addEventListener('click', refreshUsbDevices)

  connectBtn.addEventListener('click', async () => {
    if (!usbConnected) {
      const result = await window.electronAPI.usbStart()
      if (!result.success) setStatus(`USB 오류: ${result.error}`, 'error')
    } else {
      await window.electronAPI.usbStop()
      usbConnected = false
      connectBtn.textContent = '연결'
      connectBtn.classList.remove('stop')
      setStatus('대기 중', '')
    }
  })

  window._usbSetConnected = (val) => { usbConnected = val }
}

async function refreshUsbDevices() {
  const infoEl = document.getElementById('usb-device-info')
  infoEl.textContent = '스캔 중...'

  const devices = await window.electronAPI.usbListDevices()

  if (devices.length === 0) {
    infoEl.textContent = '감지된 Android 장치 없음'
    infoEl.style.color = '#aaa'
    return
  }

  infoEl.style.color = '#1a73e8'
  infoEl.textContent = devices.map((d) => {
    const vid = `0x${d.vendorId.toString(16).toUpperCase().padStart(4, '0')}`
    const pid = `0x${d.productId.toString(16).toUpperCase().padStart(4, '0')}`
    return d.isAccessory ? `AOA 모드 (${pid})` : `Android 장치 (VID ${vid})`
  }).join('\n')
}

function handleUsbStatus(status) {
  const connectBtn = document.getElementById('usb-connect-btn')

  if (status.event === 'handshake-done') {
    // AOA handshake sent — waiting for Android to reconnect in accessory mode
    setStatus('AOA 핸드셰이크 완료\nAndroid 재연결 대기 중...', 'listening')

  } else if (status.event === 'connected') {
    window._usbSetConnected(true)
    connectBtn.textContent = '연결 해제'
    connectBtn.classList.add('stop')
    setStatus(`USB AOA 연결됨\n${status.address}`, 'connected')

  } else if (status.event === 'disconnected') {
    window._usbSetConnected(false)
    connectBtn.textContent = '연결'
    connectBtn.classList.remove('stop')
    setStatus('USB 연결 해제', '')

  } else if (status.event === 'error') {
    window._usbSetConnected(false)
    connectBtn.textContent = '연결'
    connectBtn.classList.remove('stop')
    setStatus(`USB 오류: ${status.message}`, 'error')
  }
}

function setStatus(text, cssClass) {
  const el = document.getElementById('status-display')
  el.textContent = text
  el.className = cssClass
}

// --- Transport events ---

function setupTransportEvents() {
  window.electronAPI.onImageList(({ items, newItems }) => {
    for (const item of items) addMarkerToMap(item)
    if (newItems.length > 0) fitMapToBounds(newItems)
  })

  window.electronAPI.onThumbnail((data) => {
    if (!data?.imageID || !data?.thumbnailData) return

    const entry = markerStore.get(data.imageID)
    if (entry) entry.thumbnailData = data.thumbnailData

    // 대기 중인 InfoWindow 또는 모달에 즉시 표시
    const pending = pendingInfoWindows.get(data.imageID)
    if (pending === 'modal') {
      updateModalImage(data.thumbnailData)
      pendingInfoWindows.delete(data.imageID)
    } else if (pending) {
      showThumbnailInInfoWindow(pending.infoWindow, pending.marker, data.imageID, data.thumbnailData)
      pendingInfoWindows.delete(data.imageID)
    }

    // 클러스터 패널의 썸네일도 갱신
    updateClusterPanelThumbnail(data.imageID, data.thumbnailData)
  })
}

// --- Marker management ---

function addMarkerToMap(item) {
  if (item.imageLat == null || item.imageLong == null) return
  if (markerStore.has(item.imageID)) return

  const marker = new google.maps.Marker({
    position: { lat: item.imageLat, lng: item.imageLong },
    title: item.imageDisplayName
    // map은 clusterer가 관리
  })

  // imageID를 마커에 저장 (클러스터 클릭 시 역참조용)
  marker._imageID = item.imageID

  markerStore.set(item.imageID, {
    marker,
    displayName: item.imageDisplayName,
    thumbnailData: item.thumbnailData ?? null
  })

  marker.addListener('click', () => onMarkerClick(item.imageID))
  clusterer.addMarker(marker)
}

// --- Map bounds ---

function fitMapToBounds(items) {
  const valid = items.filter((i) => i.imageLat != null && i.imageLong != null)
  if (!valid.length) return

  if (valid.length === 1) {
    map.panTo({ lat: valid[0].imageLat, lng: valid[0].imageLong })
    if (map.getZoom() < 13) map.setZoom(13)
    return
  }

  const bounds = new google.maps.LatLngBounds()
  for (const item of valid) bounds.extend({ lat: item.imageLat, lng: item.imageLong })
  map.fitBounds(bounds, 60)

  google.maps.event.addListenerOnce(map, 'bounds_changed', () => {
    if (map.getZoom() > 15) map.setZoom(15)
  })
}

// --- Single marker click ---

function onMarkerClick(imageID) {
  const entry = markerStore.get(imageID)
  if (!entry) return

  const infoWindow = new google.maps.InfoWindow()
  infoWindow.open(map, entry.marker)

  if (entry.thumbnailData) {
    showThumbnailInInfoWindow(infoWindow, entry.marker, imageID, entry.thumbnailData)
  } else {
    infoWindow.setContent(
      `<div style="font-size:13px;padding:4px 0">${entry.displayName}<br><em style="color:#888">이미지 요청 중...</em></div>`
    )
    pendingInfoWindows.set(imageID, { infoWindow, marker: entry.marker })
    window.electronAPI.requestImages([imageID])
  }
}

function showThumbnailInInfoWindow(infoWindow, marker, imageID, base64Data) {
  const name = markerStore.get(imageID)?.displayName ?? ''
  infoWindow.setContent(`
    <div style="font-size:13px;max-width:220px">
      <div style="font-weight:600;margin-bottom:6px">${name}</div>
      <img src="data:image/jpeg;base64,${base64Data}"
           style="max-width:200px;border-radius:6px;display:block">
    </div>
  `)
  infoWindow.open(map, marker)
}

// --- Cluster click → 이미지 목록 패널 ---

function onClusterClick(_event, cluster, _map) {
  const items = cluster.markers.map((m) => ({
    imageID: m._imageID,
    displayName: m.getTitle()
  }))

  showClusterPanel(items)
}

function showClusterPanel(items) {
  const list = document.getElementById('cluster-list')
  document.getElementById('cluster-count').textContent = items.length
  list.innerHTML = ''

  for (const item of items) {
    const entry = markerStore.get(item.imageID)
    const el = document.createElement('div')
    el.className = 'cluster-item'
    el.dataset.imageId = item.imageID

    if (entry?.thumbnailData) {
      el.innerHTML = `
        <img class="thumb" src="data:image/jpeg;base64,${entry.thumbnailData}">
        <span class="cluster-item-name">${item.displayName}</span>
      `
    } else {
      el.innerHTML = `
        <div class="thumb-placeholder">🖼</div>
        <span class="cluster-item-name">${item.displayName}</span>
      `
    }

    el.addEventListener('click', () => onClusterItemClick(item.imageID, item.displayName))
    list.appendChild(el)
  }

  document.getElementById('cluster-panel').classList.add('visible')
}

function onClusterItemClick(imageID, displayName) {
  const entry = markerStore.get(imageID)
  if (!entry) return

  showModal(displayName)

  if (entry.thumbnailData) {
    updateModalImage(entry.thumbnailData)
  } else {
    pendingInfoWindows.set(imageID, 'modal')
    window.electronAPI.requestImages([imageID])
  }
}

function updateClusterPanelThumbnail(imageID, base64Data) {
  const item = document.querySelector(`#cluster-list .cluster-item[data-image-id="${imageID}"]`)
  if (!item) return

  item.innerHTML = `
    <img class="thumb" src="data:image/jpeg;base64,${base64Data}">
    <span class="cluster-item-name">${markerStore.get(imageID)?.displayName ?? ''}</span>
  `
}

document.getElementById('cluster-panel-close').addEventListener('click', () => {
  document.getElementById('cluster-panel').classList.remove('visible')
})

// --- Image modal ---

function showModal(title) {
  document.getElementById('modal-title').textContent = title
  document.getElementById('modal-loading').style.display = 'block'
  document.getElementById('modal-img').style.display = 'none'
  document.getElementById('image-modal').classList.add('visible')
}

function updateModalImage(base64Data) {
  const img = document.getElementById('modal-img')
  img.src = `data:image/jpeg;base64,${base64Data}`
  img.style.display = 'block'
  document.getElementById('modal-loading').style.display = 'none'
}

function setupModalClose() {
  document.getElementById('modal-close').addEventListener('click', () => {
    document.getElementById('image-modal').classList.remove('visible')
  })
  document.getElementById('image-modal').addEventListener('click', (e) => {
    if (e.target === document.getElementById('image-modal')) {
      document.getElementById('image-modal').classList.remove('visible')
    }
  })
}

init().catch((e) => console.error('[renderer] init 실패:', e))
