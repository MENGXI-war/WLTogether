<template>
  <div class="package-page">
    <div class="package-container">
      <h2 class="page-title">导入离线包</h2>
      <el-steps :active="activeStep" align-center style="margin-bottom: 32px">
        <el-step title="选择文件" />
        <el-step title="安全校验" />
        <el-step title="导入完成" />
      </el-steps>

      <!-- Step 1: Select .wlp file -->
      <div v-if="activeStep === 0" class="step-content">
        <div class="file-select-area" @click="onPickFile">
          <el-icon :size="48"><UploadFilled /></el-icon>
          <p v-if="!importFile">点击选择 .wlp 离线包文件</p>
          <p v-else class="selected-file">
            <strong>{{ importFile.name }}</strong><br/>
            <span class="file-size">{{ formatSize(importFile.size) }}</span>
          </p>
        </div>
        <div class="step-actions">
          <el-button type="primary" :disabled="!importFile" @click="onStartValidation">下一步</el-button>
        </div>
      </div>

      <!-- Step 2: Validation -->
      <div v-if="activeStep === 1" class="step-content">
        <div class="validation-list">
          <div v-for="check in checks" :key="check.label" class="check-item">
            <el-icon v-if="check.status === 'pass'" class="check-pass"><CircleCheckFilled /></el-icon>
            <el-icon v-else-if="check.status === 'fail'" class="check-fail"><CircleCloseFilled /></el-icon>
            <el-icon v-else class="check-pending"><Loading /></el-icon>
            <span>{{ check.label }}</span>
            <span v-if="check.status === 'fail'" class="check-error">{{ check.error }}</span>
          </div>
        </div>
        <div class="step-actions">
          <el-button @click="activeStep = 0" :disabled="validating">上一步</el-button>
          <el-button type="primary" :disabled="!allPassed" @click="activeStep = 2">下一步</el-button>
        </div>
      </div>

      <!-- Step 3: Import complete -->
      <div v-if="activeStep === 2" class="step-content">
        <el-result icon="success" title="导入成功" sub-title="文件已注册到本地库">
          <template #extra>
            <el-button type="primary" @click="router.push('/groups')">返回群组</el-button>
          </template>
        </el-result>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, CircleCheckFilled, CircleCloseFilled, Loading } from '@element-plus/icons-vue'

const router = useRouter()

const activeStep = ref(0)
const importFile = ref(null)
const validating = ref(false)

const checks = ref([
  { label: '文件格式校验 (.wlp)', status: '' },
  { label: '文件完整性校验', status: '' },
  { label: '签名验证', status: '' },
  { label: '群组绑定校验', status: '' },
  { label: '密钥有效性校验', status: '' },
  { label: '哈希匹配校验', status: '' },
  { label: '本地存储空间检查', status: '' }
])

const allPassed = computed(() => checks.value.every(c => c.status === 'pass'))

function onPickFile() {
  ElMessage.info('离线包导入功能将在后续版本实现')
}

async function onStartValidation() {
  ElMessage.info('离线包导入功能将在后续版本实现')
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}
</script>

<style scoped>
.package-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.package-container {
  max-width: 700px;
  margin: 32px auto;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
}

.step-content { min-height: 200px; }

.file-select-area {
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  color: #909399;
}

.file-select-area:hover { border-color: #409eff; }

.selected-file { margin-top: 12px; color: #303133; }
.file-size { font-size: 13px; color: #909399; }

.validation-list {
  max-width: 450px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
}

.check-pass { color: #67c23a; }
.check-fail { color: #f56c6c; }
.check-pending { color: #e6a23c; }

.check-error {
  font-size: 12px;
  color: #f56c6c;
  margin-left: auto;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
}
</style>
