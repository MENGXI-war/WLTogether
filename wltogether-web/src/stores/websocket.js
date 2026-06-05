import { defineStore } from 'pinia'
import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from './auth'
import { ElMessage } from 'element-plus'

const RECONNECT_DELAYS = [2000, 5000, 10000, 30000]

export const useWebSocketStore = defineStore('websocket', () => {
  const client = ref(null)
  const connected = ref(false)
  const subscriptions = ref(new Map())
  let reconnectAttempt = 0
  let reconnectTimer = null
  let intentionalDisconnect = false

  function getWsUrl() {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${location.host}/ws`
  }

  function connect() {
    const authStore = useAuthStore()
    if (!authStore.accessToken) {
      console.warn('[WS] Cannot connect: no access token')
      return
    }

    intentionalDisconnect = false

    if (client.value) {
      client.value.deactivate()
    }

    const stompClient = new Client({
      brokerURL: getWsUrl(),
      connectHeaders: {
        token: authStore.accessToken
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 0, // We handle reconnection ourselves
      onConnect: () => {
        connected.value = true
        reconnectAttempt = 0
        console.log('[WS] Connected')
        // Resubscribe to previously active subscriptions
        subscriptions.value.forEach((callback, destination) => {
          stompClient.subscribe(destination, callback)
        })
      },
      onDisconnect: () => {
        connected.value = false
        console.log('[WS] Disconnected')
        if (!intentionalDisconnect) {
          scheduleReconnect()
        }
      },
      onStompError: (frame) => {
        console.error('[WS] STOMP error:', frame.headers?.message)
        if (frame.headers?.message?.includes('AUTH')) {
          ElMessage.error('WebSocket 认证失败，请重新登录')
          authStore.logout()
        }
      },
      onWebSocketError: (event) => {
        console.error('[WS] WebSocket error:', event)
      }
    })

    client.value = stompClient
    stompClient.activate()
  }

  function disconnect() {
    intentionalDisconnect = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    subscriptions.value.clear()
    if (client.value) {
      client.value.deactivate()
      client.value = null
    }
    connected.value = false
  }

  function scheduleReconnect() {
    if (intentionalDisconnect) return
    const delay = RECONNECT_DELAYS[Math.min(reconnectAttempt, RECONNECT_DELAYS.length - 1)]
    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${reconnectAttempt + 1})`)
    reconnectTimer = setTimeout(() => {
      reconnectAttempt++
      connect()
    }, delay)
  }

  function subscribe(destination, callback) {
    if (!client.value || !connected.value) {
      // Queue subscription for when connected
      subscriptions.value.set(destination, callback)
      return { unsubscribe: () => subscriptions.value.delete(destination) }
    }

    const sub = client.value.subscribe(destination, callback)
    subscriptions.value.set(destination, callback)

    return {
      unsubscribe: () => {
        sub.unsubscribe()
        subscriptions.value.delete(destination)
      }
    }
  }

  function send(destination, body) {
    if (!client.value || !connected.value) {
      console.warn('[WS] Cannot send: not connected')
      ElMessage.warning('消息发送失败：WebSocket 未连接，正在尝试重连…')
      return false
    }
    client.value.publish({
      destination: `/app${destination}`,
      body: JSON.stringify(body)
    })
    return true
  }

  return {
    connected,
    connect,
    disconnect,
    subscribe,
    send
  }
})
