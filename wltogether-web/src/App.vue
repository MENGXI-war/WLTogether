<template>
  <GlobalHeader v-if="showHeader" />
  <div :class="['main-content', { 'has-header': showHeader }]">
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import { useTheme } from '@/composables/useTheme'
import GlobalHeader from '@/components/GlobalHeader.vue'

// Initialize theme synchronously before render to prevent flash
const { isDark } = useTheme()

const route = useRoute()
const authStore = useAuthStore()
const { connect, disconnect } = useWebSocket()

const showHeader = computed(() => {
  const name = route.name
  return name !== 'Login' && name !== 'Landing' && name !== 'Docs'
    && !route.path.startsWith('/admin')
})

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

<style>
.main-content.has-header {
  padding-top: 52px;
}
</style>
