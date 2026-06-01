<template>
  <div class="chat-bubble" :class="{ 'is-own': isOwn, 'is-system': message.messageType === 'SYSTEM' }">
    <!-- System message -->
    <div v-if="message.messageType === 'SYSTEM'" class="system-message">
      <span>{{ message.content }}</span>
    </div>

    <!-- Regular message -->
    <template v-else>
      <div v-if="!isOwn && showAvatar" class="bubble-avatar">
        <el-avatar :size="36" :src="message.senderAvatarUrl">
          {{ message.senderNickname?.charAt(0) || '?' }}
        </el-avatar>
      </div>
      <div class="bubble-body">
        <div v-if="!isOwn && showAvatar" class="bubble-sender">
          <span class="sender-name">{{ message.senderNickname || message.senderId }}</span>
        </div>
        <!-- Reply reference -->
        <div v-if="message.replyToId" class="reply-ref">
          <el-icon><ChatLineRound /></el-icon>
          <span>回复 #{{ message.replyToId }}</span>
        </div>
        <div class="bubble-content">
          <div class="bubble-text">{{ message.content }}</div>
        </div>
        <div class="bubble-meta">
          <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
          <el-icon v-if="message.isPinned" class="pinned-icon"><Pushpin /></el-icon>
        </div>
      </div>
      <div v-if="isOwn" class="bubble-avatar">
        <el-avatar :size="36" :src="message.senderAvatarUrl">
          {{ message.senderNickname?.charAt(0) || '?' }}
        </el-avatar>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  showAvatar: {
    type: Boolean,
    default: true
  }
})

const authStore = useAuthStore()

const isOwn = computed(() => props.message.senderId === authStore.user?.id)

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  if (isToday) return `${hours}:${minutes}`
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}
</script>

<style scoped>
.chat-bubble {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
  padding: 0 16px;
}

.chat-bubble.is-own {
  flex-direction: row-reverse;
}

.chat-bubble.is-system {
  justify-content: center;
  margin-bottom: 8px;
}

.system-message {
  color: #909399;
  font-size: 12px;
  background: #f0f2f5;
  padding: 4px 12px;
  border-radius: 4px;
}

.bubble-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.bubble-body {
  max-width: 65%;
  min-width: 60px;
}

.is-own .bubble-body {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.bubble-sender {
  margin-bottom: 2px;
}

.sender-name {
  font-size: 12px;
  color: #909399;
}

.reply-ref {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
}

.bubble-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bubble-text {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  background: var(--color-message-other);
}

.is-own .bubble-text {
  background: var(--color-message-own);
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.bubble-time {
  font-size: 11px;
  color: #c0c4cc;
}

.pinned-icon {
  font-size: 12px;
  color: #e6a23c;
}
</style>
