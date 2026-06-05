import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

let nextId = 1
function generateId() {
  return `qitem_${Date.now()}_${nextId++}`
}

export const usePlayQueueStore = defineStore('playQueue', () => {
  const items = ref([])
  const currentIndex = ref(-1)

  // Getters
  const currentItem = computed(() => {
    if (currentIndex.value >= 0 && currentIndex.value < items.value.length) {
      return items.value[currentIndex.value]
    }
    return null
  })

  const queueLength = computed(() => items.value.length)

  // Actions
  function addFiles(fileList) {
    // fileList: Array<{ file: File, hash: string, blobUrl: string }>
    for (const entry of fileList) {
      items.value.push({
        id: generateId(),
        name: entry.file.name,
        file: entry.file,
        blobUrl: entry.blobUrl,
        hash: entry.hash,
        duration: 0
      })
    }
    // Auto-select first item if queue was empty
    if (currentIndex.value < 0 && items.value.length > 0) {
      currentIndex.value = 0
    }
  }

  function removeItem(id) {
    const idx = items.value.findIndex(item => item.id === id)
    if (idx < 0) return

    // Revoke blob URL to free memory
    const item = items.value[idx]
    if (item.blobUrl) {
      URL.revokeObjectURL(item.blobUrl)
    }

    items.value.splice(idx, 1)

    // Adjust current index
    if (items.value.length === 0) {
      currentIndex.value = -1
    } else if (idx < currentIndex.value) {
      currentIndex.value--
    } else if (idx === currentIndex.value) {
      // Stay at same index (now points to next item) or clamp
      if (currentIndex.value >= items.value.length) {
        currentIndex.value = items.value.length - 1
      }
    }
  }

  function setCurrentIndex(index) {
    if (index >= 0 && index < items.value.length) {
      currentIndex.value = index
    }
  }

  function moveItem(fromIndex, toIndex) {
    if (fromIndex < 0 || fromIndex >= items.value.length) return
    if (toIndex < 0 || toIndex >= items.value.length) return
    if (fromIndex === toIndex) return

    const [moved] = items.value.splice(fromIndex, 1)
    items.value.splice(toIndex, 0, moved)

    // Adjust currentIndex
    if (currentIndex.value === fromIndex) {
      currentIndex.value = toIndex
    } else if (fromIndex < currentIndex.value && toIndex >= currentIndex.value) {
      currentIndex.value--
    } else if (fromIndex > currentIndex.value && toIndex <= currentIndex.value) {
      currentIndex.value++
    }
  }

  function next() {
    if (items.value.length === 0) return -1
    currentIndex.value = (currentIndex.value + 1) % items.value.length
    return currentIndex.value
  }

  function prev() {
    if (items.value.length === 0) return -1
    if (currentIndex.value <= 0) {
      currentIndex.value = items.value.length - 1
    } else {
      currentIndex.value--
    }
    return currentIndex.value
  }

  function setDuration(index, duration) {
    if (index >= 0 && index < items.value.length) {
      items.value[index].duration = duration
    }
  }

  function clear() {
    for (const item of items.value) {
      if (item.blobUrl) URL.revokeObjectURL(item.blobUrl)
    }
    items.value = []
    currentIndex.value = -1
  }

  return {
    items,
    currentIndex,
    currentItem,
    queueLength,
    addFiles,
    removeItem,
    setCurrentIndex,
    moveItem,
    next,
    prev,
    setDuration,
    clear
  }
})
