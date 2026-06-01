<template>
  <div class="groups-page">
    <!-- Header -->
    <header class="page-header">
      <h1>WLTogether</h1>
      <div class="header-actions">
        <el-button @click="router.push('/settings')" :icon="Setting" circle />
        <el-dropdown @command="onCommand">
          <el-avatar :size="36" :src="authStore.user?.avatarUrl">
            {{ authStore.user?.nickname?.charAt(0) || 'U' }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                {{ authStore.user?.nickname || authStore.user?.username }}
              </el-dropdown-item>
              <el-dropdown-item command="settings" :icon="Setting">设置</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- Group list -->
    <div class="groups-container">
      <div class="groups-header">
        <span class="groups-title">我的群组</span>
        <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">创建群组</el-button>
      </div>

      <el-empty v-if="!groupsStore.loading && groups.length === 0" description="还没有群组，创建一个吧" />

      <div v-else class="groups-grid">
        <el-card
          v-for="group in groups"
          :key="group.id"
          class="group-card"
          shadow="hover"
          @click="router.push(`/groups/${group.id}`)"
        >
          <div class="group-card-content">
            <el-avatar :size="48" :src="group.avatarUrl" class="group-avatar">
              {{ group.name?.charAt(0) || 'G' }}
            </el-avatar>
            <div class="group-info">
              <span class="group-name">{{ group.name }}</span>
              <span class="group-meta">
                {{ group.memberCount || 0 }} 人
                <span v-if="group.lastMessageAt"> · {{ formatDate(group.lastMessageAt) }}</span>
              </span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- Create group dialog -->
    <el-dialog v-model="showCreateDialog" title="创建群组" width="400px" :close-on-click-modal="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="群组名称" prop="name">
          <el-input v-model="createForm.name" placeholder="输入群组名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="群组描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3"
            placeholder="输入群组描述（可选）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreateGroup">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useGroupsStore } from '@/stores/groups'
import { useWebSocket } from '@/composables/useWebSocket'

const router = useRouter()
const authStore = useAuthStore()
const groupsStore = useGroupsStore()
const { disconnect } = useWebSocket()

const groups = ref([])
const showCreateDialog = ref(false)
const creating = ref(false)

const createFormRef = ref(null)
const createForm = reactive({ name: '', description: '' })
const createRules = {
  name: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 1, max: 100, message: '群组名称1-100个字符', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述最多500个字符', trigger: 'blur' }
  ]
}

async function fetchGroups() {
  await groupsStore.fetchGroups()
  groups.value = groupsStore.groups
}

async function onCreateGroup() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    await groupsStore.createGroup({ name: createForm.name, description: createForm.description })
    ElMessage.success('群组创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.description = ''
    await fetchGroups()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    disconnect()
    authStore.logout()
    ElMessage.success('已退出登录')
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${date.getMonth() + 1}-${date.getDate()}`
}

onMounted(() => fetchGroups())
</script>

<style scoped>
.groups-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h1 {
  font-size: 20px;
  color: #667eea;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.groups-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.groups-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.groups-title {
  font-size: 18px;
  font-weight: 600;
}

.groups-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.group-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.group-card:hover {
  transform: translateY(-2px);
}

.group-card-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.group-avatar {
  flex-shrink: 0;
}

.group-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.group-name {
  font-size: 15px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  font-size: 13px;
  color: #909399;
}
</style>
