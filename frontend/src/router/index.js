import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/auth/callback',
    name: 'AuthCallback',
    component: () => import('@/views/AuthCallbackView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/map',
    name: 'Map',
    component: () => import('@/views/MapView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/restaurants/:id',
    name: 'RestaurantDetail',
    component: () => import('@/views/RestaurantDetailView.vue'),
    meta: { requiresAuth: false },
    props: true,
  },
  {
    path: '/reviews/new',
    name: 'ReviewCreate',
    component: () => import('@/views/ReviewFormView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/reviews/:reviewId/edit',
    name: 'ReviewEdit',
    component: () => import('@/views/ReviewFormView.vue'),
    meta: { requiresAuth: true },
    props: true,
  },
  {
    path: '/mypage',
    name: 'MyPage',
    component: () => import('@/views/MyPageView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    redirect: '/map',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'Login' && auth.isAuthenticated) {
    return { name: 'Map' }
  }
})

export default router
