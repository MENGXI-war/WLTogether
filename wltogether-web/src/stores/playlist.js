import { defineStore } from 'pinia'
import { ref } from 'vue'
import playlistsApi from '@/api/playlists'

export const usePlaylistStore = defineStore('playlist', () => {
  const playlists = ref([])
  const currentPlaylist = ref(null)
  const loading = ref(false)

  async function fetchPlaylists(groupId) {
    loading.value = true
    try {
      playlists.value = await playlistsApi.listByGroup(groupId)
    } catch (e) {
      playlists.value = []
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchPlaylist(id) {
    loading.value = true
    try {
      currentPlaylist.value = await playlistsApi.get(id)
      return currentPlaylist.value
    } finally {
      loading.value = false
    }
  }

  async function createPlaylist(groupId, data) {
    const pl = await playlistsApi.create(groupId, data)
    playlists.value.unshift(pl)
    return pl
  }

  async function updatePlaylist(id, data) {
    const pl = await playlistsApi.update(id, data)
    const idx = playlists.value.findIndex(p => p.id === id)
    if (idx >= 0) playlists.value[idx] = pl
    if (currentPlaylist.value?.id === id) {
      currentPlaylist.value = { ...pl, tracks: currentPlaylist.value.tracks }
    }
    return pl
  }

  async function deletePlaylist(id) {
    await playlistsApi.remove(id)
    playlists.value = playlists.value.filter(p => p.id !== id)
    if (currentPlaylist.value?.id === id) currentPlaylist.value = null
  }

  // Track operations
  async function fetchTracks(playlistId) {
    const tracks = await playlistsApi.listTracks(playlistId)
    if (currentPlaylist.value?.id === playlistId) {
      currentPlaylist.value = { ...currentPlaylist.value, tracks }
    }
    return tracks
  }

  async function addTrack(playlistId, data) {
    const track = await playlistsApi.addTrack(playlistId, data)
    if (currentPlaylist.value?.id === playlistId) {
      const tracks = [...(currentPlaylist.value.tracks || []), track]
      currentPlaylist.value = { ...currentPlaylist.value, tracks, trackCount: tracks.length }
    }
    return track
  }

  async function removeTrack(playlistId, trackId) {
    await playlistsApi.removeTrack(playlistId, trackId)
    if (currentPlaylist.value?.id === playlistId) {
      const tracks = currentPlaylist.value.tracks.filter(t => t.id !== trackId)
      currentPlaylist.value = { ...currentPlaylist.value, tracks, trackCount: tracks.length }
    }
  }

  async function reorderTracks(playlistId, trackIds) {
    const tracks = await playlistsApi.reorderTracks(playlistId, trackIds)
    if (currentPlaylist.value?.id === playlistId) {
      currentPlaylist.value = { ...currentPlaylist.value, tracks }
    }
    return tracks
  }

  function clearCurrent() {
    currentPlaylist.value = null
  }

  return {
    playlists, currentPlaylist, loading,
    fetchPlaylists, fetchPlaylist,
    createPlaylist, updatePlaylist, deletePlaylist,
    fetchTracks, addTrack, removeTrack, reorderTracks,
    clearCurrent
  }
})
