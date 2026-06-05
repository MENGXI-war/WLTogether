<template>
  <div class="package-page">
    <div class="package-container">
      <h2 class="page-title">导出离线包</h2>
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
              <el-radio :value="0">永久有效</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-alert
            v-if="security.aesEncrypt && security.ttl > 0"
            title="密钥将生成 .wlk 私钥文件。请安全保存此文件，过期后将无法解密。"
            type="warning"
            show-icon
            :closable="false"
          />
          <el-alert
            v-if="security.aesEncrypt && security.ttl === 0"
            title="密钥将生成 .wlk 私钥文件（永久有效）。请安全保存此文件，离线时或离开服务器后仍可使用。"
            type="info"
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
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FolderOpened, Loading } from '@element-plus/icons-vue'
import { useLocalFiles } from '@/composables/useLocalFiles'
import { useGroupsStore } from '@/stores/groups'
import { buildWlp, buildWlk, generateAesKey, generateSigningKeyPair } from '@/utils/crypto'

const router = useRouter()
const groupsStore = useGroupsStore()
const { pickFiles } = useLocalFiles()

const activeStep = ref(0)
const selectedFile = ref(null)
const fileHash = ref('')
const generated = ref(false)
const generating = ref(false)
const wlpBytes = ref(null)
const wlkBytes = ref(null)

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

function formatTime(seconds) {
  if (seconds === 0) return '永久有效'
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  if (seconds < 86400) return `${Math.round(seconds / 3600)}小时`
  return `${Math.round(seconds / 86400)}天`
}

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}

// Start generation when entering step 3
watch(activeStep, async (step) => {
  if (step === 2 && !generated.value) {
    generating.value = true
    try {
      const groupId = groupsStore.currentGroup?.id
      let aesKey = null
      let signingKey = null

      if (security.aesEncrypt) {
        aesKey = await generateAesKey()
      }
      if (security.ed25519Sign) {
        signingKey = await generateSigningKeyPair()
      }

      wlpBytes.value = await buildWlp(selectedFile.value, {
        groupBound: security.groupBound,
        groupId: groupId ? Number(groupId) : 0,
        aesKey,
        signingKey
      })

      if (aesKey) {
        wlkBytes.value = await buildWlk(aesKey, security.ttl)
      }

      generated.value = true
    } catch (err) {
      ElMessage.error('打包失败: ' + (err.message || '未知错误'))
      activeStep.value = 1 // go back to options
    } finally {
      generating.value = false
    }
  }
})

function downloadBytes(bytes, filename) {
  const blob = new Blob([bytes], { type: 'application/octet-stream' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function onDownloadWlp() {
  if (!wlpBytes.value) return
  const name = selectedFile.value?.name || 'file'
  const base = name.replace(/\.[^.]+$/, '')
  downloadBytes(wlpBytes.value, `${base}.wlp`)
  ElMessage.success('离线包已下载')
}

function onDownloadWlk() {
  if (!wlkBytes.value) return
  const name = selectedFile.value?.name || 'file'
  const base = name.replace(/\.[^.]+$/, '')
  downloadBytes(wlkBytes.value, `${base}.wlk`)
  ElMessage.success('密钥文件已下载，请安全保存')
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
  background: var(--color-card-bg);
  border-radius: 12px;
}

.step-content {
  min-height: 260px;
}

.file-select-area {
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  color: var(--color-text-secondary);
}

.file-select-area:hover {
  border-color: #409eff;
}

.selected-file {
  margin-top: 12px;
  color: var(--color-text);
}

.file-size {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.file-hash {
  font-size: 12px;
  color: var(--color-text-secondary);
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
  color: var(--color-text-secondary);
}
</style>
