import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import authApi from '@/api/auth'
import usersApi from '@/api/users'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = computed(() => !!accessToken.value && !!user.value)

  function setTokens(access, refresh, userData) {
    accessToken.value = access
    refreshToken.value = refresh
    user.value = userData
    localStorage.setItem('accessToken', access)
    localStorage.setItem('refreshToken', refresh)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function clearAuth() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')
  }

  async function login(account, password) {
    const res = await authApi.login({ account, password })
    setTokens(res.accessToken, res.refreshToken, res.user)
    return res
  }

  async function register(email, username, password) {
    return await authApi.register({ email, username, password })
  }

  async function verify(email, code) {
    const res = await authApi.verify({ email, code })
    setTokens(res.accessToken, res.refreshToken, res.user)
    return res
  }

  async function refresh() {
    const res = await authApi.refresh({ refreshToken: refreshToken.value })
    setTokens(res.accessToken, res.refreshToken, res.user)
    return res
  }

  async function fetchMe() {
    const res = await usersApi.getMe()
    user.value = res
    localStorage.setItem('user', JSON.stringify(res))
    return res
  }

  function logout() {
    clearAuth()
    const router = useRouter()
    router.push('/login')
  }

  // Try to restore session from stored tokens on app start
  async function restoreSession() {
    if (!accessToken.value || !refreshToken.value) {
      return false
    }
    try {
      await fetchMe()
      return true
    } catch {
      // Token expired, try refresh
      try {
        await refresh()
        return true
      } catch {
        clearAuth()
        return false
      }
    }
  }

  return {
    accessToken,
    refreshToken,
    user,
    isAuthenticated,
    setTokens,
    clearAuth,
    login,
    register,
    verify,
    refresh,
    fetchMe,
    logout,
    restoreSession
  }
})
