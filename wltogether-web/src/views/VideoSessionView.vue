<template>
  <div class="video-session-page">
    <!-- Floating top bar -->
    <div class="session-topbar">
      <el-button text :icon="ArrowLeft" @click="onLeave" class="topbar-btn">返回</el-button>
      <span class="session-title">{{ session?.fileName || '视频同步' }}</span>
      <div class="topbar-actions">
        <el-tag :type="syncHealth" size="small" class="sync-tag">{{ syncText }}</el-tag>
        <el-button text :icon="Refresh" @click="onManualSync" :loading="syncing" class="topbar-btn">同步</el-button>
        <el-dropdown @command="onCommand">
          <el-button text :icon="MoreFilled" class="topbar-btn" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="addFile"><el-icon><FolderOpened /></el-icon> 添加本地文件</el-dropdown-item>
              <el-dropdown-item command="importWlp"><el-icon><UploadFilled /></el-icon> 导入离线包</el-dropdown-item>
              <el-dropdown-item command="toggleP2p">
                <el-icon><component :is="p2pEnabled ? Connection : Link" /></el-icon>
                {{ p2pEnabled ? 'P2P 已启用' : 'P2P 已禁用' }}
              </el-dropdown-item>
              <el-dropdown-item divided command="end" :disabled="!isHost">结束会话</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Main area: player + sidebar -->
    <div class="session-body">
      <div class="player-area">
        <SyncPlayer
          v-if="fileSrc"
          :src="fileSrc"
          :clock-offset="playbackStore.clockOffset"
          :participants="playbackStore.participants"
          ref="syncPlayerRef"
          @play="onLocalPlay"
          @pause="onLocalPause"
          @seek="onLocalSeek"
          @sync="onManualSync"
          @ready="onPlayerReady"
        />
        <div v-else class="no-file">
          <el-icon :size="60"><VideoCamera /></el-icon>
          <h3>选择文件加入同步播放</h3>
          <el-button type="primary" @click="onSelectFile" :loading="hashing">
            <el-icon><FolderOpened /></el-icon> 选择本地文件
          </el-button>
          <span class="hint" v-if="session?.fileHash">
            需要匹配哈希: {{ session.fileHash?.substring(0, 16) }}...
          </span>
        </div>

        <!-- Queue for video -->
        <div class="video-queue" v-if="playQueue.items.length > 1">
          <div class="queue-header">
            <span class="queue-title">播放列表 ({{ playQueue.items.length }})</span>
            <el-button size="small" :icon="Plus" @click="onSelectFile">添加文件</el-button>
          </div>
          <div
            v-for="(item, idx) in playQueue.items"
            :key="item.id"
            class="queue-item"
            :class="{ active: idx === playQueue.currentIndex }"
            @click="onSelectTrack(idx)"
          >
            <span class="queue-index">{{ idx + 1 }}</span>
            <span class="queue-name">{{ item.name }}</span>
            <el-button text size="small" :icon="Top" @click.stop="onMoveTrack(idx, -1)" :disabled="idx === 0" title="上移" />
            <el-button text size="small" :icon="Bottom" @click.stop="onMoveTrack(idx, 1)" :disabled="idx === playQueue.items.length - 1" title="下移" />
            <el-button text size="small" type="danger" :icon="Delete" @click.stop="onRemoveTrack(item.id)" title="移除" />
          </div>
        </div>
      </div>

      <div class="sidebar-right">
        <ChatPanel v-if="session?.groupId" :groupId="session.groupId" />
      </div>
    </div>

    <!-- WLP Import Dialog -->
    <WlpImportDialog ref="wlpImportDialog" @import="onWlpImported" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh, MoreFilled, VideoCamera, FolderOpened, UploadFilled, Plus, Top, Bottom, Delete, Connection, Link } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { usePlaybackStore } from '@/stores/playback'
import { usePlayQueueStore } from '@/stores/playQueue'
import { useWebSocket } from '@/composables/useWebSocket'
import { usePlaybackSync } from '@/composables/usePlaybackSync'
import { useLocalFiles } from '@/composables/useLocalFiles'
import { useSessionTransferStore } from '@/stores/sessionTransfer'
import SyncPlayer from '@/components/player/SyncPlayer.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import WlpImportDialog from '@/components/WlpImportDialog.vue'
import sessionsApi from '@/api/sessions'

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const authStore = useAuthStore()
const playbackStore = usePlaybackStore()
const playQueue = usePlayQueueStore()
const { send, subscribe } = useWebSocket()
const { startSync, stopSync, sendPlay, sendPause, sendSeek } = usePlaybackSync(props.id)
const { pickMultipleFiles, getFileUrl, revokeFileUrl } = useLocalFiles()
const sessionTransferStore = useSessionTransferStore()

const PLAYBACK_KEY_PREFIX = 'wltogether:playback:'

const session = ref(null)
const fileSrc = ref('')
const hashing = ref(false)
const syncing = ref(false)
const syncPlayerRef = ref(null)
const wlpImportDialog = ref(null)
let playbackSaveTimer = null

const isHost = computed(() => session.value?.hostId === authStore.user?.id)
const p2pEnabled = ref(localStorage.getItem('p2pEnabled') !== 'false')

const syncHealth = computed(() => {
  const offset = Math.abs(playbackStore.clockOffset)
  if (offset < 100) return 'success'
  if (offset < 500) return 'warning'
  return 'danger'
})

const syncText = computed(() => {
  const offset = Math.abs(playbackStore.clockOffset)
  if (offset < 100) return '同步良好'
  if (offset < 500) return `偏差 ${offset}ms`
  return `偏差 ${offset}ms`
})

async function init() {
  try {
    session.value = await sessionsApi.get(props.id)
    await sessionsApi.join(props.id)

    // Check for pending file from session creation dialog
    const pending = sessionTransferStore.consumePendingFile()
    if (pending) {
      playQueue.addFiles([{ file: pending.file, hash: pending.hash, blobUrl: pending.blobUrl }])
      fileSrc.value = pending.blobUrl
    }

    startSync()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '无法加载会话')
    router.push('/groups')
  }
}

// Switch source when current queue index changes
watch(() => playQueue.currentIndex, (newIdx) => {
  if (newIdx >= 0 && playQueue.items[newIdx]) {
    const item = playQueue.items[newIdx]
    if (item.blobUrl && item.blobUrl !== fileSrc.value) {
      fileSrc.value = item.blobUrl
    }
  }
})

async function onSelectFile() {
  hashing.value = true
  try {
    const result = await pickMultipleFiles()
    if (result && result.files.length > 0) {
      const entries = result.files.map((file, i) => ({
        file,
        hash: result.hashes[i],
        blobUrl: getFileUrl(file)
      }))
      playQueue.addFiles(entries)
      const cur = playQueue.currentItem
      if (cur) fileSrc.value = cur.blobUrl
    }
  } finally {
    hashing.value = false
  }
}

function onSelectTrack(index) {
  playQueue.setCurrentIndex(index)
}

function onMoveTrack(index, direction) {
  playQueue.moveItem(index, index + direction)
}

function onRemoveTrack(id) {
  const wasCurrent = playQueue.items[playQueue.currentIndex]?.id === id
  playQueue.removeItem(id)
  if (wasCurrent) {
    const cur = playQueue.currentItem
    fileSrc.value = cur ? cur.blobUrl : ''
  }
}

function savePlaybackPosition() {
  const player = syncPlayerRef.value?.player
  if (player && player.currentTime && fileSrc.value) {
    const pos = Math.floor(player.currentTime)
    localStorage.setItem(PLAYBACK_KEY_PREFIX + props.id, pos)
  }
}

function onPlayerReady(player) {
  // Restore saved playback position
  const saved = localStorage.getItem(PLAYBACK_KEY_PREFIX + props.id)
  if (saved) {
    const pos = Number(saved)
    if (pos > 0 && player.duration && pos < player.duration) {
      player.currentTime = pos
    }
    localStorage.removeItem(PLAYBACK_KEY_PREFIX + props.id)
  }
  // Start periodic save
  if (playbackSaveTimer) clearInterval(playbackSaveTimer)
  playbackSaveTimer = setInterval(savePlaybackPosition, 5000)
}

function onLocalPlay(position) {
  if (isHost.value || playbackStore.playMode === 'SHARED_CONTROL') {
    sendPlay()
  }
}

function onLocalPause(position) {
  if (isHost.value || playbackStore.playMode === 'SHARED_CONTROL') {
    sendPause()
  }
}

function onLocalSeek(position) {
  if (isHost.value || playbackStore.playMode === 'SHARED_CONTROL') {
    sendSeek(position)
  }
}

function onManualSync() {
  syncing.value = true
  send('/sync.ping', { clientTime: Date.now() })
  const target = playbackStore.correctedPosition
  syncPlayerRef.value?.seekTo(target)
  setTimeout(() => { syncing.value = false }, 1000)
}

async function onCommand(cmd) {
  if (cmd === 'addFile') {
    onSelectFile()
  } else if (cmd === 'importWlp') {
    wlpImportDialog.value?.open()
  } else if (cmd === 'toggleP2p') {
    p2pEnabled.value = !p2pEnabled.value
    localStorage.setItem('p2pEnabled', p2pEnabled.value)
    ElMessage.info(p2pEnabled.value ? 'P2P 传输已启用' : 'P2P 传输已禁用')
  } else if (cmd === 'end') {
    try {
      await ElMessageBox.confirm('确定要结束这个会话吗？', '确认', { type: 'warning' })
      await sessionsApi.end(props.id)
      send('/session.end', { sessionId: Number(props.id) })
      ElMessage.success('会话已结束')
      onLeave()
    } catch { /* cancelled */ }
  }
}

function onWlpImported(result) {
  if (result && result.entries.length > 0) {
    playQueue.addFiles(result.entries)
    const cur = playQueue.currentItem
    if (cur) fileSrc.value = cur.blobUrl
    ElMessage.success(`已导入: ${result.metadata.fileName}`)
  }
}

function onLeave() {
  stopSync()
  send('/session.leave', { sessionId: Number(props.id) })
  if (fileSrc.value) revokeFileUrl(fileSrc.value)
  playQueue.clear()
  router.back()
}

onMounted(() => init())
onUnmounted(() => {
  if (playbackSaveTimer) clearInterval(playbackSaveTimer)
  savePlaybackPosition()
  stopSync()
  if (fileSrc.value) revokeFileUrl(fileSrc.value)
})
</script>

<style scoped>
.video-session-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.session-topbar {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border-radius: 14px;
  background: transparent;
  color: var(--color-text);
  z-index: 20;
  pointer-events: none;
  white-space: nowrap;
}

.session-topbar > * {
  pointer-events: auto;
}

.topbar-btn {
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
  background: rgba(255, 255, 255, 0.18) !important;
  border-radius: 10px !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  transition: background 0.2s;
}

html.dark .topbar-btn {
  background: rgba(30, 30, 50, 0.55) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

.topbar-btn:hover {
  background: rgba(255, 255, 255, 0.3) !important;
}

html.dark .topbar-btn:hover {
  background: rgba(40, 40, 70, 0.7) !important;
}

.sync-tag {
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
}

.session-title {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.session-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.player-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  padding-top: 60px;
  min-width: 0;
  gap: 16px;
  position: relative;
}

.no-file {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--color-text);
  text-align: center;
}

.no-file .el-icon { color: var(--color-primary); }
.no-file h3 { font-size: 18px; font-weight: 500; }

.hint {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-family: monospace;
}

.video-queue {
  width: 100%;
  max-width: 500px;
  background: var(--color-card-bg);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid var(--color-border);
}

.queue-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.queue-title {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 6px;
  color: var(--color-text);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.queue-item:hover {
  background: var(--color-session-hover);
}

.queue-item.active {
  background: var(--color-session-active-bg);
  color: var(--color-session-active-text);
}

.queue-index {
  width: 24px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 12px;
  flex-shrink: 0;
}

.queue-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-right {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
}
</style>
