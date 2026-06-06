import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/LandingView.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/groups',
    name: 'GroupList',
    component: () => import('@/views/GroupListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/groups/:id',
    name: 'Group',
    component: () => import('@/views/GroupListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/sessions/video/:id',
    name: 'VideoSession',
    component: () => import('@/views/VideoSessionView.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/sessions/music/:id',
    name: 'MusicSession',
    component: () => import('@/views/MusicSessionView.vue'),
    meta: { requiresAuth: true },
    props: true
  },
  {
    path: '/playlists',
    name: 'Playlists',
    component: () => import('@/views/PlaylistView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/package/export',
    name: 'PackageExport',
    component: () => import('@/views/PackageExportView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/package/import',
    name: 'PackageImport',
    component: () => import('@/views/PackageImportView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/docs',
    name: 'Docs',
    component: () => import('@/views/DocsView.vue')
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { requiresAuth: true }
  },
  // ========== Admin Routes ==========
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UsersView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'groups',
        name: 'AdminGroups',
        component: () => import('@/views/admin/GroupsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'sessions',
        name: 'AdminSessions',
        component: () => import('@/views/admin/SessionsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'messages',
        name: 'AdminMessages',
        component: () => import('@/views/admin/MessagesView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'files',
        name: 'AdminFiles',
        component: () => import('@/views/admin/FilesView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('@/views/admin/AnnouncementsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'wallpapers',
        name: 'AdminWallpapers',
        component: () => import('@/views/admin/WallpapersView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'errors',
        name: 'AdminErrors',
        component: () => import('@/views/admin/ErrorsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/SettingsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/LogsView.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && authStore.isAuthenticated) {
    next({ name: 'GroupList' })
  } else if (to.meta.requiresAdmin) {
    if (!authStore.user || authStore.user.role !== 'ADMIN') {
      next({ name: 'GroupList' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
