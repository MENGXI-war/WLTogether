<template>
  <div class="sync-player">
    <div ref="playerContainer" class="player-container">
      <video ref="videoEl" controls />
    </div>
    <div class="player-controls-bar">
      <div class="sync-status">
        <el-tag v-if="clockOffset !== null" :type="syncQuality" size="small">
          同步 {{ Math.abs(clockOffset) }}ms
        </el-tag>
        <el-button text size="small" @click="$emit('sync')">手动同步</el-button>
      </div>
      <div class="participant-badges">
        <el-tag v-for="p in participants" :key="p.userId" size="small" effect="plain">
          {{ p.nickname || p.userId }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import { usePlaybackStore } from '@/stores/playback'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  clockOffset: {
    type: Number,
    default: 0
  },
  participants: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['ready', 'timeupdate', 'sync', 'play', 'pause', 'seek'])

const playbackStore = usePlaybackStore()
const playerContainer = ref(null)
const videoEl = ref(null)
let player = null
let isSeekingFromSync = false

const syncQuality = computed(() => {
  const offset = Math.abs(props.clockOffset)
  if (offset < 100) return 'success'
  if (offset < 500) return 'warning'
  return 'danger'
})

function initPlayer() {
  if (!videoEl.value) return

  player = new Plyr(videoEl.value, {
    controls: ['play-large', 'play', 'progress', 'current-time', 'mute', 'volume', 'captions', 'settings', 'pip', 'airplay', 'fullscreen'],
    ratio: '16:9',
    autoplay: false
  })

  player.on('ready', () => {
    emit('ready', player)
  })

  player.on('timeupdate', () => {
    emit('timeupdate', player.currentTime)
  })

  player.on('play', () => {
    if (!isSeekingFromSync) {
      emit('play', player.currentTime)
    }
  })

  player.on('pause', () => {
    if (!isSeekingFromSync) {
      emit('pause', player.currentTime)
    }
  })

  player.on('seeked', () => {
    if (!isSeekingFromSync) {
      emit('seek', player.currentTime)
    }
  })
}

function seekTo(time) {
  if (player && Math.abs(player.currentTime - time) > 0.5) {
    isSeekingFromSync = true
    player.currentTime = time
    setTimeout(() => { isSeekingFromSync = false }, 100)
  }
}

function adjustPlaybackRate(rate) {
  if (player) {
    player.speed = rate
  }
}

// Watch for sync corrections from playbackStore
watch(() => playbackStore.correctedPosition, (targetPos) => {
  if (!player || !playbackStore.playing) return
  const current = player.currentTime
  const deviation = targetPos - current

  if (Math.abs(deviation) > 0.5) {
    seekTo(targetPos)
  } else if (Math.abs(deviation) > 0.1) {
    // Slight speed adjustment
    adjustPlaybackRate(deviation > 0 ? 1.04 : 0.96)
    setTimeout(() => adjustPlaybackRate(1), 1000)
  }
})

// Watch for src changes
watch(() => props.src, (newSrc) => {
  if (player && newSrc) {
    player.source = {
      type: 'video',
      sources: [{ src: newSrc }]
    }
  }
})

onMounted(() => {
  initPlayer()
})

onUnmounted(() => {
  if (player) {
    player.destroy()
    player = null
  }
})

defineExpose({ seekTo, player })
</script>

<style scoped>
.sync-player {
  background: #000;
  border-radius: 8px;
  overflow: hidden;
}

.player-container {
  position: relative;
}

.player-container :deep(.plyr) {
  --plyr-color-main: #409eff;
}

.player-controls-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #1a1a2e;
  gap: 12px;
}

.sync-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.participant-badges {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
