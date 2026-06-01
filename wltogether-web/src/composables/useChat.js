import { ref } from 'vue'
import { useChatStore } from '@/stores/chat'
import { useWebSocket } from './useWebSocket'

export function useChat(groupId) {
  const chatStore = useChatStore()
  const { send, subscribe } = useWebSocket()
  const loading = ref(false)
  const loadingMore = ref(false)
  const sending = ref(false)

  async function init() {
    loading.value = true
    try {
      await chatStore.loadMessages(groupId)
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    if (loadingMore.value) return
    loadingMore.value = true
    try {
      return await chatStore.loadMore(groupId)
    } finally {
      loadingMore.value = false
    }
  }

  function sendMessage(content, sessionId = null, replyToId = null) {
    send('/chat.send', {
      groupId: Number(groupId),
      content,
      sessionId,
      replyToId
    })
  }

  function startTyping() {
    send('/typing.start', { groupId: Number(groupId) })
  }

  function stopTyping() {
    send('/typing.stop', { groupId: Number(groupId) })
  }

  function onMessage(callback) {
    return subscribe(`/topic/group/${groupId}`, (message) => {
      try {
        const body = JSON.parse(message.body)
        if (body.id && body.content) {
          // It's a chat message
          chatStore.addMessage(groupId, body)
          callback?.(body)
        } else if (body.type === 'typing.status') {
          if (body.stopped) {
            chatStore.removeTypingUser(groupId, body.userId)
          } else {
            chatStore.addTypingUser(groupId, body.userId, body.nickname)
          }
        }
      } catch (e) {
        console.error('[Chat] Failed to parse message:', e)
      }
    })
  }

  function onSessionEvent(callback) {
    return subscribe(`/topic/group/${groupId}`, (message) => {
      try {
        const body = JSON.parse(message.body)
        if (body.type?.startsWith('session.')) {
          callback?.(body)
        }
      } catch (e) {
        // ignore parse errors for non-JSON messages
      }
    })
  }

  const messages = chatStore.groupMessages.get(Number(groupId))
  const typingUsers = chatStore.getTypingUsers(groupId)

  return {
    messages,
    typingUsers,
    loading,
    loadingMore,
    sending,
    init,
    loadMore,
    sendMessage,
    startTyping,
    stopTyping,
    onMessage,
    onSessionEvent
  }
}
