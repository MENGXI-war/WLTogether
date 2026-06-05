import api from './index'

export default {
  // Playlist CRUD
  create(groupId, data) {
    return api.post(`/api/groups/${groupId}/playlists`, data)
  },
  listByGroup(groupId) {
    return api.get(`/api/groups/${groupId}/playlists`)
  },
  get(id) {
    return api.get(`/api/playlists/${id}`)
  },
  update(id, data) {
    return api.put(`/api/playlists/${id}`, data)
  },
  remove(id) {
    return api.delete(`/api/playlists/${id}`)
  },

  // Tracks
  listTracks(playlistId) {
    return api.get(`/api/playlists/${playlistId}/tracks`)
  },
  addTrack(playlistId, data) {
    return api.post(`/api/playlists/${playlistId}/tracks`, data)
  },
  removeTrack(playlistId, trackId) {
    return api.delete(`/api/playlists/${playlistId}/tracks/${trackId}`)
  },
  reorderTracks(playlistId, trackIds) {
    return api.put(`/api/playlists/${playlistId}/tracks/reorder`, trackIds)
  }
}
