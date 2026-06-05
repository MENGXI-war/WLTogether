import { ref } from 'vue'
import SparkMD5 from 'spark-md5'

export function useLocalFiles() {
  const selectedFile = ref(null)
  const fileHash = ref('')
  const hashing = ref(false)
  const hashProgress = ref(0)

  async function computeHash(file) {
    hashing.value = true
    hashProgress.value = 0

    try {
      const chunkSize = 1024 * 1024 // 1MB
      const fileSize = file.size

      const firstChunk = await readChunk(file, 0, Math.min(chunkSize, fileSize))
      hashProgress.value = 30

      let lastChunk = null
      if (fileSize > chunkSize) {
        const start = Math.max(0, fileSize - chunkSize)
        lastChunk = await readChunk(file, start, fileSize - start)
      }
      hashProgress.value = 60

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

  // Fallback helper: create <input type="file"> and return promise
  function inputFileFallback({ multiple, accept }) {
    return new Promise((resolve) => {
      const input = document.createElement('input')
      input.type = 'file'
      input.multiple = !!multiple
      if (accept) input.accept = accept

      let resolved = false

      input.onchange = async (e) => {
        resolved = true
        const fileList = e.target.files
        if (!fileList || fileList.length === 0) {
          resolve(multiple ? { files: [], hashes: [] } : null)
          return
        }

        if (multiple) {
          const files = []
          const hashes = []
          for (const file of fileList) {
            files.push(file)
            hashes.push(await computeHash(file))
          }
          selectedFile.value = files[0]
          fileHash.value = hashes[0]
          resolve({ files, hashes })
        } else {
          const file = fileList[0]
          selectedFile.value = file
          const hash = await computeHash(file)
          fileHash.value = hash
          resolve({ file, hash })
        }
      }

      // Detect cancellation via window refocus
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

  /**
   * Open file picker for a single file. Returns { file, hash } or null.
   */
  async function pickFiles(options = {}) {
    if (window.showOpenFilePicker) {
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

    return inputFileFallback({ multiple: false, accept: options.accept })
  }

  /**
   * Pick multiple files at once. Returns { files: File[], hashes: string[] } or null.
   */
  async function pickMultipleFiles(options = {}) {
    if (window.showOpenFilePicker) {
      try {
        const handles = await window.showOpenFilePicker({
          multiple: true,
          ...options
        })
        const files = []
        const hashes = []
        for (const handle of handles) {
          const file = await handle.getFile()
          files.push(file)
          hashes.push(await computeHash(file))
        }
        if (files.length === 0) return null
        selectedFile.value = files[0]
        fileHash.value = hashes[0]
        return { files, hashes }
      } catch (err) {
        if (err.name !== 'AbortError') {
          console.error('Multiple file picker error:', err)
        }
        return null
      }
    }

    return inputFileFallback({ multiple: true, accept: options.accept })
  }

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
    pickMultipleFiles,
    computeHash,
    getFileUrl,
    revokeFileUrl
  }
}
