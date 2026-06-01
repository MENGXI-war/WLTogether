import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
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
    component: () => import('@/views/GroupView.vue'),
    meta: { requiresAuth: true },
    props: true
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
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/SettingsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/groups'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/groups'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.guest && authStore.isAuthenticated) {
    next({ name: 'GroupList' })
  } else {
    next()
  }
})

export default router
