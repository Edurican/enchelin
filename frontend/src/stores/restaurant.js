import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchRestaurantsInBounds, searchRestaurants } from '@/api/restaurant'

export const useRestaurantStore = defineStore('restaurant', () => {
  // 지도에 표시되는 리뷰된 DB 식당 목록
  const mapRestaurants = ref([])
  // 카카오 검색 결과 목록
  const searchResults = ref([])
  // 선택된 식당 (마커 클릭 등)
  const selected = ref(null)
  // 마지막 지도 center (뒤로가기 시 위치 복원용)
  const mapCenter = ref(null)
  const loading = ref(false)
  const searchLoading = ref(false)
  const error = ref(null)
  const hasMore = ref(false)

  /**
   * bounds 내 리뷰된 식당 조회
   * @param {{ lat: number, lng: number }} sw
   * @param {{ lat: number, lng: number }} ne
   */
  async function loadByBounds(sw, ne) {
    loading.value = true
    error.value = null
    try {
      const result = await fetchRestaurantsInBounds(sw, ne)
      mapRestaurants.value = Array.isArray(result) ? result : (result.restaurants ?? [])
      hasMore.value = result.hasMore ?? false
    } catch (e) {
      error.value = e
      mapRestaurants.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * 카카오 키워드 검색 (백엔드 프록시)
   * @param {string} query
   * @param {{ x: number, y: number } | null} center
   */
  async function searchKakao(query, center = null) {
    searchLoading.value = true
    try {
      const result = await searchRestaurants(query, center)
      searchResults.value = Array.isArray(result) ? result : (result.results ?? [])
    } catch (e) {
      searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }

  function clearSearch() {
    searchResults.value = []
  }

  function selectRestaurant(restaurant) {
    selected.value = restaurant
  }

  function clearSelected() {
    selected.value = null
  }

  function saveCenter(center) {
    mapCenter.value = { ...center }
  }

  return {
    mapRestaurants,
    searchResults,
    selected,
    mapCenter,
    loading,
    searchLoading,
    error,
    hasMore,
    loadByBounds,
    searchKakao,
    clearSearch,
    selectRestaurant,
    clearSelected,
    saveCenter,
  }
})
