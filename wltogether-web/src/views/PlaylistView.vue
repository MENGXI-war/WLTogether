<template>
  <div class="playlist-page">
    <div class="playlist-container">
      <div class="playlist-toolbar">
        <h2>歌单管理</h2>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">新建歌单</el-button>
      </div>
      <el-empty v-if="playlists.length === 0" description="还没有歌单" />

      <el-card v-for="pl in playlists" :key="pl.id" class="playlist-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>{{ pl.name }}</span>
            <el-button text type="danger" :icon="Delete" @click="onDelete(pl.id)" />
          </div>
        </template>
        <div class="track-list">
          <div v-for="(track, idx) in pl.tracks || []" :key="idx" class="track-item">
            <span class="track-index">{{ idx + 1 }}</span>
            <span class="track-name">{{ track.fileName || '未命名' }}</span>
            <span class="track-duration">{{ formatDuration(track.durationSeconds) }}</span>
          </div>
          <el-empty v-if="!pl.tracks?.length" description="暂无曲目" :image-size="40" />
        </div>
      </el-card>
    </div>

    <!-- Create dialog -->
    <el-dialog v-model="showCreateDialog" title="新建歌单" width="400px">
      <el-form>
        <el-form-item label="歌单名称">
          <el-input v-model="newPlaylistName" placeholder="输入歌单名称" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

const router = useRouter()

const playlists = ref([])
const showCreateDialog = ref(false)
const newPlaylistName = ref('')

function formatDuration(seconds) {
  if (!seconds) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function onCreate() {
  ElMessage.info('歌单功能将在后续版本实现')
  showCreateDialog.value = false
}

function onDelete(id) {
  ElMessage.info('歌单功能将在后续版本实现')
}
</script>

<style scoped>
.playlist-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.playlist-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.playlist-toolbar h2 {
  font-size: 18px;
  font-weight: 600;
}

.playlist-container {
  max-width: 700px;
  margin: 24px auto;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 14px;
}

.track-item:hover {
  background: #f5f7fa;
}

.track-index {
  width: 24px;
  text-align: center;
  color: #909399;
}

.track-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-duration {
  color: #909399;
  font-size: 13px;
  font-family: monospace;
}
</style>
