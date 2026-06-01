import { ref } from 'vue'
import SparkMD5 from 'spark-md5'

export function useLocalFiles() {
  const selectedFile = ref(null)
  const fileHash = ref('')
  const hashing = ref(false)
  const hashProgress = ref(0)

  /**
   * Compute a fast file hash: SHA256 of first 1MB + last 1MB + file size.
   * Falls back to SparkMD5 for simplicity (SHA256 is available via Web Crypto).
   */
  async function computeHash(file) {
    hashing.value = true
    hashProgress.value = 0

    try {
      const chunkSize = 1024 * 1024 // 1MB
      const fileSize = file.size

      // Read first 1MB
      const firstChunk = await readChunk(file, 0, Math.min(chunkSize, fileSize))
      hashProgress.value = 30

      // Read last 1MB (or full file if smaller than 2MB)
      let lastChunk = null
      if (fileSize > chunkSize) {
        const start = Math.max(0, fileSize - chunkSize)
        lastChunk = await readChunk(file, start, fileSize - start)
      }
      hashProgress.value = 60

      // Compute hash: SparkMD5(first + last + size)
      const spark = new SparkMD5.ArrayBuffer()
      spark.append(firstChunk)
      if (lastChunk) spark.append(lastChunk)
      spark.append(new TextEncoder().encode(String(fileSize)).buffer)

      hashProgress.value = 100
      return spark.end()
    } finally {
      hashing.value = false
    }
  }

  function readChunk(file, start, length) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsArrayBuffer(file.slice(start, start + length))
    })
  }

  /**
   * Open file picker and return selected files with their hashes.
   */
  async function pickFiles(options = {}) {
    try {
      const [handle] = await window.showOpenFilePicker({
        multiple: false,
        ...options
      })
      const file = await handle.getFile()
      selectedFile.value = file

      const hash = await computeHash(file)
      fileHash.value = hash

      return { file, hash }
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('File picker error:', err)
      }
      return null
    }
  }

  /**
   * Create a blob URL for a file to be used as video/audio source.
   */
  function getFileUrl(file) {
    return URL.createObjectURL(file)
  }

  function revokeFileUrl(url) {
    URL.revokeObjectURL(url)
  }

  return {
    selectedFile,
    fileHash,
    hashing,
    hashProgress,
    pickFiles,
    computeHash,
    getFileUrl,
    revokeFileUrl
  }
}
