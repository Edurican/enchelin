<template>
  <div class="map-page">
    <LoadingSpinner v-if="locating" message="위치를 가져오는 중..." />
    <template v-else>
      <KakaoMap
        :center="center"
        :markers="restaurantStore.nearbyList"
        :level="4"
        @marker-click="onMarkerClick"
        @bounds-changed="onBoundsChanged"
      />
      <button
        v-if="movedFromOrigin"
        class="btn btn-primary search-here-btn"
        @click="searchHere"
      >
        이 위치에서 검색
      </button>
      <div v-if="restaurantStore.loading" class="map-loading">
        <LoadingSpinner message="식당을 찾는 중..." />
      </div>
      <ErrorMessage
        v-if="restaurantStore.error"
        class="map-error"
        :message="restaurantStore.error.message || '식당 정보를 불러올 수 없습니다.'"
        :retry="true"
        @retry="loadRestaurants(center.lng, center.lat)"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRestaurantStore } from '@/stores/restaurant'
import KakaoMap from '@/components/map/KakaoMap.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const router = useRouter()
const restaurantStore = useRestaurantStore()

const DEFAULT_CENTER = { lat: 37.4979, lng: 127.0276 } // 강남역
const center = ref({ ...DEFAULT_CENTER })
const locating = ref(true)
const movedFromOrigin = ref(false)
let lastSearchPos = { x: 0, y: 0 }
let currentMapCenter = { x: 0, y: 0 }

function loadRestaurants(x, y) {
  lastSearchPos = { x, y }
  movedFromOrigin.value = false
  restaurantStore.loadNearby(x, y)
}

function onMarkerClick(restaurant) {
  restaurantStore.selectRestaurant(restaurant)
  router.push(`/restaurants/${restaurant.id}`)
}

function onBoundsChanged({ x, y }) {
  currentMapCenter = { x, y }
  const dx = Math.abs(x - lastSearchPos.x)
  const dy = Math.abs(y - lastSearchPos.y)
  if (dx > 0.005 || dy > 0.005) {
    movedFromOrigin.value = true
  }
}

function searchHere() {
  center.value = { lat: currentMapCenter.y, lng: currentMapCenter.x }
  loadRestaurants(currentMapCenter.x, currentMapCenter.y)
}

onMounted(() => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        center.value = { lat: pos.coords.latitude, lng: pos.coords.longitude }
        locating.value = false
        loadRestaurants(pos.coords.longitude, pos.coords.latitude)
      },
      () => {
        center.value = { ...DEFAULT_CENTER }
        locating.value = false
        loadRestaurants(DEFAULT_CENTER.lng, DEFAULT_CENTER.lat)
      },
      { timeout: 5000 },
    )
  } else {
    locating.value = false
    loadRestaurants(DEFAULT_CENTER.lng, DEFAULT_CENTER.lat)
  }
})
</script>

<style scoped>
.map-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

.search-here-btn {
  position: absolute;
  top: var(--space-4);
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  box-shadow: var(--shadow-md);
  font-size: var(--font-size-sm);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-full);
}

.map-loading {
  position: absolute;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: var(--space-2) var(--space-4);
}

.map-error {
  position: absolute;
  bottom: var(--space-6);
  left: var(--space-4);
  right: var(--space-4);
  z-index: 10;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
}
</style>
