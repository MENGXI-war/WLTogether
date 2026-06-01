<template>
  <div class="music-session-page">
    <!-- Top bar -->
    <div class="session-topbar">
      <el-button text :icon="ArrowLeft" @click="onLeave">返回</el-button>
      <span class="session-title">{{ session?.fileName || '音乐同步' }}</span>
      <div class="topbar-actions">
        <el-tag :type="syncHealth" size="small">{{ syncText }}</el-tag>
        <el-button text :icon="Refresh" @click="onManualSync">同步</el-button>
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

    <!-- Body -->
    <div class="session-body">
      <div class="player-area">
        <MusicPlayer
          v-if="fileSrc"
          :src="fileSrc"
          :title="session?.fileName"
          :sync-offset="playbackStore.clockOffset"
          :queue-length="queue.length"
          ref="musicPlayerRef"
          @play="onLocalPlay"
          @pause="onLocalPause"
          @seek="onLocalSeek"
          @sync="onManualSync"
          @next="onLocalNext"
        />
        <div v-else class="no-file">
          <el-icon :size="60"><Headset /></el-icon>
          <h3>选择音频文件加入同步播放</h3>
          <el-button type="primary" @click="onSelectFile" :loading="hashing">
            <el-icon><FolderOpened /></el-icon> 选择本地文件
          </el-button>
        </div>

        <!-- Queue -->
        <div class="music-queue" v-if="queue.length > 0">
          <div class="queue-title">播放队列 ({{ queue.length }})</div>
          <div v-for="(track, idx) in queue" :key="idx" class="queue-item" :class="{ active: idx === currentTrack }">
            <span class="queue-index">{{ idx + 1 }}</span>
            <span class="queue-name">{{ track.name || track.fileName || '未命名' }}</span>
          </div>
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
import { ArrowLeft, Refresh, MoreFilled, Headset, FolderOpened } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { usePlaybackStore } from '@/stores/playback'
import { useWebSocket } from '@/composables/useWebSocket'
import { usePlaybackSync } from '@/composables/usePlaybackSync'
import { useLocalFiles } from '@/composables/useLocalFiles'
import MusicPlayer from '@/components/player/MusicPlayer.vue'
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
const { send } = useWebSocket()
const { startSync, stopSync, sendPlay, sendPause, sendSeek, sendNext } = usePlaybackSync(props.id)
const { pickFiles, getFileUrl, revokeFileUrl } = useLocalFiles()

const session = ref(null)
const fileSrc = ref('')
const hashing = ref(false)
const queue = ref([])
const currentTrack = ref(0)
const musicPlayerRef = ref(null)

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
    await sessionsApi.join(props.id)
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
      queue.value = [{ name: result.file.name, file: result.file }]
    }
  } finally {
    hashing.value = false
  }
}

function onLocalPlay() { if (isHost.value) sendPlay() }
function onLocalPause() { if (isHost.value) sendPause() }
function onLocalSeek(position) { if (isHost.value) sendSeek(position) }
function onLocalNext() {
  if (isHost.value && currentTrack.value < queue.value.length - 1) {
    currentTrack.value++
    sendNext(0)
  }
}

function onManualSync() {
  send('/sync.ping', { clientTime: Date.now() })
}

async function onCommand(cmd) {
  if (cmd === 'end') {
    try {
      await ElMessageBox.confirm('确定要结束这个会话吗？', '确认', { type: 'warning' })
      await sessionsApi.end(props.id)
      send('/session.end', { sessionId: Number(props.id) })
      ElMessage.success('会话已结束')
      onLeave()
    } catch { /* cancelled */ }
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
.music-session-page {
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
  padding: 20px;
  overflow-y: auto;
  min-width: 0;
}

.no-file {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: #fff;
  text-align: center;
  padding-top: 60px;
}

.no-file .el-icon { color: #667eea; }
.no-file h3 { font-size: 18px; font-weight: 500; }

.music-queue {
  margin-top: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
}

.queue-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

.queue-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  color: #ccc;
  font-size: 14px;
}

.queue-item.active {
  background: rgba(102, 126, 234, 0.3);
  color: #fff;
}

.queue-index {
  width: 20px;
  text-align: center;
  color: #909399;
  font-size: 12px;
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
  border-left: 1px solid #2a2a4a;
  display: flex;
  flex-direction: column;
}
</style>
