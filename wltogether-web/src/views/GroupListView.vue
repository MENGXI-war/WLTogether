<template>
  <div class="groups-layout">
    <!-- ====== 群组栏 ====== -->
    <aside :class="['groups-sidebar', { expanded: groupsExpanded }]">
      <!-- 折叠按钮 -->
      <div class="sidebar-toggle" @click="groupsExpanded = !groupsExpanded">
        <el-icon :size="14"><component :is="groupsExpanded ? Fold : Expand" /></el-icon>
      </div>

      <div class="groups-list" v-loading="groupsLoading">
        <el-empty v-if="!groupsLoading && groups.length === 0" description="" :image-size="40" />

        <div
          v-for="g in groups"
          :key="g.id"
          :class="['group-item', { active: selectedGroupId === g.id }]"
          @click="selectGroup(g.id)"
        >
          <el-tooltip :content="groupsExpanded ? '' : g.name" placement="right" :disabled="groupsExpanded">
            <el-avatar :size="38" :src="g.avatarUrl" shape="square" class="group-avatar-icon">
              {{ g.name?.charAt(0) || 'G' }}
            </el-avatar>
          </el-tooltip>
          <span v-if="groupsExpanded" class="item-label">{{ g.name }}</span>
        </div>
      </div>
    </aside>

    <!-- ====== 动态栏 ====== -->
    <aside v-if="selectedGroupId" :class="['activity-bar', { expanded: activityExpanded }]">
      <!-- 折叠按钮 -->
      <div class="sidebar-toggle" @click="activityExpanded = !activityExpanded">
        <el-icon :size="14"><component :is="activityExpanded ? Fold : Expand" /></el-icon>
      </div>

      <!-- 群组信息 -->
      <div v-if="currentGroup" class="sidebar-group-info">
        <el-avatar :size="34" :src="currentGroup.avatarUrl" shape="square">
          {{ currentGroup.name?.charAt(0) || 'G' }}
        </el-avatar>
        <span v-if="activityExpanded" class="sidebar-group-name">{{ currentGroup.name }}</span>
      </div>

      <div class="section-divider" />

      <!-- 操作按钮 -->
      <div class="sidebar-actions">
        <div class="action-item">
          <el-button :icon="Search" circle title="搜索消息" @click="onSearchMessages" />
          <span v-if="activityExpanded" class="item-label">搜索</span>
        </div>

        <div class="action-item">
          <el-button :icon="Setting" circle title="群组设置" @click="openGroupSettings" />
          <span v-if="activityExpanded" class="item-label">设置</span>
        </div>

        <div class="action-item">
          <el-button :icon="Bell" circle title="群公告" @click="openAnnouncement" />
          <span v-if="activityExpanded" class="item-label">公告</span>
        </div>

        <div class="action-item">
          <el-button :icon="UserFilled" circle title="邀请成员" @click="showInviteDialog = true" />
          <span v-if="activityExpanded" class="item-label">邀请</span>
        </div>

        <div class="action-item">
          <el-button :icon="VideoCamera" circle title="发起同步会话" @click="showSessionDialog = true" />
          <span v-if="activityExpanded" class="item-label">发起同步</span>
        </div>
      </div>

      <!-- 进行中的会话 -->
      <template v-if="activeSessions.length > 0">
        <div class="section-divider" />
        <div class="session-icons">
          <div v-for="s in activeSessions" :key="s.id" class="action-item">
            <el-button
              :icon="s.mediaType === 'MUSIC' ? Headset : VideoPlay"
              :title="s.fileName"
              circle size="small" class="session-icon"
              @click="openSession(s)"
            />
            <span v-if="activityExpanded" class="item-label session-label">
              {{ s.fileName }}
            </span>
          </div>
        </div>
      </template>

      <!-- 成员 -->
      <div class="section-divider" />
      <div class="member-icons">
        <template v-for="m in members" :key="m.id">
          <el-popover
            v-if="canManage() && m.userId !== authStore.user.id"
            trigger="click" placement="right" :width="160"
            @show="memberMenuTarget = m"
          >
            <template #reference>
              <div class="action-item">
                <div class="avatar-wrapper">
                  <el-avatar :size="30" :src="m.avatarUrl"
                    :title="'管理 ' + (m.nickname || m.username)"
                    class="member-avatar member-manageable">
                    {{ (m.nickname || m.username)?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div v-if="isTyping(m.userId || m.id)" class="glass-overlay">
                    <span class="dot dot-1" />
                    <span class="dot dot-2" />
                    <span class="dot dot-3" />
                  </div>
                </div>
                <span v-if="activityExpanded" class="item-label">
                  {{ m.nickname || m.username }}
                  <span v-if="isTyping(m.userId || m.id)" class="typing-suffix">输入中</span>
                </span>
              </div>
            </template>
            <div class="member-menu">
              <div class="member-menu-title">{{ m.nickname || m.username }}</div>
              <el-button text size="small" @click="onChangeRole(m, 'ADMIN')" v-if="m.role !== 'ADMIN'">设为管理员</el-button>
              <el-button text size="small" @click="onChangeRole(m, 'MEMBER')" v-if="m.role === 'ADMIN'">取消管理员</el-button>
              <el-button text size="small" @click="onMuteMember(m, 1)">禁言 1 小时</el-button>
              <el-button text size="small" @click="onMuteMember(m, 24)">禁言 24 小时</el-button>
              <el-button text size="small" @click="onMuteMember(m, 168)">禁言 7 天</el-button>
              <el-button text size="small" type="danger" @click="onRemoveMember(m)">移出群组</el-button>
            </div>
          </el-popover>
          <div v-else class="action-item">
            <div class="avatar-wrapper">
              <el-avatar :size="30" :src="m.avatarUrl"
                :title="m.nickname || m.username"
                class="member-avatar">
                {{ (m.nickname || m.username)?.charAt(0) || 'U' }}
              </el-avatar>
              <div v-if="isTyping(m.userId || m.id)" class="glass-overlay">
                <span class="dot dot-1" />
                <span class="dot dot-2" />
                <span class="dot dot-3" />
              </div>
            </div>
            <span v-if="activityExpanded" class="item-label">
              {{ m.nickname || m.username }}
              <span v-if="isTyping(m.userId || m.id)" class="typing-suffix">输入中</span>
            </span>
          </div>
        </template>
      </div>
    </aside>

    <!-- ====== 右侧聊天区 ====== -->
    <main class="groups-main">
      <div v-if="!selectedGroupId" class="no-selection">
        <el-empty description="选择一个群组开始聊天" :image-size="100" />
      </div>
      <div v-else class="group-chat">
        <ChatPanel :groupId="selectedGroupId" />
      </div>
    </main>

    <!-- ====== 对话框 ====== -->
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

    <el-dialog v-model="showGroupSettings" title="群组设置" width="480px" :close-on-click-modal="false">
      <el-form ref="settingsFormRef" :model="settingsForm" :rules="settingsRules" label-position="top">
        <!-- Avatar -->
        <el-form-item label="群头像">
          <div class="avatar-upload-row">
            <el-avatar :size="64" :src="currentGroup.avatarUrl" shape="square">
              {{ currentGroup.name?.charAt(0) || 'G' }}
            </el-avatar>
            <div class="avatar-upload-actions">
              <el-button size="small" @click="onPickAvatar" :loading="uploadingAvatar">上传新头像</el-button>
              <el-button v-if="currentGroup.avatarUrl" size="small" type="danger" plain
                @click="onDeleteAvatar" :loading="deletingAvatar">移除头像</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="群组名称" prop="name">
          <el-input v-model="settingsForm.name" placeholder="输入群组名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="群组描述" prop="description">
          <el-input v-model="settingsForm.description" type="textarea" :rows="2"
            placeholder="输入群组描述（可选）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-input v-model="settingsForm.tags" placeholder="如：电影 音乐 游戏（空格分隔）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="加群方式" prop="joinMode">
          <el-radio-group v-model="settingsForm.joinMode">
            <el-radio value="OPEN">开放加入</el-radio>
            <el-radio value="APPROVAL">需要审批</el-radio>
            <el-radio value="INVITE_ONLY">仅邀请</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="display:flex;justify-content:space-between;width:100%">
          <el-popconfirm title="确定要解散该群组吗？此操作不可恢复" confirm-button-text="确定解散"
            cancel-button-text="取消" @confirm="onDeleteGroup">
            <template #reference>
              <el-button type="danger" plain :loading="deleting">解散群组</el-button>
            </template>
          </el-popconfirm>
          <div>
            <el-button @click="showGroupSettings = false">取消</el-button>
            <el-button type="primary" :loading="savingSettings" @click="onSaveGroupSettings">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="showAnnouncement" title="群公告" width="460px" :close-on-click-modal="false">
      <div v-if="announcementEditing">
        <el-input v-model="announcementText" type="textarea" :rows="5"
          placeholder="输入群公告…" maxlength="500" show-word-limit />
      </div>
      <div v-else class="announcement-display">
        <p v-if="currentGroup.announcement" class="announcement-text">{{ currentGroup.announcement }}</p>
        <el-empty v-else description="暂无群公告" :image-size="60" />
      </div>
      <template #footer>
        <div style="display:flex;justify-content:space-between;width:100%">
          <div>
            <el-button v-if="!announcementEditing"
              type="primary" plain @click="startEditAnnouncement">编辑</el-button>
            <el-button v-else @click="announcementEditing = false">取消</el-button>
          </div>
          <div>
            <el-button v-if="announcementEditing" type="primary" :loading="savingAnnouncement" @click="onSaveAnnouncement">
              保存
            </el-button>
            <el-button @click="showAnnouncement = false">关闭</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

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
        <el-button type="primary" :loading="creatingSession" @click="onCreateSession">发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  VideoCamera, FolderOpened, UserFilled, VideoPlay, Headset,
  Search, Setting, Fold, Expand, Bell
} from '@element-plus/icons-vue'
import { useGroupsStore } from '@/stores/groups'
import { useAuthStore } from '@/stores/auth'
import { useLocalFiles } from '@/composables/useLocalFiles'
import { useAppActions } from '@/composables/useAppActions'
import { useSessionTransferStore } from '@/stores/sessionTransfer'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const router = useRouter()
const route = useRoute()
const groupsStore = useGroupsStore()
const authStore = useAuthStore()
const { pickFiles, getFileUrl } = useLocalFiles()
const sessionTransferStore = useSessionTransferStore()

// --- Collapse state ---
const groupsExpanded = ref(false)
const activityExpanded = ref(false)

// --- Groups list ---
const groups = ref([])
const groupsLoading = ref(false)
const selectedGroupId = ref(null)

// --- Current group detail ---
const currentGroup = ref({})
const members = ref([])
const sessions = ref([])

// --- Typing users ---
const typingList = ref([])
function isTyping(userId) {
  return typingList.value.some(u => u.userId === userId)
}
function canManage() {
  if (!currentGroup.value || !authStore.user) return false
  if (currentGroup.value.ownerId === authStore.user.id) return true
  const me = members.value.find(m => m.userId === authStore.user.id)
  return me && (me.role === 'OWNER' || me.role === 'ADMIN')
}

// --- Dialogs ---
const showCreateDialog = ref(false)
const showGroupSettings = ref(false)
const showInviteDialog = ref(false)
const showSessionDialog = ref(false)
const inviteUsername = ref('')
const inviting = ref(false)
const creating = ref(false)
const creatingSession = ref(false)
const selectedFile = ref(null)
const fileHash = ref('')
const savingSettings = ref(false)
const deleting = ref(false)
const showAnnouncement = ref(false)
const announcementEditing = ref(false)
const announcementText = ref('')
const savingAnnouncement = ref(false)
const uploadingAvatar = ref(false)
const deletingAvatar = ref(false)
const settingsFormRef = ref(null)
const settingsForm = reactive({ name: '', description: '', tags: '', joinMode: 'INVITE_ONLY' })
const settingsRules = {
  name: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 1, max: 100, message: '群组名称1-100个字符', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '描述最多500个字符', trigger: 'blur' }],
  tags: [{ max: 500, message: '标签最多500个字符', trigger: 'blur' }]
}

// --- Forms ---
const createFormRef = ref(null)
const createForm = reactive({ name: '', description: '' })
const createRules = {
  name: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 1, max: 100, message: '群组名称1-100个字符', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '描述最多500个字符', trigger: 'blur' }]
}

const sessionFormRef = ref(null)
const sessionForm = reactive({ fileName: '', mediaType: 'VIDEO', fileHash: '' })
const sessionRules = {
  fileName: [{ required: true, message: '请输入会话名称', trigger: 'blur' }]
}

const activeSessions = computed(() =>
  sessions.value.filter(s => s.status === 'ACTIVE' || s.status === 'CREATED')
)

// --- Poll typing users from chatStore ---
let typingPollTimer = null
function updateTypingList() {
  if (!selectedGroupId.value) {
    typingList.value = []
    return
  }
  import('@/stores/chat').then(({ useChatStore }) => {
    try {
      const store = useChatStore()
      if (store && store.getTypingUsers) {
        typingList.value = store.getTypingUsers(selectedGroupId.value)
      }
    } catch (e) {
      console.error('[GroupListView] Failed to get typing users:', e)
    }
  }).catch(() => {
    // dynamic import failed silently
  })
}

watch(selectedGroupId, (newId) => {
  if (typingPollTimer) clearInterval(typingPollTimer)
  if (newId) {
    updateTypingList()
    typingPollTimer = setInterval(updateTypingList, 2000)
  }
})

onUnmounted(() => {
  if (typingPollTimer) clearInterval(typingPollTimer)
})

// --- Watch route param ---
watch(() => route.params.id, async (newId) => {
  if (newId) {
    const id = Number(newId)
    selectedGroupId.value = id
    await loadGroupDetail(id)
  }
}, { immediate: true })

// --- Fetch groups ---
async function fetchGroups() {
  groupsLoading.value = true
  try {
    await groupsStore.fetchGroups()
    groups.value = groupsStore.groups
  } finally {
    groupsLoading.value = false
  }
}

// --- Select group ---
async function selectGroup(id) {
  if (selectedGroupId.value === id) {
    // Toggle off: deselect and close activity bar
    selectedGroupId.value = null
    currentGroup.value = {}
    members.value = []
    sessions.value = []
    typingList.value = []
    router.replace({ name: 'GroupList' })
    return
  }
  selectedGroupId.value = id
  router.replace({ name: 'Group', params: { id } })
  await loadGroupDetail(id)
}

async function loadGroupDetail(id) {
  try {
    await groupsStore.fetchGroup(id)
    currentGroup.value = groupsStore.currentGroup
  } catch {
    ElMessage.error('无法加载群组')
    return
  }
  await groupsStore.fetchMembers(id)
  members.value = groupsStore.members
  await fetchSessions(id)
}

async function fetchSessions(id) {
  await groupsStore.fetchSessions(id)
  sessions.value = groupsStore.sessions
}

// --- Create group ---
async function onCreateGroup() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    const newGroup = await groupsStore.createGroup({ name: createForm.name, description: createForm.description })
    ElMessage.success('群组创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.description = ''
    await fetchGroups()
    selectGroup(newGroup.id)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

// --- Invite ---
async function onInvite() {
  if (!inviteUsername.value.trim()) return
  inviting.value = true
  try {
    await groupsStore.inviteMember(selectedGroupId.value, inviteUsername.value.trim())
    ElMessage.success('邀请成功')
    showInviteDialog.value = false
    inviteUsername.value = ''
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

// --- Session ---
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
  creatingSession.value = true
  try {
    const session = await groupsStore.createSession(selectedGroupId.value, {
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
    creatingSession.value = false
  }
}

// --- Search ---
function onSearchMessages() {
  ElMessage.info('消息搜索功能即将上线')
}

// --- Group settings ---
function openGroupSettings() {
  if (!currentGroup.value) {
    ElMessage.warning('群组信息加载中，请稍后再试')
    return
  }
  settingsForm.name = currentGroup.value.name || ''
  settingsForm.description = currentGroup.value.description || ''
  settingsForm.tags = currentGroup.value.tags || ''
  settingsForm.joinMode = currentGroup.value.joinMode || 'INVITE_ONLY'
  showGroupSettings.value = true
}

async function onSaveGroupSettings() {
  const valid = await settingsFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingSettings.value = true
  try {
    await groupsStore.updateGroup(selectedGroupId.value, {
      name: settingsForm.name,
      description: settingsForm.description,
      tags: settingsForm.tags,
      joinMode: settingsForm.joinMode
    })
    ElMessage.success('群组设置已保存')
    showGroupSettings.value = false
    // Refresh current group info
    currentGroup.value = await groupsStore.fetchGroup(selectedGroupId.value)
    // Refresh groups list
    await fetchGroups()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    savingSettings.value = false
  }
}

async function onDeleteGroup() {
  deleting.value = true
  try {
    await groupsStore.deleteGroup(selectedGroupId.value)
    ElMessage.success('群组已解散')
    showGroupSettings.value = false
    selectedGroupId.value = null
    currentGroup.value = {}
    members.value = []
    sessions.value = []
    router.replace({ name: 'GroupList' })
    await fetchGroups()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '解散失败')
  } finally {
    deleting.value = false
  }
}

// --- Announcement ---
function openAnnouncement() {
  if (!currentGroup.value) return
  announcementEditing.value = false
  announcementText.value = currentGroup.value.announcement || ''
  showAnnouncement.value = true
}

function startEditAnnouncement() {
  announcementText.value = currentGroup.value.announcement || ''
  announcementEditing.value = true
}

async function onSaveAnnouncement() {
  savingAnnouncement.value = true
  try {
    await groupsStore.updateGroup(selectedGroupId.value, {
      announcement: announcementText.value
    })
    ElMessage.success('公告已更新')
    announcementEditing.value = false
    currentGroup.value = await groupsStore.fetchGroup(selectedGroupId.value)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    savingAnnouncement.value = false
  }
}

// --- Avatar ---
async function onPickAvatar() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/png,image/jpeg,image/webp'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    uploadingAvatar.value = true
    try {
      await groupsStore.uploadAvatar(selectedGroupId.value, file)
      ElMessage.success('头像已更新')
      currentGroup.value = await groupsStore.fetchGroup(selectedGroupId.value)
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '上传失败')
    } finally {
      uploadingAvatar.value = false
    }
  }
  input.click()
}

async function onDeleteAvatar() {
  deletingAvatar.value = true
  try {
    await groupsStore.deleteAvatar(selectedGroupId.value)
    ElMessage.success('头像已移除')
    currentGroup.value = await groupsStore.fetchGroup(selectedGroupId.value)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    deletingAvatar.value = false
  }
}

// --- Member management state ---
const memberMenuTarget = ref(null)

async function onChangeRole(member, role) {
  try {
    await groupsStore.changeMemberRole(selectedGroupId.value, member.userId, role)
    ElMessage.success(role === 'ADMIN' ? '已设为管理员' : '已取消管理员')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

async function onMuteMember(member, hours) {
  try {
    await groupsStore.muteMember(selectedGroupId.value, member.userId, hours)
    const label = hours >= 168 ? '7 天' : hours >= 24 ? '24 小时' : `${hours} 小时`
    ElMessage.success(`已禁言 ${label}`)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  }
}

// --- Remove member ---
async function onRemoveMember(member) {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.nickname || member.username} 移出群组吗？`,
      '移除成员',
      { confirmButtonText: '确定移除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // cancelled
  }
  try {
    await groupsStore.removeMember(selectedGroupId.value, member.userId)
    ElMessage.success('成员已移除')
    members.value = members.value.filter(m => m.userId !== member.userId)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '移除失败')
  }
}

// --- Session click handling ---
function openSession(session) {
  const path = session.mediaType === 'MUSIC'
    ? `/sessions/music/${session.id}`
    : `/sessions/video/${session.id}`
  router.push(path)
}

// --- Listen for create group request from global header ---
const { onRequestCreateGroup } = useAppActions()
onRequestCreateGroup(() => {
  showCreateDialog.value = true
})

// --- Lifecycle ---
onMounted(() => fetchGroups())
onUnmounted(() => groupsStore.clearCurrent())
</script>

<style scoped>
.groups-layout {
  display: flex;
  height: calc(100vh - 52px);
  background: var(--color-bg);
}

/* ====== 群组栏 ====== */
.groups-sidebar {
  width: 60px;
  flex-shrink: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  transition: width 0.2s;
  overflow: hidden;
  border-right: 1px solid var(--color-border);
  position: relative;
}
.groups-sidebar.expanded {
  width: 150px;
  align-items: flex-start;
  padding: 4px 8px;
}

.sidebar-toggle {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-text-secondary);
  opacity: 0.4;
  transition: opacity 0.15s;
  flex-shrink: 0;
  z-index: 1;
}
.sidebar-toggle:hover {
  opacity: 1;
}

.groups-list {
  flex: 1;
  overflow-y: auto;
  width: 100%;
  padding: 4px;
  padding-top: 28px;
}
.groups-sidebar.expanded .groups-list {
  padding: 4px 0;
}

.group-item {
  display: flex;
  align-items: center;
  gap: 0;
  padding: 5px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  justify-content: center;
}
.groups-sidebar.expanded .group-item {
  justify-content: flex-start;
  padding: 6px 8px;
  gap: 10px;
}
.group-item:hover {
  background: rgba(0, 0, 0, 0.05);
}
.group-item.active::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: #409eff;
  border-radius: 0 3px 3px 0;
}
.groups-sidebar.expanded .group-item.active::before {
  left: 0;
}

.group-avatar-icon {
  flex-shrink: 0;
  transition: border-radius 0.2s;
  border-radius: 50%;
}
.group-item:hover .group-avatar-icon,
.group-item.active .group-avatar-icon {
  border-radius: 10px;
}

.item-label {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--color-text);
}

/* ====== 动态栏 ====== */
.activity-bar {
  width: 52px;
  flex-shrink: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  padding-top: 28px;
  gap: 2px;
  overflow-y: auto;
  transition: width 0.2s;
  position: relative;
}
.activity-bar.expanded {
  width: 140px;
  align-items: flex-start;
  padding: 4px 8px;
}

.avatar-wrapper {
  position: relative;
  display: inline-flex;
}

.glass-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #409eff;
  animation: dotFlow 1.2s ease-in-out infinite;
}
.dot-1 { animation-delay: 0s; }
.dot-2 { animation-delay: 0.2s; }
.dot-3 { animation-delay: 0.4s; }

@keyframes dotFlow {
  0%, 60%, 100% { transform: translateY(0); opacity: 1; }
  30% { transform: translateY(-4px); opacity: 0.3; }
}

.typing-suffix { color: var(--color-text-secondary); font-style: italic; margin-left: 2px; }

.announcement-display {
  min-height: 120px;
}
.announcement-text {
  white-space: pre-wrap;
  color: var(--color-text);
  line-height: 1.6;
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar-upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* group info in activity bar */
.sidebar-group-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 2px 0;
}
.activity-bar.expanded .sidebar-group-info {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.sidebar-group-name {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* action-item: row wrapper for icon + label */
.action-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
}
.activity-bar.expanded .action-item {
  justify-content: flex-start;
  gap: 8px;
}

/* actions */
.sidebar-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 2px 0;
}
.activity-bar.expanded .sidebar-actions {
  align-items: flex-start;
  gap: 2px;
}

.section-divider {
  width: 24px;
  height: 1px;
  background: var(--color-border);
  margin: 4px 0;
}
.activity-bar.expanded .section-divider {
  width: 100%;
}

.session-icons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.activity-bar.expanded .session-icons {
  align-items: flex-start;
}

.session-label {
  font-size: 11px;
}

.member-icons {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.activity-bar.expanded .member-icons {
  align-items: flex-start;
  gap: 4px;
}

.member-avatar {
  cursor: pointer;
  transition: transform 0.2s;
  flex-shrink: 0;
}
.member-avatar:hover {
  transform: scale(1.15);
}
.member-manageable {
  cursor: pointer;
}
.member-manageable:hover {
  transform: scale(1.15);
  outline: 2px solid #409eff;
  outline-offset: 1px;
}

.member-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.member-menu-title {
  font-weight: 600;
  font-size: 13px;
  padding: 4px 0 8px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 4px;
}
.member-menu .el-button {
  justify-content: flex-start;
}

/* 强制动态栏所有圆形按钮与群组栏头像一致尺寸 */
.activity-bar :deep(.el-button.is-circle) {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 38px !important;
  min-width: 38px !important;
  height: 38px;
  padding: 4px !important;
  margin: 0 !important;
  flex-shrink: 0;
}
.activity-bar :deep(.el-button.is-circle .el-icon) {
  margin: 0 !important;
}

/* ====== 右侧区 ====== */
.groups-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-width: 0;
}

.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 聊天区域背景透明 */
.group-chat :deep(.chat-panel) {
  background: transparent;
}
.group-chat :deep(.chat-input) {
  background: transparent;
}
.group-chat :deep(.chat-messages) {
  background: transparent;
}
</style>
