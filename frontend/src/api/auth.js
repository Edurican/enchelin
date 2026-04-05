import client from './client'

const API_BASE = import.meta.env.VITE_API_BASE || ''

export function getGithubLoginUrl() {
  return `${API_BASE}/api/auth/github`
}

export function exchangeToken(code) {
  return client.post('/api/auth/github/token', { code })
}
