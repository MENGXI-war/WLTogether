<template>
  <div class="video-session-page">
    <!-- Top bar -->
    <div class="session-topbar">
      <el-button text :icon="ArrowLeft" @click="onLeave">返回</el-button>
      <span class="session-title">{{ session?.fileName || '视频同步' }}</span>
      <div class="topbar-actions">
        <el-tag :type="syncHealth" size="small">{{ syncText }}</el-tag>
        <el-button text :icon="Refresh" @click="onManualSync" :loading="syncing">同步</el-button>
        <el-dropdown @command="onCommand">
          <el-button text :icon="MoreFilled" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="end" :disabled="!isHost">结束会话</el-dropdown-item>
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
      </div>

      <div class="sidebar-right">
        <ChatPanel :groupId="session?.groupId" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh, MoreFilled, VideoCamera, FolderOpened } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { usePlaybackStore } from '@/stores/playback'
import { useWebSocket } from '@/composables/useWebSocket'
import { usePlaybackSync } from '@/composables/usePlaybackSync'
import { useLocalFiles } from '@/composables/useLocalFiles'
import SyncPlayer from '@/components/player/SyncPlayer.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
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
const { send, subscribe } = useWebSocket()
const { startSync, stopSync, sendPlay, sendPause, sendSeek } = usePlaybackSync(props.id)
const { pickFiles, getFileUrl, revokeFileUrl } = useLocalFiles()

const session = ref(null)
const fileSrc = ref('')
const hashing = ref(false)
const syncing = ref(false)
const syncPlayerRef = ref(null)

const isHost = computed(() => session.value?.hostId === authStore.user?.id)

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
    // Join the session
    await sessionsApi.join(props.id)
    // Start sync
    startSync()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '无法加载会话')
    router.push('/groups')
  }
}

async function onSelectFile() {
  hashing.value = true
  try {
    const result = await pickFiles()
    if (result) {
      fileSrc.value = getFileUrl(result.file)
    }
  } finally {
    hashing.value = false
  }
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
  // Force a clock sync ping
  send('/sync.ping', { clientTime: Date.now() })
  // Also seek to corrected position
  const target = playbackStore.correctedPosition
  syncPlayerRef.value?.seekTo(target)
  setTimeout(() => { syncing.value = false }, 1000)
}

async function onCommand(cmd) {
  if (cmd === 'end') {
    try {
      await ElMessageBox.confirm('确定要结束这个会话吗？', '确认', {
        type: 'warning'
      })
      await sessionsApi.end(props.id)
      send('/session.end', { sessionId: Number(props.id) })
      ElMessage.success('会话已结束')
      onLeave()
    } catch {
      // cancelled
    }
  }
}

function onLeave() {
  stopSync()
  send('/session.leave', { sessionId: Number(props.id) })
  if (fileSrc.value) revokeFileUrl(fileSrc.value)
  router.back()
}

onMounted(() => init())
onUnmounted(() => {
  stopSync()
  if (fileSrc.value) revokeFileUrl(fileSrc.value)
})
</script>

<style scoped>
.video-session-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #1a1a2e;
}

.session-topbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  height: 48px;
  background: #16213e;
  color: #fff;
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
  align-items: center;
  justify-content: center;
  padding: 20px;
  min-width: 0;
}

.no-file {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #fff;
  text-align: center;
}

.no-file .el-icon {
  color: #667eea;
}

.no-file h3 {
  font-size: 18px;
  font-weight: 500;
}

.hint {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
}

.sidebar-right {
  width: 320px;
  flex-shrink: 0;
  border-left: 1px solid #2a2a4a;
  display: flex;
  flex-direction: column;
}
</style>
