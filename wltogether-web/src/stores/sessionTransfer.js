import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Temporary transfer store — passes selected file from session creation dialog
 * to the session view. Consumed once on session view mount, then cleared.
 * Survives SPA navigation but NOT page refresh (by design — blob URLs are ephemeral).
 */
export const useSessionTransferStore = defineStore('sessionTransfer', () => {
  const pendingFile = ref(null)
  // { file: File, hash: string, blobUrl: string }

  function setPendingFile(file, hash, blobUrl) {
    pendingFile.value = { file, hash, blobUrl }
  }

  /**
   * Consume the pending file — returns it and clears the store.
   * Returns null if no pending file.
   */
  function consumePendingFile() {
    const data = pendingFile.value
    pendingFile.value = null
    return data
  }

  function clearPendingFile() {
    pendingFile.value = null
  }

  return {
    pendingFile,
    setPendingFile,
    consumePendingFile,
    clearPendingFile
  }
})
