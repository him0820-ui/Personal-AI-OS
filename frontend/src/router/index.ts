import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/main',
      name: 'Main',
      component: () => import('@/views/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '/main/chat',
          name: 'Chat',
          component: () => import('@/views/Chat.vue')
        },
        {
          path: '/main/chat/:sessionId',
          name: 'ChatWithSession',
          component: () => import('@/views/Chat.vue')
        },
        {
          path: '/main/memory',
          name: 'Memory',
          component: () => import('@/views/Memory.vue')
        },
        {
          path: '/main/planner',
          name: 'Planner',
          component: () => import('@/views/Planner.vue')
        },
        {
          path: '/main/review',
          name: 'Review',
          component: () => import('@/views/Review.vue')
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login' }
  }
})

export default router