import { useWebSocketStore } from '@/stores/websocket'
import { useAuthStore } from '@/stores/auth'

export function useWebSocket() {
  const wsStore = useWebSocketStore()
  const authStore = useAuthStore()

  function connect() {
    if (authStore.isAuthenticated) {
      wsStore.connect()
    }
  }

  function disconnect() {
    wsStore.disconnect()
  }

  function subscribe(destination, callback) {
    return wsStore.subscribe(destination, callback)
  }

  function send(destination, body) {
    wsStore.send(destination, body)
  }

  return {
    connected: wsStore.connected,
    connect,
    disconnect,
    subscribe,
    send
  }
}
