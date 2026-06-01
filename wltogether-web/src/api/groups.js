import api from './index'

export default {
  create(data) {
    return api.post('/api/groups', data)
  },
  list() {
    return api.get('/api/groups')
  },
  get(id) {
    return api.get(`/api/groups/${id}`)
  },
  update(id, data) {
    return api.put(`/api/groups/${id}`, data)
  },
  remove(id) {
    return api.delete(`/api/groups/${id}`)
  },
  listMembers(groupId) {
    return api.get(`/api/groups/${groupId}/members`)
  },
  inviteMember(groupId, username) {
    return api.post(`/api/groups/${groupId}/members`, { username })
  },
  removeMember(groupId, userId) {
    return api.delete(`/api/groups/${groupId}/members/${userId}`)
  }
}
