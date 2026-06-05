<template>
  <div class="group-page">
    <!-- Header -->
    <GroupHeader
      :group="group"
      :members="members"
      @settings="showSettings = true"
    />

    <!-- Main layout -->
    <div class="group-main">
      <!-- Chat panel -->
      <div class="group-chat">
        <ChatPanel :groupId="groupId" />
      </div>

      <!-- Right sidebar -->
      <div class="group-sidebar">
        <!-- Active sessions -->
        <div class="sidebar-section" v-if="activeSessions.length > 0">
          <div class="section-title">进行中的会话</div>
          <SessionCard
            v-for="session in activeSessions"
            :key="session.id"
            :session="session"
            @click="openSession(session)"
          />
        </div>

        <!-- Members -->
        <div class="sidebar-section">
          <MemberList
            :members="members"
            @invite="showInviteDialog = true"
          />
        </div>

        <!-- New session button -->
        <div class="sidebar-section">
          <el-button type="primary" style="width: 100%" @click="showSessionDialog = true">
            <el-icon><VideoCamera /></el-icon> 发起同步会话
          </el-button>
        </div>
      </div>
    </div>

    <!-- Invite dialog -->
    <el-dialog v-model="showInviteDialog" title="邀请成员" width="380px">
      <el-form @submit.prevent="onInvite">
        <el-form-item label="用户名">
          <el-input v-model="inviteUsername" placeholder="输入要邀请的用户名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" :loading="inviting" @click="onInvite">邀请</el-button>
      </template>
    </el-dialog>

    <!-- Create session dialog -->
    <el-dialog v-model="showSessionDialog" title="发起同步会话" width="460px">
      <el-form ref="sessionFormRef" :model="sessionForm" :rules="sessionRules" label-position="top">
        <el-form-item label="本地文件" prop="file">
          <el-button @click="onPickFile" :icon="FolderOpened">
            {{ sessionForm.fileName || '选择本地媒体文件' }}
          </el-button>
        </el-form-item>
        <el-form-item label="会话名称" prop="fileName">
          <el-input v-model="sessionForm.fileName" placeholder="输入会话名称" />
        </el-form-item>
        <el-form-item label="媒体类型">
          <el-radio-group v-model="sessionForm.mediaType">
            <el-radio value="VIDEO">视频</el-radio>
            <el-radio value="MUSIC">音乐</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSessionDialog = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreateSession">发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VideoCamera, FolderOpened } from '@element-plus/icons-vue'
import { useGroupsStore } from '@/stores/groups'
import { useChat } from '@/composables/useChat'
import { useLocalFiles } from '@/composables/useLocalFiles'
import { useSessionTransferStore } from '@/stores/sessionTransfer'
import GroupHeader from '@/components/group/GroupHeader.vue'
import MemberList from '@/components/group/MemberList.vue'
import SessionCard from '@/components/group/SessionCard.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const props = defineProps({
  id: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const groupsStore = useGroupsStore()
const { pickFiles, getFileUrl } = useLocalFiles()
const sessionTransferStore = useSessionTransferStore()

const groupId = computed(() => Number(props.id))
const group = ref({})
const members = ref([])
const sessions = ref([])
const showSettings = ref(false)
const showInviteDialog = ref(false)
const showSessionDialog = ref(false)
const inviteUsername = ref('')
const inviting = ref(false)
const creating = ref(false)
const selectedFile = ref(null)
const fileHash = ref('')

const sessionFormRef = ref(null)
const sessionForm = reactive({
  fileName: '',
  mediaType: 'VIDEO',
  fileHash: ''
})
const sessionRules = {
  fileName: [{ required: true, message: '请输入会话名称', trigger: 'blur' }]
}

const activeSessions = computed(() =>
  sessions.value.filter(s => s.status === 'ACTIVE' || s.status === 'CREATED')
)

// Chat websocket initialization
const { onSessionEvent } = useChat(groupId.value)

onSessionEvent((event) => {
  if (event.type === 'session.created') {
    fetchSessions()
  }
})

async function fetchData() {
  try {
    group.value = await groupsStore.fetchGroup(groupId.value)
  } catch {
    ElMessage.error('无法加载群组')
    router.push('/groups')
    return
  }
  await groupsStore.fetchMembers(groupId.value)
  members.value = groupsStore.members
  await fetchSessions()
}

async function fetchSessions() {
  await groupsStore.fetchSessions(groupId.value)
  sessions.value = groupsStore.sessions
}

async function onInvite() {
  if (!inviteUsername.value.trim()) return
  inviting.value = true
  try {
    await groupsStore.inviteMember(groupId.value, inviteUsername.value.trim())
    ElMessage.success('邀请成功')
    showInviteDialog.value = false
    inviteUsername.value = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

async function onPickFile() {
  const result = await pickFiles()
  if (result) {
    selectedFile.value = result.file
    fileHash.value = result.hash
    sessionForm.fileHash = result.hash
    sessionForm.fileName = result.file.name
  }
}

async function onCreateSession() {
  if (!sessionForm.fileName.trim()) return

  creating.value = true
  try {
    const session = await groupsStore.createSession(groupId.value, {
      fileHash: sessionForm.fileHash || 'pending',
      fileName: sessionForm.fileName,
      fileSize: selectedFile.value?.size || 0,
      duration: 0,
      mediaType: sessionForm.mediaType,
      transferMode: 'p2pDirect'
    })
    ElMessage.success('会话已发起')
    showSessionDialog.value = false
    sessionForm.fileName = ''
    // Pass selected file to session view via store
    if (selectedFile.value) {
      const blobUrl = getFileUrl(selectedFile.value)
      sessionTransferStore.setPendingFile(selectedFile.value, fileHash.value, blobUrl)
      selectedFile.value = null
      fileHash.value = ''
    }
    openSession(session)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '发起失败')
  } finally {
    creating.value = false
  }
}

function openSession(session) {
  const path = session.mediaType === 'MUSIC'
    ? `/sessions/music/${session.id}`
    : `/sessions/video/${session.id}`
  router.push(path)
}

onMounted(() => fetchData())
onUnmounted(() => groupsStore.clearCurrent())
</script>

<style scoped>
.group-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
}

.group-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.group-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.group-sidebar {
  width: 300px;
  flex-shrink: 0;
  border-left: 1px solid var(--color-border);
  background: var(--color-card-bg);
  overflow-y: auto;
  padding: 12px;
}

.sidebar-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  padding: 0 4px;
}
</style>
