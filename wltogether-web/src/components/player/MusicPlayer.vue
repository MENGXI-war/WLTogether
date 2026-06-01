<template>
  <div class="music-player">
    <div class="music-main">
      <div class="album-art" :class="{ playing: isPlaying }">
        <img v-if="albumArt" :src="albumArt" alt="album art" />
        <el-icon v-else :size="80"><Headset /></el-icon>
      </div>
      <div class="track-info">
        <span class="track-title">{{ title || '未命名曲目' }}</span>
        <span class="track-artist">{{ artist || '未知艺术家' }}</span>
        <span class="track-album">{{ album || '' }}</span>
      </div>
    </div>
    <audio ref="audioEl" controls class="audio-element" />
    <div class="player-bar">
      <div class="sync-status">
        <el-tag v-if="syncOffset !== null" :type="syncQuality" size="small">
          同步 {{ Math.abs(syncOffset) }}ms
        </el-tag>
        <el-button text size="small" @click="$emit('sync')">手动同步</el-button>
      </div>
      <div class="queue-info">
        <span v-if="queueLength">{{ queueLength }} 首曲目</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import Plyr from 'plyr'
import 'plyr/dist/plyr.css'
import { Headset } from '@element-plus/icons-vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  artist: {
    type: String,
    default: ''
  },
  album: {
    type: String,
    default: ''
  },
  albumArt: {
    type: String,
    default: ''
  },
  syncOffset: {
    type: Number,
    default: null
  },
  queueLength: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['ready', 'timeupdate', 'play', 'pause', 'seek', 'sync', 'next'])

const audioEl = ref(null)
const isPlaying = ref(false)
let player = null

const syncQuality = computed(() => {
  const offset = Math.abs(props.syncOffset)
  if (offset < 100) return 'success'
  if (offset < 500) return 'warning'
  return 'danger'
})

function initPlayer() {
  if (!audioEl.value) return

  player = new Plyr(audioEl.value, {
    controls: ['play-large', 'play', 'progress', 'current-time', 'mute', 'volume']
  })

  player.on('ready', () => emit('ready', player))
  player.on('timeupdate', () => emit('timeupdate', player.currentTime))
  player.on('play', () => { isPlaying.value = true; emit('play', player.currentTime) })
  player.on('pause', () => { isPlaying.value = false; emit('pause', player.currentTime) })
  player.on('seeked', () => emit('seek', player.currentTime))
}

watch(() => props.src, (newSrc) => {
  if (player && newSrc) {
    player.source = {
      type: 'audio',
      sources: [{ src: newSrc }]
    }
  }
})

onMounted(() => initPlayer())

onUnmounted(() => {
  if (player) { player.destroy(); player = null }
})

defineExpose({ player })
</script>

<script>
import { computed } from 'vue'
</script>

<style scoped>
.music-player {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 24px;
  color: #fff;
}

.music-main {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 16px;
}

.album-art {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.album-art.playing {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255,255,255,0.4); }
  50% { box-shadow: 0 0 0 12px rgba(255,255,255,0); }
}

.album-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.track-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.track-title {
  font-size: 20px;
  font-weight: 600;
}

.track-artist {
  font-size: 14px;
  opacity: 0.8;
}

.track-album {
  font-size: 12px;
  opacity: 0.6;
}

.audio-element {
  width: 100%;
  margin-bottom: 12px;
}

.audio-element :deep(.plyr) {
  --plyr-color-main: #fff;
}

.player-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sync-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-info {
  font-size: 13px;
  opacity: 0.7;
}
</style>
