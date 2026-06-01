import api from './index'

export default {
  getMe() {
    return api.get('/api/users/me')
  },
  updateMe(data) {
    return api.put('/api/users/me', data)
  },
  getPublicKey(userId) {
    return api.get(`/api/users/${userId}/public-key`)
  }
}
