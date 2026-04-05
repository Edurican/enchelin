import axios from 'axios'

const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body.success) {
      return body.data
    }
    return Promise.reject(body.error)
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
      return Promise.reject({ code: 'A001', message: '세션이 만료되었습니다.' })
    }
    const apiError = error.response?.data?.error
    return Promise.reject(apiError || { code: 'C002', message: '서버 오류입니다.' })
  },
)

export default client
