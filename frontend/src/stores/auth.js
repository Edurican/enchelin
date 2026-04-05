import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(null)

  const isAuthenticated = computed(() => !!token.value)

  function decodePayload(jwt) {
    try {
      const base64 = jwt.split('.')[1]
      return JSON.parse(atob(base64))
    } catch {
      return null
    }
  }

  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    const payload = decodePayload(newToken)
    if (payload) {
      user.value = {
        id: payload.sub,
        nickname: payload.nickname || null,
        avatarUrl: payload.avatarUrl || null,
        htmlUrl: payload.htmlUrl || null,
      }
    }
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('token')
  }

  function isTokenValid() {
    if (!token.value) return false
    const payload = decodePayload(token.value)
    if (!payload?.exp) return false
    return payload.exp * 1000 > Date.now()
  }

  // 초기화: 저장된 토큰이 있으면 유저 정보 복원
  if (token.value) {
    if (isTokenValid()) {
      const payload = decodePayload(token.value)
      if (payload) {
        user.value = {
          id: payload.sub,
          nickname: payload.nickname || null,
          avatarUrl: payload.avatarUrl || null,
          htmlUrl: payload.htmlUrl || null,
        }
      }
    } else {
      logout()
    }
  }

  return { token, user, isAuthenticated, setToken, logout, isTokenValid }
})
