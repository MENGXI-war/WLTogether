<template>
  <div class="logs-page">
    <el-row :gutter="16" style="height: 100%">
      <!-- File List -->
      <el-col :xs="24" :md="7" class="log-file-col">
        <el-card header="日志文件" shadow="hover" class="log-file-card">
          <div v-loading="loadingFiles">
            <el-empty v-if="!loadingFiles && files.length === 0" description="暂无日志文件" :image-size="60" />
            <div
              v-for="f in files"
              :key="f.name"
              :class="['log-file-item', { active: selectedFile === f.name }]"
              @click="selectFile(f.name)"
            >
              <div class="file-info">
                <el-icon :size="18" color="#409eff"><Document /></el-icon>
                <span class="file-name">{{ f.name }}</span>
              </div>
              <div class="file-meta">
                <span class="file-size">{{ formatBytes(f.size) }}</span>
                <span class="file-time">{{ formatTimestamp(f.lastModified) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Log Content -->
      <el-col :xs="24" :md="17" class="log-content-col">
        <el-card shadow="hover" class="log-content-card">
          <template #header>
            <div class="log-content-header">
              <span class="log-content-title">
                {{ selectedFile || '选择日志文件' }}
              </span>
              <div class="log-content-actions">
                <span class="lines-label">显示行数</span>
                <el-input-number
                  v-model="linesCount"
                  :min="50"
                  :max="2000"
                  :step="50"
                  size="small"
                  style="width: 120px"
                  @change="fetchContent"
                />
                <el-button size="small" @click="fetchContent" :loading="loadingContent" :disabled="!selectedFile">
                  刷新
                </el-button>
              </div>
            </div>
          </template>
          <div v-if="!selectedFile" class="no-file-selected">
            <el-empty description="请从左侧选择日志文件" :image-size="80" />
          </div>
          <div v-else v-loading="loadingContent" class="log-content-wrap">
            <pre v-if="content" class="log-content">{{ content }}</pre>
            <el-empty v-else description="该文件暂无内容" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Document } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const files = ref([])
const loadingFiles = ref(false)
const selectedFile = ref('')
const content = ref('')
const loadingContent = ref(false)
const linesCount = ref(200)

function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, v = bytes
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++ }
  return v.toFixed(1) + ' ' + units[i]
}
function formatTimestamp(ts) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('zh-CN')
}

async function loadFiles() {
  loadingFiles.value = true
  try {
    const res = await adminApi.getLogFiles()
    files.value = res.data || []
  } finally {
    loadingFiles.value = false
  }
}

async function selectFile(filename) {
  selectedFile.value = filename
  content.value = ''
  await fetchContent()
}

async function fetchContent() {
  if (!selectedFile.value) return
  loadingContent.value = true
  try {
    const res = await adminApi.readLogFile(selectedFile.value, linesCount.value)
    content.value = res.data || ''
  } finally {
    loadingContent.value = false
  }
}

loadFiles()
</script>

<style scoped>
.logs-page {
  height: 100%;
}
.logs-page :deep(.el-row) {
  height: 100%;
}
.log-file-col {
  height: 100%;
}
.log-content-col {
  height: 100%;
}
.log-file-card {
  height: 100%;
}
.log-file-card :deep(.el-card__body) {
  padding: 8px;
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}
.log-content-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.log-content-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 0;
  display: flex;
}

.log-file-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}
.log-file-item:hover {
  background: #f5f7fa;
}
.log-file-item.active {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  padding-left: 9px;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  word-break: break-all;
}
.file-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  margin-left: 26px;
}
.file-size {
  font-size: 11px;
  color: #909399;
}
.file-time {
  font-size: 11px;
  color: #c0c4cc;
}

.log-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.log-content-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  word-break: break-all;
}
.log-content-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.lines-label {
  font-size: 12px;
  color: #909399;
}

.no-file-selected {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.log-content-wrap {
  flex: 1;
  overflow: auto;
  padding: 0;
  display: flex;
}
.log-content {
  flex: 1;
  padding: 16px;
  margin: 0;
  font-family: 'Cascadia Code', 'Fira Code', 'JetBrains Mono', monospace;
  font-size: 12px;
  line-height: 1.7;
  color: #e0e0e0;
  background: #1e1e1e;
  border-radius: 0 0 8px 8px;
  white-space: pre-wrap;
  word-break: break-all;
  overflow-x: auto;
}
</style>
