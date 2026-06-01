<template>
  <header class="global-header">
    <div class="header-left">
      <router-link to="/groups" class="logo-link" title="首页">
        <img src="/Logs.png" alt="WLTogether" class="header-logo" />
      </router-link>
    </div>

    <div class="header-right">
      <el-tooltip content="首页" placement="bottom">
        <el-button :icon="HomeFilled" circle @click="router.push('/groups')" class="icon-btn" />
      </el-tooltip>

      <el-tooltip content="新建群组" placement="bottom">
        <el-button :icon="Plus" circle @click="onCreateGroup" class="icon-btn" />
      </el-tooltip>

      <el-tooltip content="设置" placement="bottom">
        <el-button :icon="Setting" circle @click="router.push('/settings')" class="icon-btn" />
      </el-tooltip>

      <el-dropdown trigger="click" @command="onCommand">
        <el-avatar :size="36" :src="authStore.user?.avatarUrl" class="avatar-btn">
          {{ authStore.user?.nickname?.charAt(0) || 'U' }}
        </el-avatar>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ authStore.user?.nickname || authStore.user?.username }}
            </el-dropdown-item>
            <el-dropdown-item command="settings" :icon="Setting">设置</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Plus, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import { useAppActions } from '@/composables/useAppActions'

const router = useRouter()
const authStore = useAuthStore()
const { disconnect } = useWebSocket()
const { requestCreateGroup } = useAppActions()

function onCreateGroup() {
  requestCreateGroup()
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    disconnect()
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}
</script>

<style scoped>
.global-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 20px;
  background: transparent;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  pointer-events: none;
}

.global-header > * {
  pointer-events: auto;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  line-height: 0;
}

.header-logo {
  height: 48px;
  width: 48px;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.2s;
}

.header-logo:hover {
  transform: scale(1.1);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-btn {
  transition: transform 0.2s;
  width: 36px;
  height: 36px;
}

.icon-btn:hover {
  transform: scale(1.1);
}

.avatar-btn {
  cursor: pointer;
  width: 36px;
  height: 36px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.avatar-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
</style>
