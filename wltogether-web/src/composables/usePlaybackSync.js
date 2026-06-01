import { ref, onUnmounted } from 'vue'
import { usePlaybackStore } from '@/stores/playback'
import { useWebSocket } from './useWebSocket'

export function usePlaybackSync(sessionId) {
  const playbackStore = usePlaybackStore()
  const { send, subscribe } = useWebSocket()
  const syncInterval = ref(null)
  let sessionSub = null
  let syncPongSub = null

  function startSync() {
    playbackStore.currentSessionId = sessionId

    // Subscribe to playback state changes
    sessionSub = subscribe(`/topic/session/${sessionId}`, (message) => {
      try {
        const body = JSON.parse(message.body)
        if (body.type === 'playback.state') {
          playbackStore.updatePlaybackState({
            playing: body.playing,
            position: body.position || 0,
            serverTimestamp: body.serverTimestamp
          })
        } else if (body.type === 'playback.next') {
          playbackStore.updatePlaybackState({
            playing: true,
            position: body.position || 0,
            serverTimestamp: body.serverTimestamp
          })
        } else if (body.type === 'session.user_joined') {
          playbackStore.participants.push({
            userId: body.userId,
            nickname: body.nickname
          })
        } else if (body.type === 'session.user_left') {
          playbackStore.participants = playbackStore.participants.filter(
            p => p.userId !== body.userId
          )
        } else if (body.type === 'session.ended') {
          playbackStore.updatePlaybackState({ playing: false })
        }
      } catch (e) {
        // ignore non-JSON messages
      }
    })

    // Subscribe to clock sync pong
    syncPongSub = subscribe('/user/queue/sync.pong', (message) => {
      try {
        const body = JSON.parse(message.body)
        if (body.type === 'sync.pong') {
          const t3 = Date.now()
          const t1 = body.clientTime
          playbackStore.addOffsetSample(body.clientTime, body.serverTime, t3 - t1)
        }
      } catch (e) {
        // ignore
      }
    })

    // Start clock sync loop (every 5 seconds)
    syncInterval.value = setInterval(() => {
      send('/sync.ping', {
        clientTime: Date.now()
      })
    }, 5000)
  }

  function stopSync() {
    if (syncInterval.value) {
      clearInterval(syncInterval.value)
      syncInterval.value = null
    }
    sessionSub?.unsubscribe()
    syncPongSub?.unsubscribe()
    playbackStore.clear()
  }

  function sendPlay() {
    send('/playback.play', {
      sessionId: Number(sessionId),
      position: playbackStore.correctedPosition
    })
  }

  function sendPause() {
    send('/playback.pause', {
      sessionId: Number(sessionId),
      position: playbackStore.correctedPosition
    })
  }

  function sendSeek(position) {
    send('/playback.seek', {
      sessionId: Number(sessionId),
      position
    })
  }

  function sendNext(position = 0) {
    send('/playback.next', {
      sessionId: Number(sessionId),
      position
    })
  }

  // Auto-cleanup on component unmount
  onUnmounted(() => {
    stopSync()
  })

  return {
    startSync,
    stopSync,
    sendPlay,
    sendPause,
    sendSeek,
    sendNext
  }
}
