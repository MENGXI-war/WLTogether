<template>
  <div class="settings-page">
    <header class="page-header">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2>设置</h2>
    </header>

    <div class="settings-container">
      <el-tabs tab-position="left" class="settings-tabs">
        <!-- Profile -->
        <el-tab-pane label="个人资料">
          <div class="tab-content">
            <el-form :model="profileForm" label-position="top" class="settings-form">
              <el-form-item label="头像">
                <el-avatar :size="80" :src="authStore.user?.avatarUrl">
                  {{ authStore.user?.nickname?.charAt(0) || 'U' }}
                </el-avatar>
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
                <el-radio-group v-model="appearance.theme">
                  <el-radio value="light">浅色</el-radio>
                  <el-radio value="dark">深色</el-radio>
                  <el-radio value="auto">跟随系统</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="onSaveAppearance">保存</el-button>
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import usersApi from '@/api/users'

const router = useRouter()
const authStore = useAuthStore()
const saving = ref(false)

const profileForm = reactive({
  nickname: authStore.user?.nickname || ''
})

const appearance = reactive({
  theme: localStorage.getItem('theme') || 'auto'
})

const playback = reactive({
  defaultMode: localStorage.getItem('playbackMode') || 'HOST_ONLY',
  syncInterval: Number(localStorage.getItem('syncInterval')) || 5,
  deviationThreshold: Number(localStorage.getItem('deviationThreshold')) || 500
})

const p2p = reactive({
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

function onSaveAppearance() {
  localStorage.setItem('theme', appearance.theme)
  ElMessage.success('外观设置已保存')
}

function onSavePlayback() {
  localStorage.setItem('playbackMode', playback.defaultMode)
  localStorage.setItem('syncInterval', playback.syncInterval)
  localStorage.setItem('deviationThreshold', playback.deviationThreshold)
  ElMessage.success('播放设置已保存')
}

function onSaveP2p() {
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

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 24px;
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.page-header h2 { font-size: 18px; }

.settings-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
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
  color: #909399;
  font-size: 14px;
  margin-bottom: 4px;
}
</style>
