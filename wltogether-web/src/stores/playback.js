import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePlaybackStore = defineStore('playback', () => {
  const currentSessionId = ref(null)
  const playing = ref(false)
  const position = ref(0)
  const serverTimestamp = ref(0)
  const clockOffset = ref(0) // ms: localTime + offset ≈ serverTime
  const playMode = ref('HOST_ONLY')
  const hostId = ref(null)
  const participants = ref([])
  const offsetSamples = ref([])

  const correctedPosition = computed(() => {
    if (!serverTimestamp.value) return position.value
    const elapsed = Date.now() - serverTimestamp.value + clockOffset.value
    if (playing.value) {
      return position.value + elapsed / 1000
    }
    return position.value
  })

  function updatePlaybackState(state) {
    if (state.playing !== undefined) playing.value = state.playing
    if (state.position !== undefined) position.value = state.position
    if (state.serverTimestamp !== undefined) serverTimestamp.value = state.serverTimestamp
    if (state.hostId !== undefined) hostId.value = state.hostId
    if (state.playMode !== undefined) playMode.value = state.playMode
  }

  function addOffsetSample(clientTime, serverTime, rtt) {
    const offset = serverTime - (clientTime + rtt / 2)
    offsetSamples.value.push(offset)
    // Keep last 5 samples
    if (offsetSamples.value.length > 5) {
      offsetSamples.value.shift()
    }
    // Use median
    const sorted = [...offsetSamples.value].sort((a, b) => a - b)
    clockOffset.value = sorted[Math.floor(sorted.length / 2)]
  }

  function setParticipants(list) {
    participants.value = list
  }

  function clear() {
    currentSessionId.value = null
    playing.value = false
    position.value = 0
    serverTimestamp.value = 0
    clockOffset.value = 0
    hostId.value = null
    playMode.value = 'HOST_ONLY'
    participants.value = []
    offsetSamples.value = []
  }

  return {
    currentSessionId,
    playing,
    position,
    serverTimestamp,
    clockOffset,
    playMode,
    hostId,
    participants,
    correctedPosition,
    updatePlaybackState,
    addOffsetSample,
    setParticipants,
    clear
  }
})
