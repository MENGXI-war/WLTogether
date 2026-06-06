import api from './index'

export default {
  // ========== Dashboard Stats ==========
  getStats() {
    return api.get('/api/admin/stats')
  },

  // ========== Users ==========
  getUsers(params = {}) {
    return api.get('/api/admin/users', { params })
  },
  updateUserStatus(id, status) {
    return api.put(`/api/admin/users/${id}/status`, { status })
  },
  deleteUser(id) {
    return api.delete(`/api/admin/users/${id}`)
  },

  // ========== Groups ==========
  getGroups(params = {}) {
    return api.get('/api/admin/groups', { params })
  },
  deleteGroup(id) {
    return api.delete(`/api/admin/groups/${id}`)
  },

  // ========== Sessions ==========
  getSessions(params = {}) {
    return api.get('/api/admin/sessions', { params })
  },
  forceEndSession(id) {
    return api.delete(`/api/admin/sessions/${id}`)
  },

  // ========== Messages ==========
  getMessages(params = {}) {
    return api.get('/api/admin/messages', { params })
  },
  deleteMessage(id) {
    return api.delete(`/api/admin/messages/${id}`)
  },

  // ========== Files ==========
  getFiles(params = {}) {
    return api.get('/api/admin/files', { params })
  },
  deleteFile(id) {
    return api.delete(`/api/admin/files/${id}`)
  },

  // ========== Announcements ==========
  getAnnouncements(params = {}) {
    return api.get('/api/admin/announcements', { params })
  },
  createAnnouncement(data) {
    return api.post('/api/admin/announcements', data)
  },
  updateAnnouncement(id, data) {
    return api.put(`/api/admin/announcements/${id}`, data)
  },
  deleteAnnouncement(id) {
    return api.delete(`/api/admin/announcements/${id}`)
  },

  // ========== Wallpapers ==========
  getWallpapers() {
    return api.get('/api/admin/wallpapers')
  },
  createWallpaper(data) {
    return api.post('/api/admin/wallpapers', data)
  },
  updateWallpaper(id, data) {
    return api.put(`/api/admin/wallpapers/${id}`, data)
  },
  deleteWallpaper(id) {
    return api.delete(`/api/admin/wallpapers/${id}`)
  },

  // ========== Error Logs ==========
  getErrors(params = {}) {
    return api.get('/api/admin/errors', { params })
  },
  updateErrorStatus(id, data) {
    return api.put(`/api/admin/errors/${id}`, data)
  },

  // ========== Settings ==========
  getSettings() {
    return api.get('/api/admin/settings')
  },
  getSetting(key) {
    return api.get(`/api/admin/settings/${key}`)
  },
  updateSetting(key, value) {
    return api.put(`/api/admin/settings/${key}`, { value })
  },

  // ========== Logs ==========
  getLogFiles() {
    return api.get('/api/admin/logs')
  },
  readLogFile(filename, lines = 200) {
    return api.get(`/api/admin/logs/${filename}`, { params: { lines } })
  }
}
