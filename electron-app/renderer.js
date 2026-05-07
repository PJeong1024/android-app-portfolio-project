let map

async function init() {
  const config = await window.electronAPI.getConfig()
  console.log('[init] config:', config)
  const { mapsApiKey } = config

  if (!mapsApiKey) {
    console.error('[init] GOOGLE_MAPS_API_KEY가 .env.local에 없습니다.')
    return
  }

  await loadMapsScript(mapsApiKey)
  console.log('[init] Maps 스크립트 로드 완료')

  map = new google.maps.Map(document.getElementById('map'), {
    center: { lat: 37.5665, lng: 126.9780 },
    zoom: 12
  })

  window.electronAPI.onNewLocation((data) => {
    addMarker(data)
  })
}

function loadMapsScript(apiKey) {
  return new Promise((resolve, reject) => {
    window.__mapsReady = resolve
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&loading=async&callback=__mapsReady`
    script.onerror = (e) => {
      console.error('[loadMapsScript] 스크립트 로드 실패:', e)
      reject(e)
    }
    document.head.appendChild(script)
  })
}

function addMarker({ lat, lng, imageData }) {
  const marker = new google.maps.Marker({
    position: { lat, lng },
    map
  })

  if (imageData) {
    const infoWindow = new google.maps.InfoWindow({
      content: `<img src="data:image/jpeg;base64,${imageData}" style="max-width:200px;max-height:200px;">`
    })
    marker.addListener('click', () => infoWindow.open(map, marker))
  }
}

init().catch((e) => console.error('[init] 실패:', e))
