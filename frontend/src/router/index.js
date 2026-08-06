import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
  {
    path: '/projects',
    name: 'projects',
    component: () => import('@/views/ProjectList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/projects/new',
    name: 'project-new',
    component: () => import('@/views/DesignUpload.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/projects/:id',
    name: 'project-detail',
    component: () => import('@/views/ProjectDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    // 独立的 3D 动态效果网页
    path: '/viewer/:generationId',
    name: 'viewer',
    component: () => import('@/views/Viewer3D.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('@/views/Community.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/me',
    name: 'me',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
