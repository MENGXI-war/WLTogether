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
  },

  // Avatar
  uploadAvatar(file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAvatar() {
    return api.delete('/api/users/me/avatar')
  },

  changePassword(data) {
    return api.put('/api/users/me/password', data)
  },

  // User lookup
  getByUid(uid) {
    return api.get(`/api/users/by-uid/${uid}`)
  }
}
