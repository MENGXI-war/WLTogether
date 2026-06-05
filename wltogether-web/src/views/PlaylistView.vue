<template>
  <div class="playlist-page">
    <div class="playlist-container">
      <!-- No group selected -->
      <el-empty v-if="!groupId" description="请先选择一个群组" />

      <template v-else>
        <!-- Toolbar -->
        <div class="playlist-toolbar">
          <h2>歌单管理</h2>
          <el-button type="primary" :icon="Plus" @click="onShowCreate">新建歌单</el-button>
        </div>

        <!-- Loading -->
        <div v-if="loading" style="text-align:center;padding:40px">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        </div>

        <!-- Empty -->
        <el-empty v-else-if="playlists.length === 0" description="还没有歌单，点击上方按钮创建" />

        <!-- Playlist cards -->
        <el-card v-for="pl in playlists" :key="pl.id" class="playlist-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-title">
                <span class="playlist-name">{{ pl.name }}</span>
                <el-tag size="small" round type="info">{{ pl.trackCount || 0 }} 首</el-tag>
                <span v-if="pl.creatorNickname" class="creator">by {{ pl.creatorNickname }}</span>
              </div>
              <div class="card-actions">
                <el-button text :icon="Edit" @click="onShowEdit(pl)" />
                <el-button text type="danger" :icon="Delete" @click="onDeletePlaylist(pl)" />
                <el-button text v-if="expandedId !== pl.id" :icon="ArrowDown" @click="onExpand(pl)" />
                <el-button text v-else :icon="ArrowUp" @click="expandedId = null" />
              </div>
            </div>
          </template>

          <!-- Expanded track list -->
          <div v-if="expandedId === pl.id">
            <div v-if="trackLoading" style="text-align:center;padding:20px">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div v-else>
              <div v-if="!pl.tracks || pl.tracks.length === 0" class="no-tracks">
                暂无曲目，点击下方按钮添加
              </div>
              <div v-else class="track-list">
                <div v-for="(track, idx) in pl.tracks" :key="track.id" class="track-item">
                  <span class="track-index">{{ idx + 1 }}</span>
                  <span class="track-name">{{ track.fileName || '未命名' }}</span>
                  <span v-if="track.artist" class="track-artist">{{ track.artist }}</span>
                  <span class="track-duration">{{ formatDuration(track.durationSeconds) }}</span>
                  <el-button text size="small" :icon="Top" @click="onMoveTrack(pl, idx, -1)" :disabled="idx === 0" title="上移" />
                  <el-button text size="small" :icon="Bottom" @click="onMoveTrack(pl, idx, 1)" :disabled="idx === pl.tracks.length - 1" title="下移" />
                  <el-button text size="small" type="danger" :icon="Delete" @click="onRemoveTrack(pl, track.id)" title="移除" />
                </div>
              </div>
              <el-button style="margin-top:8px" size="small" :icon="Plus" @click="onShowAddTrack(pl)">添加曲目</el-button>
            </div>
          </div>
        </el-card>
      </template>
    </div>

    <!-- Create/Edit dialog -->
    <el-dialog v-model="showFormDialog" :title="editingPlaylist ? '编辑歌单' : '新建歌单'" width="420px">
      <el-form @submit.prevent="onSubmitPlaylist">
        <el-form-item label="歌单名称">
          <el-input v-model="playlistForm.name" placeholder="输入歌单名称" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmitPlaylist">
          {{ editingPlaylist ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Add tracks dialog -->
    <el-dialog v-model="showAddTrackDialog" title="添加曲目" width="550px">
      <div v-if="sessionsLoading" style="text-align:center;padding:20px">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>
      <el-table v-else :data="availableSessions" @selection-change="onTrackSelectionChange" max-height="360">
        <el-table-column type="selection" width="40" />
        <el-table-column label="文件名" prop="fileName" show-overflow-tooltip />
        <el-table-column label="时长" width="80">
          <template #default="{ row }">
            {{ formatDuration(row.duration) }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showAddTrackDialog = false">取消</el-button>
        <el-button type="primary" @click="onAddTracks" :loading="addingTracks" :disabled="selectedSessions.length === 0">
          添加选中 ({{ selectedSessions.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit, ArrowDown, ArrowUp, Top, Bottom, Loading } from '@element-plus/icons-vue'
import { usePlaylistStore } from '@/stores/playlist'
import { useGroupsStore } from '@/stores/groups'

const playlistStore = usePlaylistStore()
const groupsStore = useGroupsStore()

const playlists = computed(() => playlistStore.playlists)
const loading = computed(() => playlistStore.loading)
const groupId = computed(() => groupsStore.currentGroup?.id)

const expandedId = ref(null)
const trackLoading = ref(false)

// Create/edit form
const showFormDialog = ref(false)
const saving = ref(false)
const editingPlaylist = ref(null)
const playlistForm = reactive({ name: '' })

// Add track
const showAddTrackDialog = ref(false)
const addingToPlaylist = ref(null)
const availableSessions = ref([])
const sessionsLoading = ref(false)
const selectedSessions = ref([])
const addingTracks = ref(false)

// Load playlists when group changes
watch(groupId, (gid) => {
  if (gid) {
    playlistStore.fetchPlaylists(gid).catch(err => {
      ElMessage.error(err.response?.data?.message || '加载歌单失败')
    })
  }
}, { immediate: true })

// Format duration
function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '--:--'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// Expand playlist card and load tracks
async function onExpand(pl) {
  expandedId.value = pl.id
  if (!pl.tracks || pl.tracks.length === 0) {
    trackLoading.value = true
    try {
      await playlistStore.fetchTracks(pl.id)
    } catch (err) {
      ElMessage.error('加载曲目失败')
    } finally {
      trackLoading.value = false
    }
  }
}

// Create
function onShowCreate() {
  editingPlaylist.value = null
  playlistForm.name = ''
  showFormDialog.value = true
}

// Edit
function onShowEdit(pl) {
  editingPlaylist.value = pl
  playlistForm.name = pl.name
  showFormDialog.value = true
}

async function onSubmitPlaylist() {
  if (!playlistForm.name.trim()) {
    ElMessage.warning('请输入歌单名称')
    return
  }
  saving.value = true
  try {
    if (editingPlaylist.value) {
      await playlistStore.updatePlaylist(editingPlaylist.value.id, { name: playlistForm.name })
      // Update expanded card name in-place
      if (expandedId.value === editingPlaylist.value.id && playlistStore.currentPlaylist) {
        // currentPlaylist already updated in store
      }
      ElMessage.success('歌单已更新')
    } else {
      await playlistStore.createPlaylist(groupId.value, { name: playlistForm.name })
      ElMessage.success('歌单已创建')
    }
    showFormDialog.value = false
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

// Delete playlist
async function onDeletePlaylist(pl) {
  try {
    await ElMessageBox.confirm(`确定要删除歌单「${pl.name}」吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await playlistStore.deletePlaylist(pl.id)
    if (expandedId.value === pl.id) expandedId.value = null
    ElMessage.success('歌单已删除')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

// Move track up/down
async function onMoveTrack(pl, idx, direction) {
  const tracks = [...pl.tracks]
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= tracks.length) return
  // Swap
  ;[tracks[idx], tracks[newIdx]] = [tracks[newIdx], tracks[idx]]
  const trackIds = tracks.map(t => t.id)
  try {
    await playlistStore.reorderTracks(pl.id, trackIds)
  } catch (err) {
    ElMessage.error('排序失败')
  }
}

// Remove track
async function onRemoveTrack(pl, trackId) {
  try {
    await playlistStore.removeTrack(pl.id, trackId)
    ElMessage.success('曲目已移除')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '移除失败')
  }
}

// Show add track dialog
async function onShowAddTrack(pl) {
  addingToPlaylist.value = pl
  selectedSessions.value = []
  // Load sessions for this group
  sessionsLoading.value = true
  try {
    await groupsStore.fetchSessions(groupId.value)
    availableSessions.value = groupsStore.sessions || []
  } catch {
    availableSessions.value = []
  } finally {
    sessionsLoading.value = false
  }
  showAddTrackDialog.value = true
}

function onTrackSelectionChange(selection) {
  selectedSessions.value = selection
}

// Add selected tracks
async function onAddTracks() {
  if (selectedSessions.value.length === 0) return
  addingTracks.value = true
  try {
    for (const session of selectedSessions.value) {
      await playlistStore.addTrack(addingToPlaylist.value.id, {
        fileHash: session.fileHash,
        fileName: session.fileName,
        durationSeconds: session.duration
      })
    }
    ElMessage.success(`已添加 ${selectedSessions.value.length} 首曲目`)
    showAddTrackDialog.value = false
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '添加失败')
  } finally {
    addingTracks.value = false
  }
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
  margin: 0;
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

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.playlist-name {
  font-weight: 500;
  font-size: 15px;
}

.creator {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.track-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.track-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 14px;
}

.track-item:hover {
  background: var(--color-bg);
}

.track-index {
  width: 24px;
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.track-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-artist {
  color: var(--color-text-secondary);
  font-size: 12px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.track-duration {
  color: var(--color-text-secondary);
  font-size: 13px;
  font-family: monospace;
  width: 48px;
  text-align: right;
}

.no-tracks {
  text-align: center;
  color: var(--color-text-secondary);
  font-size: 13px;
  padding: 20px 0;
}
</style>
