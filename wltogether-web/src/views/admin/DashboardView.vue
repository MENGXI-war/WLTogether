<template>
  <div class="dashboard-page">
    <!-- Stats Cards -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- System Info -->
    <el-row :gutter="16" class="info-row">
      <el-col :xs="24" :md="12">
        <el-card header="系统资源" shadow="hover">
          <div class="resource-list">
            <div class="resource-item">
              <span class="resource-label">堆内存使用</span>
              <div class="resource-value-bar">
                <el-progress
                  :percentage="memory.usagePercent"
                  :color="memory.usagePercent > 80 ? '#f56c6c' : '#409eff'"
                  :format="() => `${memory.heapUsedMb} / ${memory.heapMaxMb} MB`"
                />
              </div>
            </div>
            <div class="resource-item">
              <span class="resource-label">CPU 核心数</span>
              <span class="resource-value">{{ cpu.availableProcessors }} 核</span>
            </div>
            <div class="resource-item">
              <span class="resource-label">系统负载</span>
              <span class="resource-value">{{ cpu.systemLoad }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card header="JVM 信息" shadow="hover">
          <div class="resource-list">
            <div class="resource-item">
              <span class="resource-label">运行时长</span>
              <span class="resource-value">{{ formatUptime(jvm.uptimeSeconds) }}</span>
            </div>
            <div class="resource-item">
              <span class="resource-label">活跃线程</span>
              <span class="resource-value">{{ jvm.threadCount }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { User, Grid as GridIcon, VideoCamera, ChatDotRound } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const memory = reactive({ heapUsedMb: 0, heapMaxMb: 0, usagePercent: 0 })
const cpu = reactive({ availableProcessors: 0, systemLoad: '0' })
const jvm = reactive({ uptimeSeconds: 0, threadCount: 0 })
const counts = reactive({ totalUsers: 0, activeUsers: 0, totalGroups: 0, activeSessions: 0, totalMessages: 0 })
const loading = ref(false)

const statCards = computed(() => [
  {
    label: '总用户数',
    value: counts.totalUsers,
    icon: User,
    bg: '#ecf5ff',
    color: '#409eff'
  },
  {
    label: '活跃用户',
    value: counts.activeUsers,
    icon: User,
    bg: '#e8f5e9',
    color: '#67c23a'
  },
  {
    label: '群组数',
    value: counts.totalGroups,
    icon: GridIcon,
    bg: '#fff7e6',
    color: '#e6a23c'
  },
  {
    label: '活跃会话',
    value: counts.activeSessions,
    icon: VideoCamera,
    bg: '#fef0f0',
    color: '#f56c6c'
  },
  {
    label: '总消息数',
    value: counts.totalMessages,
    icon: ChatDotRound,
    bg: '#f0f9ff',
    color: '#0ea5e9'
  }
])

function formatUptime(seconds) {
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const parts = []
  if (d > 0) parts.push(`${d}天`)
  if (h > 0) parts.push(`${h}小时`)
  parts.push(`${m}分钟`)
  return parts.join(' ')
}

async function fetchStats() {
  loading.value = true
  try {
    const res = await adminApi.getStats()
    const data = res.data
    if (data.memory) Object.assign(memory, data.memory)
    if (data.cpu) Object.assign(cpu, data.cpu)
    if (data.jvm) Object.assign(jvm, data.jvm)
    if (data.counts) Object.assign(counts, data.counts)
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchStats())
</script>

<style scoped>
.dashboard-page {
  max-width: 1200px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  margin-bottom: 16px;
}
.stat-card :deep(.el-card__body) {
  padding: 20px;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  min-width: 0;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.info-row {
  margin-bottom: 16px;
}

.resource-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.resource-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.resource-label {
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
}
.resource-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}
.resource-value-bar {
  flex: 1;
  min-width: 0;
}
</style>
