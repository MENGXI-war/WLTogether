<template>
  <div class="chat-panel">
    <!-- Header -->
    <div class="chat-header">
      <span class="chat-title">聊天</span>
      <span v-if="typingText" class="typing-indicator">{{ typingText }}</span>
    </div>

    <!-- Message list -->
    <div class="chat-messages" ref="messagesEl" @scroll="onScroll">
      <div v-if="hasMore" class="load-more">
        <el-button v-if="!loadingMore" link type="primary" size="small" @click="onLoadMore" :loading="loadingMore">
          加载更早的消息
        </el-button>
        <el-icon v-else class="is-loading"><Loading /></el-icon>
      </div>

      <div v-if="loading" class="chat-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>

      <div v-else-if="messages.length === 0" class="chat-empty">
        <span>暂无消息，发送第一条消息吧</span>
      </div>

      <template v-else>
        <ChatBubble
          v-for="(msg, idx) in messages"
          :key="msg.id"
          :message="msg"
          :show-avatar="shouldShowAvatar(idx)"
        />
      </template>

      <div ref="messagesEndEl" />
    </div>

    <!-- Input area -->
    <div class="chat-input">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息..."
        resize="none"
        @keydown.enter.exact="onSend"
        @input="onInput"
      />
      <el-button type="primary" :icon="Promotion" circle @click="onSend" :disabled="!inputText.trim()" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Promotion, Loading } from '@element-plus/icons-vue'
import { useChat } from '@/composables/useChat'
import ChatBubble from './ChatBubble.vue'

const props = defineProps({
  groupId: {
    type: [String, Number],
    required: true
  }
})

const {
  loadMore,
  sendMessage,
  startTyping,
  stopTyping,
  onMessage,
  loading,
  loadingMore
} = useChat(props.groupId)

const messagesEl = ref(null)
const messagesEndEl = ref(null)
const inputText = ref('')
const hasMore = ref(true)
const messages = ref([])
const typingUsers = ref([])
let typingTimer = null
let scrollToBottomNext = true

const typingText = computed(() => {
  const names = typingUsers.value.map(u => u.nickname || `用户${u.userId}`)
  if (names.length === 0) return ''
  if (names.length === 1) return `${names[0]} 正在输入...`
  if (names.length === 2) return `${names[0]} 和 ${names[1]} 正在输入...`
  return `${names[0]} 等${names.length}人正在输入...`
})

// Initialize: load messages and subscribe to WebSocket
async function init() {
  const chatStore = await import('@/stores/chat').then(m => m.useChatStore())
  await chatStore.loadMessages(props.groupId)
  syncFromStore()

  // Subscribe to WS messages
  onMessage((msg) => {
    syncFromStore()
    scrollToBottom()
  })
}

function syncFromStore() {
  import('@/stores/chat').then(({ useChatStore }) => {
    const chatStore = useChatStore()
    const state = chatStore.groupMessages.get(Number(props.groupId))
    if (state) {
      messages.value = state.messages
      hasMore.value = state.hasMore
    }
    typingUsers.value = chatStore.getTypingUsers(props.groupId)
  })
}

function shouldShowAvatar(index) {
  if (index === 0) return true
  const prev = messages.value[index - 1]
  const curr = messages.value[index]
  return prev?.senderId !== curr?.senderId
}

function onScroll() {
  const el = messagesEl.value
  if (!el) return
  // Consider "at bottom" if within 60px of the bottom
  scrollToBottomNext = el.scrollTop + el.clientHeight >= el.scrollHeight - 60
}

async function onLoadMore() {
  if (loadingMore.value) return
  const prevHeight = messagesEl.value?.scrollHeight || 0
  await loadMore()
  syncFromStore()
  // Maintain scroll position after prepending
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight - prevHeight
  }
}

async function onSend() {
  const text = inputText.value.trim()
  if (!text) return

  sendMessage(text)
  inputText.value = ''
  scrollToBottomNext = true
  stopTyping()
  if (typingTimer) clearTimeout(typingTimer)
}

let inputDebounce = null
function onInput() {
  if (inputText.value.trim()) {
    startTyping()
    if (typingTimer) clearTimeout(typingTimer)
    typingTimer = setTimeout(() => stopTyping(), 2000)
  }
}

function scrollToBottom() {
  if (!scrollToBottomNext) return
  nextTick(() => {
    messagesEndEl.value?.scrollIntoView({ behavior: 'smooth' })
  })
}

watch(() => props.groupId, () => {
  init()
}, { immediate: true })

onUnmounted(() => {
  if (typingTimer) clearTimeout(typingTimer)
  stopTyping()
})
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-chat-bg);
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 48px;
}

.chat-title {
  font-size: 15px;
  font-weight: 500;
}

.typing-indicator {
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 0;
}

.chat-loading,
.chat-empty {
  display: flex;
  justify-content: center;
  padding: 40px 0;
  color: #909399;
  font-size: 14px;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}

.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.chat-input :deep(.el-textarea__inner) {
  border-radius: 8px;
}

.chat-input .el-button {
  flex-shrink: 0;
  margin-bottom: 4px;
}
</style>
