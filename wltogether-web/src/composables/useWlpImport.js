import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { parseWlp, extractPayloadData, aesDecrypt, importAesKey, parseWlk } from '@/utils/crypto'

/**
 * Composable for importing .wlp offline packages into a session.
 * Handles pick, parse, decrypt, and extract flow.
 */
export function useWlpImport() {
  const importing = ref(false)
  const progress = ref('')

  /**
   * Pick a .wlp file, optionally a .wlk key file, and extract the media file.
   * Returns an object compatible with playQueue.addFiles() or null on failure.
   */
  async function pickWlpAndExtract() {
    importing.value = true
    progress.value = '选择 .wlp 文件...'

    try {
      // Step 1: Pick .wlp file using traditional file input
      const wlpFile = await pickFileInput('.wlp,application/octet-stream')
      if (!wlpFile) {
        importing.value = false
        return null
      }

      progress.value = '解析 .wlp 文件...'
      const wlpData = new Uint8Array(await wlpFile.arrayBuffer())

      let parsed
      try {
        parsed = parseWlp(wlpData)
      } catch (e) {
        ElMessage.error('无法解析 .wlp 文件，格式可能无效')
        importing.value = false
        return null
      }

      // Step 2: Check if encrypted
      let payloadData = parsed.payload
      if (parsed.aesEncrypted) {
        // Encrypted: need key
        progress.value = '需要密钥文件，请选择 .wlk 文件...'
        const wlkFile = await pickFileInput('.wlk,application/octet-stream')
        if (!wlkFile) {
          ElMessage.warning('未提供密钥文件，无法解密')
          importing.value = false
          return null
        }

        progress.value = '解析密钥并解密...'
        const wlkData = new Uint8Array(await wlkFile.arrayBuffer())

        let keyData
        try {
          keyData = parseWlk(wlkData)
        } catch (e) {
          ElMessage.error('密钥文件无效或已过期')
          importing.value = false
          return null
        }

        try {
          const aesKey = await importAesKey(keyData.aesKey)
          payloadData = await aesDecrypt(aesKey, parsed.payload, parsed.iv)
        } catch (e) {
          ElMessage.error('解密失败，密钥可能不匹配')
          importing.value = false
          return null
        }
      }

      // Step 3: Extract media data
      progress.value = '提取媒体文件...'
      const extracted = extractPayloadData(payloadData)

      if (!extracted || !extracted.fileData || extracted.fileData.length === 0) {
        ElMessage.error('.wlp 包中未找到媒体数据')
        importing.value = false
        return null
      }

      // Step 4: Build blob URL from extracted data
      const mimeType = extracted.mimeType || 'application/octet-stream'
      const blob = new Blob([extracted.fileData], { type: mimeType })
      const blobUrl = URL.createObjectURL(blob)
      const fileName = extracted.fileName || 'imported_media'

      progress.value = ''
      importing.value = false

      return {
        entries: [{
          file: new File([blob], fileName, { type: mimeType }),
          hash: extracted.fileHash || 'wlp_import',
          blobUrl
        }],
        metadata: {
          fileName,
          fileHash: extracted.fileHash,
          mimeType,
          groupId: parsed.groupId,
          flags: parsed.flags
        }
      }
    } catch (err) {
      console.error('WLP import error:', err)
      ElMessage.error('导入离线包时发生错误: ' + (err.message || '未知错误'))
      importing.value = false
      return null
    }
  }

  return {
    importing,
    progress,
    pickWlpAndExtract
  }
}

/**
 * Helper: open file picker via <input type="file"> and return the File or null.
 */
function pickFileInput(accept) {
  return new Promise((resolve) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = accept

    let resolved = false

    input.onchange = (e) => {
      resolved = true
      const file = e.target.files?.[0]
      resolve(file || null)
    }

    const onFocus = () => {
      window.removeEventListener('focus', onFocus)
      setTimeout(() => {
        if (!resolved) resolve(null)
      }, 300)
    }
    window.addEventListener('focus', onFocus)

    input.click()
  })
}
