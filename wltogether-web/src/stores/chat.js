import { defineStore } from 'pinia'
import { ref } from 'vue'
import chatApi from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  // Map<groupId, { messages: [], cursor: number, hasMore: boolean }>
  const groupMessages = ref(new Map())
  // Map<groupId, Set<{userId, nickname}>> for typing users
  const typingUsers = ref(new Map())

  function getGroupState(groupId) {
    if (!groupMessages.value.has(groupId)) {
      groupMessages.value.set(groupId, {
        messages: [],
        cursor: null,
        hasMore: true
      })
    }
    return groupMessages.value.get(groupId)
  }

  async function loadMessages(groupId) {
    const state = getGroupState(groupId)
    const res = await chatApi.getMessages(groupId, null, 50)
    // Server returns DESC (newest first), reverse to ASC for display
    const messages = Array.isArray(res) ? [...res].reverse() : []
    state.messages = messages
    if (messages.length > 0) {
      state.cursor = messages[0].id
    }
    state.hasMore = messages.length >= 50
    return messages
  }

  async function loadMore(groupId) {
    const state = getGroupState(groupId)
    if (!state.hasMore || !state.cursor) return []

    const res = await chatApi.getMessages(groupId, state.cursor, 50)
    const older = Array.isArray(res) ? [...res].reverse() : []
    if (older.length > 0) {
      state.messages = [...older, ...state.messages]
      state.cursor = older[0].id
    }
    state.hasMore = older.length >= 50
    return older
  }

  function addMessage(groupId, message) {
    const state = getGroupState(groupId)
    state.messages.push(message)
    // Cap at 500 messages in memory
    if (state.messages.length > 500) {
      state.messages = state.messages.slice(-300)
    }
  }

  function addTypingUser(groupId, userId, nickname) {
    if (!typingUsers.value.has(groupId)) {
      typingUsers.value.set(groupId, new Map())
    }
    typingUsers.value.get(groupId).set(userId, nickname)
  }

  function removeTypingUser(groupId, userId) {
    const group = typingUsers.value.get(groupId)
    if (group) {
      group.delete(userId)
    }
  }

  function getTypingUsers(groupId) {
    const group = typingUsers.value.get(groupId)
    if (!group) return []
    return Array.from(group.entries()).map(([userId, nickname]) => ({ userId, nickname }))
  }

  function clearGroup(groupId) {
    groupMessages.value.delete(groupId)
    typingUsers.value.delete(groupId)
  }

  return {
    groupMessages,
    typingUsers,
    loadMessages,
    loadMore,
    addMessage,
    addTypingUser,
    removeTypingUser,
    getTypingUsers,
    clearGroup
  }
})
