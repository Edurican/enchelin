import { defineStore } from 'pinia'
import { ref } from 'vue'
import { fetchNearbyRestaurants } from '@/api/restaurant'

export const useRestaurantStore = defineStore('restaurant', () => {
  const nearbyList = ref([])
  const selected = ref(null)
  const loading = ref(false)
  const error = ref(null)

  async function loadNearby(x, y, radius = 1000) {
    loading.value = true
    error.value = null
    try {
      nearbyList.value = await fetchNearbyRestaurants(x, y, radius)
    } catch (e) {
      error.value = e
      nearbyList.value = []
    } finally {
      loading.value = false
    }
  }

  function selectRestaurant(restaurant) {
    selected.value = restaurant
  }

  function clearSelected() {
    selected.value = null
  }

  return { nearbyList, selected, loading, error, loadNearby, selectRestaurant, clearSelected }
})
