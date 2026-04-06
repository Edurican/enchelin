<template>
  <div class="map-page">
    <LoadingSpinner v-if="locating" message="위치를 가져오는 중..." />
    <template v-else>
      <!-- 검색창 영역 -->
      <div class="map-search-overlay">
        <SearchBar
          ref="searchBarRef"
          v-model="searchQuery"
          @search="onSearch"
          @clear="onSearchClear"
        />
        <SearchResults
          :results="restaurantStore.searchResults"
          :loading="restaurantStore.searchLoading"
          :visible="showSearchResults"
          @select="onSearchResultSelect"
        />
      </div>

      <KakaoMap
        :center="center"
        :markers="restaurantStore.mapRestaurants"
        :level="4"
        @marker-click="onMarkerClick"
        @bounds-changed="onBoundsChanged"
      />

      <!-- 로딩 인디케이터 -->
      <div v-if="restaurantStore.loading" class="map-loading">
        <LoadingSpinner message="식당을 찾는 중..." />
      </div>

      <!-- 에러 -->
      <ErrorMessage
        v-if="restaurantStore.error"
        class="map-error"
        :message="restaurantStore.error.message || '식당 정보를 불러올 수 없습니다.'"
        :retry="true"
        @retry="retryLoad"
      />

      <!-- Empty state: 리뷰된 식당 없음 -->
      <div
        v-if="!restaurantStore.loading && !restaurantStore.error && restaurantStore.mapRestaurants.length === 0 && boundsLoaded"
        class="map-empty"
      >
        <p class="map-empty__text">
          아직 리뷰된 식당이 없어요.<br />
          상단 검색창에서 식당을 찾아 첫 리뷰를 남겨보세요.
        </p>
        <button class="btn btn-primary map-empty__btn" @click="focusSearch">
          식당 검색하기
        </button>
      </div>

      <!-- viewport 상한 경고 -->
      <div v-if="restaurantStore.hasMore" class="map-has-more">
        더 확대하면 더 많은 식당을 볼 수 있어요.
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRestaurantStore } from '@/stores/restaurant'
import KakaoMap from '@/components/map/KakaoMap.vue'
import SearchBar from '@/components/map/SearchBar.vue'
import SearchResults from '@/components/map/SearchResults.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const router = useRouter()
const restaurantStore = useRestaurantStore()

const DEFAULT_CENTER = { lat: 37.4979, lng: 127.0276 } // 강남역
const center = ref({ ...DEFAULT_CENTER })
const locating = ref(true)
const boundsLoaded = ref(false)
const searchQuery = ref('')
const showSearchResults = ref(false)
const searchBarRef = ref(null)

// 현재 bounds (bounds-changed 이벤트로 갱신)
let currentBounds = { sw: null, ne: null }
let currentCenter = { x: DEFAULT_CENTER.lng, y: DEFAULT_CENTER.lat }

// bounds 변경 시 debounce 로드
let boundsDebounceTimer = null

function scheduleBoundsLoad(sw, ne) {
  clearTimeout(boundsDebounceTimer)
  boundsDebounceTimer = setTimeout(() => {
    boundsLoaded.value = false
    restaurantStore.loadByBounds(sw, ne).finally(() => {
      boundsLoaded.value = true
    })
  }, 300)
}

function onBoundsChanged({ center: c, sw, ne }) {
  currentCenter = c
  currentBounds = { sw, ne }
  // 검색 결과가 열려있으면 bounds 재조회 하지 않음
  if (!showSearchResults.value) {
    scheduleBoundsLoad(sw, ne)
  }
}

function retryLoad() {
  if (currentBounds.sw && currentBounds.ne) {
    restaurantStore.loadByBounds(currentBounds.sw, currentBounds.ne)
  }
}

function onMarkerClick(restaurant) {
  restaurantStore.selectRestaurant(restaurant)
  router.push(`/restaurants/${restaurant.id}`)
}

// 검색
function onSearch(query) {
  showSearchResults.value = true
  restaurantStore.searchKakao(query, currentCenter)
}

function onSearchClear() {
  showSearchResults.value = false
  restaurantStore.clearSearch()
}

function onSearchResultSelect(item) {
  // 지도를 해당 식당 좌표로 이동
  center.value = { lat: Number(item.y), lng: Number(item.x) }
  showSearchResults.value = false
  searchQuery.value = item.name

  // 리뷰 작성 폼으로 이동 (kakaoApiId + 스냅샷 쿼리로 전달)
  router.push({
    name: 'ReviewCreate',
    query: {
      kakaoApiId: item.kakaoApiId,
      name: item.name,
      category: item.category ?? '',
      address: item.address ?? '',
      x: item.x,
      y: item.y,
      placeUrl: item.placeUrl ?? '',
    },
  })
}

function focusSearch() {
  searchBarRef.value?.focus()
}

onMounted(() => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        center.value = { lat: pos.coords.latitude, lng: pos.coords.longitude }
        locating.value = false
      },
      () => {
        center.value = { ...DEFAULT_CENTER }
        locating.value = false
      },
      { timeout: 5000 },
    )
  } else {
    locating.value = false
  }
  // bounds 조회는 KakaoMap의 idle 이벤트(onBoundsChanged)가 최초 발생 시 자동 트리거
})
</script>

<style scoped>
.map-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 검색창 오버레이 */
.map-search-overlay {
  position: absolute;
  top: var(--space-4);
  left: var(--space-4);
  right: var(--space-4);
  z-index: 10;
}

/* 로딩 */
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

/* 에러 */
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

/* Empty state */
.map-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: var(--space-5) var(--space-6);
  text-align: center;
  max-width: 320px;
  width: calc(100% - var(--space-8));
}

.map-empty__text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-4);
  line-height: 1.6;
}

.map-empty__btn {
  font-size: var(--font-size-sm);
}

/* 상한 경고 */
.map-has-more {
  position: absolute;
  bottom: var(--space-6);
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  background: var(--color-bg);
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-md);
  padding: var(--space-2) var(--space-4);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

/* 모바일: 검색창이 NavBar 위에 겹치지 않도록 */
@media (max-width: 640px) {
  .map-empty {
    top: 55%;
  }
}
</style>
