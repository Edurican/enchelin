import client from './client'

/**
 * 리뷰 생성 — kakaoApiId 경유 식당 upsert 포함
 * @param {Object} payload
 * @param {string} payload.kakaoApiId
 * @param {string} payload.name
 * @param {string} payload.category
 * @param {string} payload.address
 * @param {string} payload.placeUrl
 * @param {number} payload.x
 * @param {number} payload.y
 * @param {number} payload.rating
 * @param {string} payload.comment
 */
export function createReview({ kakaoApiId, name, category, address, placeUrl, x, y, rating, comment }) {
  return client.post('/reviews', { kakaoApiId, name, category, address, placeUrl, x, y, rating, comment })
}

/**
 * 식당 리뷰 목록 조회
 * @param {number|string} restaurantId
 * @param {number} offset
 * @param {number} limit
 * @param {'latest'|'rating_desc'|'rating_asc'} sort
 */
export function fetchRestaurantReviews(restaurantId, offset = 0, limit = 10, sort = 'latest') {
  return client.get(`/restaurants/${restaurantId}/reviews`, {
    params: { offset, limit, sort },
  })
}

export function fetchUserReviews(userId, offset = 0, limit = 10) {
  return client.get(`/users/${userId}/reviews`, {
    params: { offset, limit },
  })
}

export function updateReview(reviewId, rating, comment) {
  return client.put(`/reviews/${reviewId}`, { rating, comment })
}

export function deleteReview(reviewId) {
  return client.delete(`/reviews/${reviewId}`)
}
