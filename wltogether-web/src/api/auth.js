import api from './index'

export default {
  register(data) {
    return api.post('/api/auth/register', data)
  },
  verify(data) {
    return api.post('/api/auth/verify', data)
  },
  resendCode(data) {
    return api.post('/api/auth/resend-code', data)
  },
  login(data) {
    return api.post('/api/auth/login', data)
  },
  refresh(data) {
    return api.post('/api/auth/refresh', data)
  },
  forgotPassword(data) {
    return api.post('/api/auth/forgot-password', data)
  },
  resetPassword(data) {
    return api.post('/api/auth/reset-password', data)
  }
}
