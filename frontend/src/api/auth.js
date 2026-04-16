import client from './client'

export function getGithubLoginUrl() {
  return '/api/auth/github'
}

export function exchangeToken(code) {
  return client.post('/api/auth/exchange', { code })
}
