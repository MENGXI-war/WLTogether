import api from './index'

export default {
  // Basic CRUD
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

  // Members
  listMembers(groupId) {
    return api.get(`/api/groups/${groupId}/members`)
  },
  inviteMember(groupId, username) {
    return api.post(`/api/groups/${groupId}/members`, { username })
  },
  removeMember(groupId, userId) {
    return api.delete(`/api/groups/${groupId}/members/${userId}`)
  },

  // Self-leave
  leave(groupId) {
    return api.delete(`/api/groups/${groupId}/members/me`)
  },

  // Role management
  changeMemberRole(groupId, userId, role) {
    return api.put(`/api/groups/${groupId}/members/${userId}/role`, { role })
  },
  transferOwner(groupId, newOwnerId) {
    return api.put(`/api/groups/${groupId}/owner`, { newOwnerId })
  },

  // Mute
  muteMember(groupId, userId, duration) {
    return api.put(`/api/groups/${groupId}/members/${userId}/mute`, { duration })
  },

  // Nickname in group (self)
  updateNickname(groupId, nicknameInGroup) {
    return api.put(`/api/groups/${groupId}/members/me/nickname`, { nicknameInGroup })
  },

  // Join requests
  requestJoin(groupId) {
    return api.post(`/api/groups/${groupId}/join-requests`)
  },
  listJoinRequests(groupId) {
    return api.get(`/api/groups/${groupId}/join-requests`)
  },
  approveJoinRequest(groupId, requestId) {
    return api.put(`/api/groups/${groupId}/join-requests/${requestId}/approve`)
  },
  rejectJoinRequest(groupId, requestId) {
    return api.put(`/api/groups/${groupId}/join-requests/${requestId}/reject`)
  },

  // Avatar
  uploadAvatar(groupId, file) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post(`/api/groups/${groupId}/avatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAvatar(groupId) {
    return api.delete(`/api/groups/${groupId}/avatar`)
  }
}
