<template>
  <div class="group-header">
    <div class="header-left">
      <el-avatar :size="40" :src="group.avatarUrl">
        {{ group.name?.charAt(0) || 'G' }}
      </el-avatar>
      <div class="header-info">
        <span class="group-name">{{ group.name }}</span>
        <span class="member-count">{{ group.memberCount || members.length }} 人</span>
      </div>
    </div>
    <div class="header-right">
      <el-button text :icon="Search" @click="$emit('search')" />
      <el-button text :icon="Setting" @click="$emit('settings')" />
    </div>
  </div>
</template>

<script setup>
import { Search, Setting } from '@element-plus/icons-vue'

defineProps({
  group: {
    type: Object,
    default: () => ({})
  },
  members: {
    type: Array,
    default: () => []
  }
})

defineEmits(['search', 'settings'])
</script>

<style scoped>
.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.group-name {
  font-size: 16px;
  font-weight: 600;
}

.member-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.header-right {
  display: flex;
  gap: 4px;
}
</style>
