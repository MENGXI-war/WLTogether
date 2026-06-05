<template>
  <div class="session-card" @click="$emit('click', session.id)">
    <div class="session-icon">
      <el-icon :size="28">
        <VideoCamera v-if="session.mediaType === 'VIDEO' || !session.mediaType" />
        <Headset v-else-if="session.mediaType === 'MUSIC'" />
        <Picture v-else />
      </el-icon>
    </div>
    <div class="session-info">
      <span class="session-name">{{ session.fileName || '未命名会话' }}</span>
      <div class="session-meta">
        <el-tag :type="statusType" size="small">{{ statusText }}</el-tag>
        <span class="participant-count">{{ session.participantCount || 1 }} 人</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { VideoCamera, Headset, Picture } from '@element-plus/icons-vue'

const props = defineProps({
  session: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

const statusType = computed(() => {
  switch (props.session.status) {
    case 'ACTIVE': return 'success'
    case 'ENDED': return 'info'
    default: return ''
  }
})

const statusText = computed(() => {
  switch (props.session.status) {
    case 'CREATED': return '等待加入'
    case 'ACTIVE': return '进行中'
    case 'ENDED': return '已结束'
    default: return props.session.status
  }
})
</script>

<style scoped>
.session-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-radius: 8px;
}

.session-card:hover {
  background: var(--color-bg);
}

.session-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  border-radius: 10px;
  color: var(--color-primary);
}

.session-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.participant-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
