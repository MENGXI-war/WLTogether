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
import GlobalHeader from '@/components/GlobalHeader.vue'

const route = useRoute()
const authStore = useAuthStore()
const { connect, disconnect } = useWebSocket()

const showHeader = computed(() => route.path !== '/login')

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
