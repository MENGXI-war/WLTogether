<template>
  <div class="package-page">
    <header class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2>导出离线包</h2>
    </header>

    <div class="package-container">
      <el-steps :active="activeStep" align-center style="margin-bottom: 32px">
        <el-step title="选择文件" />
        <el-step title="安全选项" />
        <el-step title="生成导出" />
      </el-steps>

      <!-- Step 1: Select file -->
      <div v-if="activeStep === 0" class="step-content">
        <div class="file-select-area" @click="onPickFile">
          <el-icon :size="48"><FolderOpened /></el-icon>
          <p v-if="!selectedFile">点击选择要打包的媒体文件</p>
          <p v-else class="selected-file">
            <strong>{{ selectedFile.name }}</strong><br/>
            <span class="file-size">{{ formatSize(selectedFile.size) }}</span><br/>
            <span class="file-hash">Hash: {{ fileHash }}</span>
          </p>
        </div>
        <div class="step-actions">
          <el-button type="primary" :disabled="!selectedFile" @click="activeStep = 1">下一步</el-button>
        </div>
      </div>

      <!-- Step 2: Security options -->
      <div v-if="activeStep === 1" class="step-content">
        <el-form label-position="top" class="security-form">
          <el-form-item label="安全选项">
            <el-checkbox v-model="security.groupBound" disabled>
              绑定群组（必须，仅当前群组成员可解密）
            </el-checkbox>
            <el-checkbox v-model="security.aesEncrypt">
              AES-256-GCM 加密（推荐）
            </el-checkbox>
            <el-checkbox v-model="security.ed25519Sign">
              Ed25519 数字签名（防篡改）
            </el-checkbox>
          </el-form-item>
          <el-form-item label="过期时间" v-if="security.aesEncrypt">
            <el-radio-group v-model="security.ttl">
              <el-radio :value="3600">1 小时</el-radio>
              <el-radio :value="86400">24 小时</el-radio>
              <el-radio :value="604800">7 天</el-radio>
              <el-radio :value="2592000">30 天</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert
            v-if="security.aesEncrypt"
            title="密钥将生成 .wlk 私钥文件。请安全保存此文件，否则 24 小时后无法解密。"
            type="warning"
            show-icon
            :closable="false"
          />
        </el-form>
        <div class="step-actions">
          <el-button @click="activeStep = 0">上一步</el-button>
          <el-button type="primary" @click="activeStep = 2">下一步</el-button>
        </div>
      </div>

      <!-- Step 3: Generate and download -->
      <div v-if="activeStep === 2" class="step-content">
        <div class="generate-result">
          <el-result
            v-if="generated"
            icon="success"
            title="离线包生成成功"
            sub-title="请下载以下文件"
          >
            <template #extra>
              <el-button type="primary" @click="onDownloadWlp">下载 .wlp 离线包</el-button>
              <el-button v-if="security.aesEncrypt" type="warning" @click="onDownloadWlk">下载 .wlk 密钥文件</el-button>
            </template>
          </el-result>
          <div v-else class="generating">
            <el-icon :size="48" class="is-loading"><Loading /></el-icon>
            <p>正在生成离线包...</p>
          </div>
        </div>
        <div class="step-actions">
          <el-button @click="activeStep = 1">上一步</el-button>
          <el-button type="primary" :disabled="!generated" @click="router.push('/groups')">完成</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, FolderOpened, Loading } from '@element-plus/icons-vue'
import { useLocalFiles } from '@/composables/useLocalFiles'

const router = useRouter()
const { pickFiles } = useLocalFiles()

const activeStep = ref(0)
const selectedFile = ref(null)
const fileHash = ref('')
const generated = ref(false)

const security = reactive({
  groupBound: true,
  aesEncrypt: true,
  ed25519Sign: false,
  ttl: 86400
})

async function onPickFile() {
  const result = await pickFiles()
  if (result) {
    selectedFile.value = result.file
    fileHash.value = result.hash
  }
}

function onDownloadWlp() {
  ElMessage.info('离线包功能将在后续版本实现')
}

function onDownloadWlk() {
  ElMessage.info('密钥导出功能将在后续版本实现')
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024; i++
  }
  return `${size.toFixed(1)} ${units[i]}`
}
</script>

<style scoped>
.package-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h2 {
  font-size: 18px;
}

.package-container {
  max-width: 700px;
  margin: 32px auto;
  padding: 32px;
  background: #fff;
  border-radius: 12px;
}

.step-content {
  min-height: 260px;
}

.file-select-area {
  border: 2px dashed #dcdfe6;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  color: #909399;
}

.file-select-area:hover {
  border-color: #409eff;
}

.selected-file {
  margin-top: 12px;
  color: #303133;
}

.file-size {
  font-size: 13px;
  color: #909399;
}

.file-hash {
  font-size: 12px;
  color: #c0c4cc;
  font-family: monospace;
}

.security-form {
  max-width: 500px;
  margin: 0 auto;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 32px;
}

.generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: #909399;
}
</style>
