<template>
  <el-dialog v-model="visible" title="导入离线包" width="460px" :close-on-click-modal="false" @close="onCancel">
    <!-- Step 0: Pick .wlp -->
    <div v-if="step === 0" class="wlp-step">
      <div class="wlp-drop-zone" @click="onPickWlp">
        <el-icon :size="48"><UploadFilled /></el-icon>
        <p v-if="!wlpFile">点击选择 .wlp 离线包文件</p>
        <p v-else class="selected-name">{{ wlpFile.name }}</p>
        <span v-if="wlpFile" class="file-size">{{ formatSize(wlpFile.size) }}</span>
      </div>

      <div v-if="wlpInfo" class="wlp-info-card">
        <div class="info-row"><span class="info-label">文件名</span><span>{{ wlpInfo.fileName }}</span></div>
        <div class="info-row"><span class="info-label">加密状态</span>
          <el-tag :type="wlpInfo.aesEncrypted ? 'warning' : 'success'" size="small">
            {{ wlpInfo.aesEncrypted ? 'AES-256-GCM 加密' : '未加密' }}
          </el-tag>
        </div>
        <div v-if="wlpInfo.signed" class="info-row"><span class="info-label">签名</span>
          <el-tag type="info" size="small">Ed25519</el-tag>
        </div>
        <div v-if="wlpInfo.groupBound" class="info-row"><span class="info-label">群组绑定</span>
          <el-tag type="info" size="small">ID: {{ wlpInfo.groupId }}</el-tag>
        </div>
      </div>
    </div>

    <!-- Step 1: Pick .wlk if encrypted -->
    <div v-if="step === 1" class="wlp-step">
      <el-alert
        title="此离线包已加密，需要 .wlk 密钥文件才能解密"
        type="warning" show-icon :closable="false" style="margin-bottom: 16px"
      />

      <div class="wlp-drop-zone" @click="onPickWlk">
        <el-icon :size="48"><Key /></el-icon>
        <p v-if="!wlkFile">点击选择 .wlk 密钥文件</p>
        <p v-else class="selected-name">{{ wlkFile.name }}</p>
      </div>

      <div v-if="wlkError" style="margin-top: 12px">
        <el-alert :title="wlkError" type="error" show-icon :closable="false" />
      </div>

      <div v-if="wlkInfo" class="wlp-info-card" style="margin-top: 12px">
        <div class="info-row"><span class="info-label">密钥状态</span>
          <el-tag :type="wlkInfo.permanent ? 'success' : 'info'" size="small">
            {{ wlkInfo.permanent ? '永久有效' : '有时效限制' }}
          </el-tag>
        </div>
        <div v-if="!wlkInfo.permanent && wlkInfo.expiresAt" class="info-row">
          <span class="info-label">过期时间</span>
          <span>{{ formatExpiry(wlkInfo.expiresAt) }}</span>
        </div>
      </div>
    </div>

    <!-- Step 2: Success, ready to import -->
    <div v-if="step === 2" class="wlp-step">
      <el-result icon="success" title="离线包解析成功" :sub-title="resultText">
        <template #extra>
          <div class="wlp-info-card">
            <div class="info-row"><span class="info-label">文件名</span><span>{{ resultEntry?.name }}</span></div>
            <div class="info-row"><span class="info-label">大小</span><span>{{ formatSize(resultSize) }}</span></div>
          </div>
        </template>
      </el-result>
    </div>

    <template #footer>
      <el-button @click="onCancel">取消</el-button>
      <el-button v-if="step === 0 && wlpInfo && !wlpInfo.aesEncrypted" type="primary" @click="onConfirmDirect">
        导入到播放队列
      </el-button>
      <el-button v-if="step === 0 && wlpInfo?.aesEncrypted" type="primary" @click="step = 1">
        选择密钥文件
      </el-button>
      <el-button v-if="step === 1" @click="step = 0">返回</el-button>
      <el-button v-if="step === 1 && wlkFile && !wlkError" type="primary" @click="onDecrypt" :loading="decrypting">
        解密
      </el-button>
      <el-button v-if="step === 2" type="primary" @click="onConfirm">
        导入到播放队列
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Key } from '@element-plus/icons-vue'
import { parseWlp, extractPayloadData, aesDecrypt, parseWlk } from '@/utils/crypto'

const emit = defineEmits(['import'])

const visible = ref(false)
const step = ref(0)
const wlpFile = ref(null)
const wlpInfo = ref(null)
const wlkFile = ref(null)
const wlkInfo = ref(null)
const wlkError = ref('')
const decrypting = ref(false)
const resultEntry = ref(null)
const resultSize = ref(0)
let resultBlobUrl = ''

const resultText = computed(() => {
  if (!resultEntry.value) return ''
  return `文件已解密，可导入到当前会话的播放队列`
})

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}

function formatExpiry(ts) {
  return new Date(ts).toLocaleString()
}

function open() {
  step.value = 0
  wlpFile.value = null
  wlpInfo.value = null
  wlkFile.value = null
  wlkInfo.value = null
  wlkError.value = ''
  resultEntry.value = null
  resultBlobUrl = ''
  visible.value = true
}

function onCancel() {
  visible.value = false
}

async function onPickWlp() {
  const file = await pickFileInput('.wlp,application/octet-stream')
  if (!file) return
  wlpFile.value = file

  try {
    const data = new Uint8Array(await file.arrayBuffer())
    const parsed = parseWlp(data)
    wlpInfo.value = parsed
    wlkError.value = ''
  } catch (e) {
    ElMessage.error('无法解析 .wlp 文件: ' + e.message)
    wlpFile.value = null
    wlpInfo.value = null
  }
}

async function onPickWlk() {
  const file = await pickFileInput('.wlk,application/octet-stream')
  if (!file) return
  wlkFile.value = file
  wlkError.value = ''
  wlkInfo.value = null

  try {
    const data = new Uint8Array(await file.arrayBuffer())
    const keyData = await parseWlk(data)
    wlkInfo.value = keyData
  } catch (e) {
    wlkError.value = e.message || '密钥文件无效'
  }
}

async function onDecrypt() {
  if (!wlpInfo.value || !wlkFile.value) return
  decrypting.value = true
  wlkError.value = ''

  try {
    const wlkData = new Uint8Array(await wlkFile.value.arrayBuffer())
    const keyData = await parseWlk(wlkData)
    const aesKey = keyData.aesKey  // parseWlk already returns an imported CryptoKey

    // Use iv/ciphertext from parsed wlpInfo (already extracted by parseWlp)
    const iv = wlpInfo.value.iv
    const ciphertext = wlpInfo.value.ciphertext

    if (!iv || !ciphertext) {
      throw new Error('.wlp 文件解析异常，缺少加密数据')
    }

    const payloadData = await aesDecrypt(aesKey, ciphertext, iv)
    const extracted = extractPayloadData(new Uint8Array(payloadData))

    if (!extracted || !extracted.fileData || extracted.fileData.length === 0) {
      throw new Error('解密后未找到媒体数据')
    }

    const mimeType = guessMimeType(extracted.fileName)
    const blob = new Blob([extracted.fileData], { type: mimeType })
    resultBlobUrl = URL.createObjectURL(blob)
    resultEntry.value = {
      file: new File([blob], extracted.fileName, { type: mimeType }),
      hash: 'wlp_' + Date.now(),
      blobUrl: resultBlobUrl
    }
    resultSize.value = extracted.fileData.length
    step.value = 2
  } catch (e) {
    wlkError.value = '解密失败: ' + (e.message || '密钥不匹配')
  } finally {
    decrypting.value = false
  }
}

function onConfirmDirect() {
  // Unencrypted: extract directly
  if (!wlpInfo.value || !wlpFile.value) return
  try {
    const payloadData = wlpInfo.value.payload
    const extracted = extractPayloadData(payloadData)

    const mimeType = guessMimeType(extracted.fileName)
    const blob = new Blob([extracted.fileData], { type: mimeType })
    resultBlobUrl = URL.createObjectURL(blob)
    resultEntry.value = {
      file: new File([blob], extracted.fileName, { type: mimeType }),
      hash: 'wlp_' + Date.now(),
      blobUrl: resultBlobUrl
    }
    resultSize.value = extracted.fileData.length
    step.value = 2
    onConfirm()
  } catch (e) {
    ElMessage.error('提取文件失败: ' + e.message)
  }
}

function onConfirm() {
  if (resultEntry.value) {
    emit('import', {
      entries: [resultEntry.value],
      metadata: {
        fileName: resultEntry.value.file.name,
        groupId: wlpInfo.value?.groupId
      }
    })
  }
  visible.value = false
}

function guessMimeType(name) {
  const ext = (name || '').split('.').pop()?.toLowerCase()
  const map = {
    mp4: 'video/mp4', mkv: 'video/x-matroska', webm: 'video/webm',
    avi: 'video/x-msvideo', mov: 'video/quicktime',
    mp3: 'audio/mpeg', flac: 'audio/flac', wav: 'audio/wav',
    ogg: 'audio/ogg', aac: 'audio/aac', m4a: 'audio/mp4',
    wma: 'audio/x-ms-wma', opus: 'audio/opus'
  }
  return map[ext] || 'application/octet-stream'
}

function pickFileInput(accept) {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = accept
    let resolved = false
    input.onchange = (e) => { resolved = true; resolve(e.target.files?.[0] || null) }
    const onFocus = () => {
      window.removeEventListener('focus', onFocus)
      setTimeout(() => { if (!resolved) resolve(null) }, 300)
    }
    window.addEventListener('focus', onFocus)
    input.click()
  })
}

defineExpose({ open })
</script>

<style scoped>
.wlp-step {
  min-height: 180px;
}

.wlp-drop-zone {
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  color: var(--color-text-secondary);
}

.wlp-drop-zone:hover {
  border-color: var(--color-primary);
  background: rgba(64, 158, 255, 0.04);
}

.selected-name {
  margin-top: 8px;
  color: var(--color-text);
  font-weight: 500;
  word-break: break-all;
}

.file-size {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.wlp-info-card {
  margin-top: 16px;
  background: var(--color-bg);
  border-radius: 8px;
  padding: 12px 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  font-size: 14px;
  color: var(--color-text);
}

.info-row + .info-row {
  border-top: 1px solid var(--color-border);
}

.info-label {
  color: var(--color-text-secondary);
}
</style>
