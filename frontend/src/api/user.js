import client from './client'

export function fetchUserProfile(userId) {
  return client.get(`/users/${userId}`)
}
