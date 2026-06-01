<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'

const authStore = useAuthStore()
const { connect, disconnect } = useWebSocket()

onMounted(async () => {
  const restored = await authStore.restoreSession()
  if (restored) {
    connect()
  }
})

onUnmounted(() => {
  disconnect()
})
</script>
