import client from './client'

export function fetchNearbyRestaurants(x, y, radius = 1000) {
  return client.get('/restaurant/nearby', { params: { x, y, radius } })
}

export function fetchRestaurantDetail(restaurantId) {
  return client.get(`/restaurants/${restaurantId}`)
}
