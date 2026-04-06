import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useRestaurantStore } from '@/stores/restaurant'
import * as restaurantApi from '@/api/restaurant'

vi.mock('@/api/restaurant')

describe('useRestaurantStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  // ─── loadByBounds ─────────────────────────────────────────────
  describe('loadByBounds', () => {
    it('bounds 내 리뷰된 식당 목록을 mapRestaurants에 저장한다', async () => {
      // Arrange
      const fixtures = [
        { id: 1, name: '스타벅스 강남점', category: '카페', x: 127.027, y: 37.498 },
        { id: 2, name: '한식당', category: '한식', x: 127.028, y: 37.499 },
      ]
      restaurantApi.fetchRestaurantsInBounds.mockResolvedValue(fixtures)
      const store = useRestaurantStore()
      const sw = { lat: 37.4, lng: 127.0 }
      const ne = { lat: 37.6, lng: 127.1 }

      // Act
      await store.loadByBounds(sw, ne)

      // Assert
      expect(restaurantApi.fetchRestaurantsInBounds).toHaveBeenCalledWith(sw, ne)
      expect(store.mapRestaurants).toEqual(fixtures)
      expect(store.loading).toBe(false)
      expect(store.error).toBeNull()
    })

    it('API 응답이 { restaurants, hasMore } 형태여도 올바르게 파싱한다', async () => {
      // Arrange
      const fixtures = [{ id: 3, name: '맛집', category: '일식', x: 127.01, y: 37.5 }]
      restaurantApi.fetchRestaurantsInBounds.mockResolvedValue({
        restaurants: fixtures,
        hasMore: true,
      })
      const store = useRestaurantStore()

      // Act
      await store.loadByBounds({ lat: 37.4, lng: 127.0 }, { lat: 37.6, lng: 127.1 })

      // Assert
      expect(store.mapRestaurants).toEqual(fixtures)
      expect(store.hasMore).toBe(true)
    })

    it('API 호출 중 loading이 true가 된다', async () => {
      // Arrange
      let resolveApi
      restaurantApi.fetchRestaurantsInBounds.mockReturnValue(
        new Promise((r) => { resolveApi = r }),
      )
      const store = useRestaurantStore()

      // Act
      const promise = store.loadByBounds({ lat: 37.4, lng: 127.0 }, { lat: 37.6, lng: 127.1 })
      expect(store.loading).toBe(true)
      resolveApi([])
      await promise

      // Assert
      expect(store.loading).toBe(false)
    })

    it('API 실패 시 error를 저장하고 mapRestaurants를 빈 배열로 유지한다', async () => {
      // Arrange
      const apiError = { code: 'E001', message: '서버 오류' }
      restaurantApi.fetchRestaurantsInBounds.mockRejectedValue(apiError)
      const store = useRestaurantStore()

      // Act
      await store.loadByBounds({ lat: 37.4, lng: 127.0 }, { lat: 37.6, lng: 127.1 })

      // Assert
      expect(store.error).toEqual(apiError)
      expect(store.mapRestaurants).toEqual([])
    })
  })

  // ─── searchKakao ──────────────────────────────────────────────
  describe('searchKakao', () => {
    it('검색어와 center를 API에 전달하고 searchResults에 저장한다', async () => {
      // Arrange
      const fixtures = [
        { kakaoApiId: 'k1', name: '스타벅스 강남점', category: '카페', x: '127.027', y: '37.498', address: '서울', distance: '120' },
      ]
      restaurantApi.searchRestaurants.mockResolvedValue(fixtures)
      const store = useRestaurantStore()
      const center = { x: 127.027, y: 37.498 }

      // Act
      await store.searchKakao('스타벅스', center)

      // Assert
      expect(restaurantApi.searchRestaurants).toHaveBeenCalledWith('스타벅스', center)
      expect(store.searchResults).toEqual(fixtures)
      expect(store.searchLoading).toBe(false)
    })

    it('API 실패 시 searchResults를 빈 배열로 유지한다', async () => {
      // Arrange
      restaurantApi.searchRestaurants.mockRejectedValue({ message: '네트워크 오류' })
      const store = useRestaurantStore()

      // Act
      await store.searchKakao('스타벅스')

      // Assert
      expect(store.searchResults).toEqual([])
    })

    it('center 없이 호출해도 동작한다', async () => {
      // Arrange
      restaurantApi.searchRestaurants.mockResolvedValue([])
      const store = useRestaurantStore()

      // Act
      await store.searchKakao('한식당')

      // Assert
      expect(restaurantApi.searchRestaurants).toHaveBeenCalledWith('한식당', null)
    })
  })

  // ─── clearSearch ──────────────────────────────────────────────
  describe('clearSearch', () => {
    it('searchResults를 초기화한다', async () => {
      // Arrange
      restaurantApi.searchRestaurants.mockResolvedValue([{ kakaoApiId: 'k1' }])
      const store = useRestaurantStore()
      await store.searchKakao('test')
      expect(store.searchResults.length).toBe(1)

      // Act
      store.clearSearch()

      // Assert
      expect(store.searchResults).toEqual([])
    })
  })

  // ─── selectRestaurant / clearSelected ─────────────────────────
  describe('selectRestaurant / clearSelected', () => {
    it('식당을 선택하고 해제할 수 있다', () => {
      // Arrange
      const store = useRestaurantStore()
      const restaurant = { id: 1, name: '맛집' }

      // Act & Assert
      store.selectRestaurant(restaurant)
      expect(store.selected).toEqual(restaurant)

      store.clearSelected()
      expect(store.selected).toBeNull()
    })
  })
})
