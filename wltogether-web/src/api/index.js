import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Token refresh state (singleton lock to prevent concurrent refreshes)
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  failedQueue = []
}

// Request interceptor: attach Authorization header
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: handle 401 refresh and 429 retry
api.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config
    const authStore = useAuthStore()

    // Handle 429 Rate Limit
    if (error.response?.status === 429) {
      const retryAfter = error.response.data?.retryAfter || 5
      ElMessage.warning(`请求过于频繁，${retryAfter}秒后重试`)
      await new Promise(resolve => setTimeout(resolve, retryAfter * 1000))
      return api(originalRequest)
    }

    // Handle 401 Unauthorized — try token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (!authStore.refreshToken) {
        authStore.logout()
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // Queue this request until refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          originalRequest._retry = true
          return api(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const response = await axios.post('/api/auth/refresh', {
          refreshToken: authStore.refreshToken
        })
        const { accessToken, refreshToken, user } = response.data
        authStore.setTokens(accessToken, refreshToken, user)
        processQueue(null, accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        authStore.logout()
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Pass through other errors
    return Promise.reject(error)
  }
)

export default api
