import api from './index'

export default {
  getMessages(groupId, cursor = null, limit = 50) {
    const params = { limit }
    if (cursor) params.cursor = cursor
    return api.get(`/api/groups/${groupId}/messages`, { params })
  },
  getPinned(groupId) {
    return api.get(`/api/groups/${groupId}/messages/pinned`)
  }
}
