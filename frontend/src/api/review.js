import client from './client'

export function createReview(restaurantId, rating, comment) {
  return client.post('/reviews', { restaurantId, rating, comment })
}

export function fetchRestaurantReviews(restaurantId, offset = 0, limit = 10) {
  return client.get(`/restaurants/${restaurantId}/reviews`, {
    params: { offset, limit },
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
