import api from './index'

export default {
  create(groupId, data) {
    return api.post(`/api/groups/${groupId}/sessions`, data)
  },
  list(groupId) {
    return api.get(`/api/groups/${groupId}/sessions`)
  },
  get(id) {
    return api.get(`/api/sessions/${id}`)
  },
  join(id) {
    return api.post(`/api/sessions/${id}/join`)
  },
  end(id) {
    return api.post(`/api/sessions/${id}/end`)
  }
}
