import api from './index'

export default {
  getMessages(groupId, cursor = null, limit = 50) {
    const params = { limit }
    if (cursor) params.cursor = cursor
    return api.get(`/api/groups/${groupId}/messages`, { params })
  },
  getPinned(groupId) {
    return api.get(`/api/groups/${groupId}/messages/pinned`)
  },
  uploadImage(groupId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/api/groups/${groupId}/images`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })
  }
}
