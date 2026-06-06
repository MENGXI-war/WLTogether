<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside :class="['admin-sidebar', { collapsed }]">
      <div class="sidebar-header">
        <router-link to="/admin/dashboard" class="brand-link">
          <img src="/Logs.png" alt="WL" class="brand-logo" />
          <span v-show="!collapsed" class="brand-text">管理后台</span>
        </router-link>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/groups">
          <el-icon><Grid /></el-icon>
          <template #title>群组管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/sessions">
          <el-icon><VideoCamera /></el-icon>
          <template #title>会话管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/messages">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>消息管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/files">
          <el-icon><FolderOpened /></el-icon>
          <template #title>文件管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/announcements">
          <el-icon><Bell /></el-icon>
          <template #title>公告管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/wallpapers">
          <el-icon><Picture /></el-icon>
          <template #title>壁纸管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/errors">
          <el-icon><WarningFilled /></el-icon>
          <template #title>错误日志</template>
        </el-menu-item>
        <el-menu-item index="/admin/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Document /></el-icon>
          <template #title>日志查看</template>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <el-button :icon="Expand" circle size="small" class="collapse-btn"
          :title="collapsed ? '展开菜单' : '折叠菜单'" @click="collapsed = !collapsed" />
      </div>
    </aside>

    <!-- Main Content -->
    <div class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <el-tooltip content="切换主题" placement="bottom">
            <el-button :icon="isDark ? Sunny : Moon" circle @click="onToggleTheme" />
          </el-tooltip>
          <el-tooltip content="返回前台" placement="bottom">
            <el-button :icon="House" circle @click="$router.push('/groups')" />
          </el-tooltip>
          <el-avatar :size="32" :src="authStore.user?.avatarUrl">
            {{ authStore.user?.nickname?.charAt(0) || 'A' }}
          </el-avatar>
        </div>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  DataAnalysis, UserFilled, Grid, VideoCamera, ChatDotRound,
  FolderOpened, Bell, Picture, WarningFilled, Setting, Document,
  Expand, House, Sunny, Moon
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const authStore = useAuthStore()
const collapsed = ref(false)
const { isDark, setTheme } = useTheme()

const activeMenu = computed(() => route.path)

function onToggleTheme() {
  setTheme(isDark.value ? 'light' : 'dark')
}

const pageTitle = computed(() => {
  const titles = {
    '/admin/dashboard': '仪表盘',
    '/admin/users': '用户管理',
    '/admin/groups': '群组管理',
    '/admin/sessions': '会话管理',
    '/admin/messages': '消息管理',
    '/admin/files': '文件管理',
    '/admin/announcements': '公告管理',
    '/admin/wallpapers': '壁纸管理',
    '/admin/errors': '错误日志',
    '/admin/settings': '系统设置',
    '/admin/logs': '日志查看'
  }
  return titles[route.path] || '管理后台'
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

/* Sidebar */
.admin-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--color-sidebar);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  transition: width 0.25s;
  overflow: hidden;
}
.admin-sidebar.collapsed { width: 64px; }

.sidebar-header {
  display: flex;
  align-items: center;
  padding: 16px;
  height: 60px;
  border-bottom: 1px solid var(--color-border);
}
.brand-link {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
}
.brand-logo { width: 32px; height: 32px; border-radius: 6px; flex-shrink: 0; }
.brand-text { font-size: 16px; font-weight: 700; color: #667eea; }

.admin-menu {
  flex: 1;
  border-right: none !important;
  overflow-y: auto;
  padding: 8px 0;
  background: transparent;
}
.admin-menu :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 8px;
}

.sidebar-footer {
  display: flex;
  justify-content: center;
  padding: 12px;
  border-top: 1px solid var(--color-border);
}
.admin-sidebar.collapsed .collapse-btn { transform: rotate(180deg); }

/* Main */
.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  padding: 0 24px;
  background: var(--color-card-bg);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.page-title { font-size: 18px; font-weight: 600; color: var(--color-text); margin: 0; }
.header-right { display: flex; align-items: center; gap: 12px; }

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}
</style>
