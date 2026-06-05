<template>
  <div class="member-list">
    <div class="member-header">
      <span>群成员 ({{ members.length }})</span>
      <el-button text type="primary" size="small" @click="$emit('invite')">
        <el-icon><Plus /></el-icon> 邀请
      </el-button>
    </div>
    <div class="member-items">
      <div
        v-for="member in members"
        :key="member.userId"
        class="member-item"
      >
        <el-avatar :size="32" :src="member.avatarUrl">
          {{ member.nickname?.charAt(0) || member.username?.charAt(0) || '?' }}
        </el-avatar>
        <div class="member-info">
          <span class="member-name">{{ member.nicknameInGroup || member.nickname || member.username }}</span>
          <el-tag
            v-if="member.role === 'OWNER'"
            size="small"
            type="warning"
            effect="plain"
          >群主</el-tag>
          <el-tag
            v-else-if="member.role === 'ADMIN'"
            size="small"
            type="success"
            effect="plain"
          >管理</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'

defineProps({
  members: {
    type: Array,
    default: () => []
  }
})

defineEmits(['invite'])
</script>

<style scoped>
.member-list {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.member-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 500;
  border-bottom: 1px solid var(--color-border);
}

.member-items {
  max-height: 360px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.member-item:hover {
  background: var(--color-bg);
}

.member-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
