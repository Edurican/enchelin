<template>
  <div ref="mapContainer" class="kakao-map" />
</template>

<script setup>
import { ref, onMounted, watch, onBeforeUnmount } from 'vue'

const props = defineProps({
  center: { type: Object, required: true },
  markers: { type: Array, default: () => [] },
  level: { type: Number, default: 4 },
})

const emit = defineEmits(['marker-click', 'bounds-changed'])

const mapContainer = ref(null)
let map = null
let clusterer = null
let kakaoMarkers = []
let activeInfoWindow = null

function clearMarkers() {
  if (clusterer) {
    clusterer.clear()
  } else {
    kakaoMarkers.forEach((m) => m.setMap(null))
  }
  kakaoMarkers = []
  if (activeInfoWindow) {
    activeInfoWindow.close()
    activeInfoWindow = null
  }
}

function renderMarkers() {
  if (!map || !window.kakao) return
  clearMarkers()

  const { kakao } = window
  const newMarkers = props.markers.map((restaurant) => {
    const position = new kakao.maps.LatLng(restaurant.y, restaurant.x)
    const marker = new kakao.maps.Marker({ position })

    const infoContent = `
      <div style="padding:8px 12px;font-size:13px;font-family:var(--font-family);min-width:140px;">
        <strong style="display:block;margin-bottom:2px;">${restaurant.name}</strong>
        <span style="color:#6b7280;font-size:12px;">${restaurant.category ?? ''}</span>
      </div>
    `
    const infoWindow = new kakao.maps.InfoWindow({ content: infoContent })

    kakao.maps.event.addListener(marker, 'click', () => {
      if (activeInfoWindow) activeInfoWindow.close()
      infoWindow.open(map, marker)
      activeInfoWindow = infoWindow
      emit('marker-click', restaurant)
    })

    return marker
  })

  kakaoMarkers = newMarkers

  if (clusterer) {
    clusterer.addMarkers(newMarkers)
  } else {
    newMarkers.forEach((m) => m.setMap(map))
  }
}

function emitBoundsChanged() {
  const center = map.getCenter()
  const bounds = map.getBounds()
  const sw = bounds.getSouthWest()
  const ne = bounds.getNorthEast()
  emit('bounds-changed', {
    center: { x: center.getLng(), y: center.getLat() },
    sw: { lat: sw.getLat(), lng: sw.getLng() },
    ne: { lat: ne.getLat(), lng: ne.getLng() },
  })
}

function initMap() {
  const { kakao } = window
  kakao.maps.load(() => {
    const container = mapContainer.value
    const options = {
      center: new kakao.maps.LatLng(props.center.lat, props.center.lng),
      level: props.level,
    }
    map = new kakao.maps.Map(container, options)

    // MarkerClusterer (SDK에 clusterer 라이브러리 포함 시 활성화)
    if (kakao.maps.MarkerClusterer) {
      clusterer = new kakao.maps.MarkerClusterer({
        map,
        averageCenter: true,
        minLevel: 4,
      })
    }

    kakao.maps.event.addListener(map, 'idle', emitBoundsChanged)

    renderMarkers()
  })
}

function loadKakaoSDK() {
  return new Promise((resolve, reject) => {
    if (window.kakao && window.kakao.maps) {
      resolve()
      return
    }
    const script = document.createElement('script')
    const appkey = import.meta.env.VITE_KAKAO_JS_KEY
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${appkey}&libraries=clusterer&autoload=false`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('카카오맵 SDK 로딩 실패'))
    document.head.appendChild(script)
  })
}

onMounted(async () => {
  await loadKakaoSDK()
  initMap()
})

watch(() => props.markers, renderMarkers, { deep: true })

watch(
  () => props.center,
  (newCenter) => {
    if (map && window.kakao) {
      const { kakao } = window
      map.setCenter(new kakao.maps.LatLng(newCenter.lat, newCenter.lng))
    }
  },
)

onBeforeUnmount(() => {
  clearMarkers()
  if (clusterer) {
    clusterer.clear()
    clusterer = null
  }
})
</script>

<style scoped>
.kakao-map {
  width: 100%;
  height: 100%;
  flex: 1;
}
</style>
