import client from './client'

/**
 * 지도 viewport(bounds) 내 리뷰된 식당 목록 조회
 * @param {{ lat: number, lng: number }} sw - 남서 좌표
 * @param {{ lat: number, lng: number }} ne - 북동 좌표
 */
export function fetchRestaurantsInBounds(sw, ne) {
  return client.get('/restaurants', {
    params: {
      swLng: sw.lng,
      swLat: sw.lat,
      neLng: ne.lng,
      neLat: ne.lat,
    },
  })
}

/**
 * 카카오 키워드 검색 프록시 (백엔드 경유)
 * @param {string} query - 검색어
 * @param {{ x: number, y: number } | null} center - 지도 중심 좌표 (거리순 정렬용)
 */
export function searchRestaurants(query, center = null) {
  const params = { query }
  if (center) {
    params.x = center.x
    params.y = center.y
  }
  return client.get('/restaurants/search', { params })
}

/**
 * 식당 단건 조회
 * @param {number} restaurantId
 */
export function fetchRestaurantDetail(restaurantId) {
  return client.get(`/restaurants/${restaurantId}`)
}

/**
 * 식당 AI 리뷰 요약 조회
 * @param {number} restaurantId
 */
export function fetchReviewSummary(restaurantId) {
  return client.get(`/restaurants/${restaurantId}/review-summary`)
}
