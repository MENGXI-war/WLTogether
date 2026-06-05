<template>
  <div class="settings-page">
    <div class="settings-container">
      <div class="settings-header">
        <el-button text :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2>设置</h2>
      </div>
      <el-tabs tab-position="left" class="settings-tabs">
        <!-- Profile -->
        <el-tab-pane label="个人资料">
          <div class="tab-content">
            <el-form :model="profileForm" label-position="top" class="settings-form">
              <el-form-item label="头像">
                <div class="avatar-upload-row">
                  <el-avatar :size="80" :src="authStore.user?.avatarUrl">
                    {{ authStore.user?.nickname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="avatar-upload-actions">
                    <el-button size="small" @click="onPickUserAvatar" :loading="uploadingAvatar">上传新头像</el-button>
                    <el-button v-if="authStore.user?.avatarUrl" size="small" type="danger" plain
                      @click="onDeleteUserAvatar" :loading="deletingAvatar">移除头像</el-button>
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="用户名">
                <el-input :value="authStore.user?.username" disabled />
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input :value="authStore.user?.email" disabled />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="profileForm.nickname" placeholder="输入昵称" maxlength="100" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onUpdateProfile" :loading="saving">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Appearance -->
        <el-tab-pane label="外观">
          <div class="tab-content">
            <el-form label-position="top" class="settings-form">
              <el-form-item label="主题">
                <el-radio-group v-model="appearance.theme" @change="onThemeChange">
                  <el-radio value="light">浅色</el-radio>
                  <el-radio value="dark">深色</el-radio>
                  <el-radio value="auto">跟随系统</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Playback -->
        <el-tab-pane label="播放设置">
          <div class="tab-content">
            <el-form label-position="top" class="settings-form">
              <el-form-item label="默认播放模式">
                <el-radio-group v-model="playback.defaultMode">
                  <el-radio value="HOST_ONLY">仅主持人</el-radio>
                  <el-radio value="SHARED_CONTROL">共享控制</el-radio>
                  <el-radio value="FREE">自由模式</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="时钟同步间隔 (秒)">
                <el-slider v-model="playback.syncInterval" :min="1" :max="30" :step="1" show-input />
              </el-form-item>
              <el-form-item label="同步偏差阈值 (毫秒)">
                <el-input-number v-model="playback.deviationThreshold" :min="100" :max="2000" :step="100" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSavePlayback">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- P2P -->
        <el-tab-pane label="P2P 传输">
          <div class="tab-content">
            <el-form label-position="top" class="settings-form">
              <el-form-item label="启用 P2P 传输">
                <el-switch v-model="p2p.enabled" active-text="启用" inactive-text="禁用" />
                <div class="setting-hint">启用后媒体文件将通过 P2P 网络传输，关闭后仅使用服务器中转</div>
              </el-form-item>
              <el-form-item label="最大上传速度">
                <el-select v-model="p2p.maxUploadSpeed">
                  <el-option label="不限制" :value="0" />
                  <el-option label="1 MB/s" :value="1048576" />
                  <el-option label="5 MB/s" :value="5242880" />
                  <el-option label="10 MB/s" :value="10485760" />
                </el-select>
              </el-form-item>
              <el-form-item label="最大下载速度">
                <el-select v-model="p2p.maxDownloadSpeed">
                  <el-option label="不限制" :value="0" />
                  <el-option label="1 MB/s" :value="1048576" />
                  <el-option label="5 MB/s" :value="5242880" />
                  <el-option label="10 MB/s" :value="10485760" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSaveP2p">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- Security -->
        <el-tab-pane label="安全">
          <div class="tab-content">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="settings-form">
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input v-model="passwordForm.currentPassword" type="password" placeholder="输入当前密码" show-password />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码（至少6位）" show-password />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onChangePassword" :loading="changingPassword">修改密码</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- About -->
        <el-tab-pane label="关于">
          <div class="tab-content">
            <div class="about-info">
              <h3>WLTogether</h3>
              <p>版本 0.1.0</p>
              <p>私有自托管网络媒体同步播放器</p>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import usersApi from '@/api/users'

const router = useRouter()
const authStore = useAuthStore()

function goBack() {
  router.back()
}
const saving = ref(false)
const uploadingAvatar = ref(false)
const deletingAvatar = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref(null)

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const profileForm = reactive({
  nickname: authStore.user?.nickname || ''
})

const { currentTheme, setTheme } = useTheme()

const appearance = reactive({
  theme: currentTheme.value
})

// Apply theme immediately when radio changes
function onThemeChange(value) {
  setTheme(value)
  ElMessage.success('主题已切换')
}

// Keep appearance in sync with external changes (e.g. system preference)
watch(currentTheme, (val) => {
  appearance.theme = val
})

const playback = reactive({
  defaultMode: localStorage.getItem('playbackMode') || 'HOST_ONLY',
  syncInterval: Number(localStorage.getItem('syncInterval')) || 5,
  deviationThreshold: Number(localStorage.getItem('deviationThreshold')) || 500
})

const p2p = reactive({
  enabled: localStorage.getItem('p2pEnabled') !== 'false',
  maxUploadSpeed: Number(localStorage.getItem('maxUploadSpeed')) || 0,
  maxDownloadSpeed: Number(localStorage.getItem('maxDownloadSpeed')) || 0
})

async function onUpdateProfile() {
  if (!profileForm.nickname.trim()) return
  saving.value = true
  try {
    await usersApi.updateMe({ nickname: profileForm.nickname })
    await authStore.fetchMe()
    ElMessage.success('个人资料已更新')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

function onPickUserAvatar() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = 'image/png,image/jpeg,image/webp'
  input.onchange = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      ElMessage.warning('头像大小不能超过 2MB')
      return
    }
    uploadingAvatar.value = true
    try {
      await usersApi.uploadAvatar(file)
      await authStore.fetchMe()
      ElMessage.success('头像已更新')
    } catch (err) {
      ElMessage.error(err.response?.data?.message || '上传失败')
    } finally {
      uploadingAvatar.value = false
    }
  }
  input.click()
}

async function onDeleteUserAvatar() {
  deletingAvatar.value = true
  try {
    await usersApi.deleteAvatar()
    await authStore.fetchMe()
    ElMessage.success('头像已移除')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    deletingAvatar.value = false
  }
}

function onSavePlayback() {
  localStorage.setItem('playbackMode', playback.defaultMode)
  localStorage.setItem('syncInterval', playback.syncInterval)
  localStorage.setItem('deviationThreshold', playback.deviationThreshold)
  ElMessage.success('播放设置已保存')
}

async function onChangePassword() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch { return }
  changingPassword.value = true
  try {
    await usersApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    // Redirect to login
    setTimeout(() => {
      authStore.logout()
      router.push('/login')
    }, 1500)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

function onSaveP2p() {
  localStorage.setItem('p2pEnabled', p2p.enabled)
  localStorage.setItem('maxUploadSpeed', p2p.maxUploadSpeed)
  localStorage.setItem('maxDownloadSpeed', p2p.maxDownloadSpeed)
  ElMessage.success('P2P 设置已保存')
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: var(--color-bg);
}

.settings-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 24px;
  background: var(--color-card-bg);
  border-radius: 12px;
}

.settings-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border);
}

.settings-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.settings-tabs {
  min-height: 400px;
}

.settings-tabs :deep(.el-tabs__item) {
  height: 40px;
  line-height: 40px;
}

.tab-content {
  padding-left: 24px;
}

.settings-form {
  max-width: 400px;
}

.about-info {
  padding: 24px 0;
}

.about-info h3 {
  font-size: 24px;
  color: #667eea;
  margin-bottom: 8px;
}

.about-info p {
  color: var(--color-text-secondary);
  font-size: 14px;
  margin-bottom: 4px;
}

.setting-hint {
  color: var(--color-text-secondary);
  font-size: 12px;
  margin-top: 4px;
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
</style>
