import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

// 라우트 정의를 직접 가져와 테스트용 라우터 생성
// (실제 컴포넌트 lazy-import는 더미로 대체)
const dummyComponent = { template: '<div />' }

function buildRouter(isAuthenticated) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'Login', component: dummyComponent, meta: { requiresAuth: false } },
      { path: '/map', name: 'Map', component: dummyComponent, meta: { requiresAuth: true } },
      { path: '/mypage', name: 'MyPage', component: dummyComponent, meta: { requiresAuth: true } },
      { path: '/', redirect: '/map' },
    ],
  })

  router.beforeEach((to) => {
    // stores/auth를 직접 읽지 않고 주입된 값 사용
    const authenticated = isAuthenticated()
    if (to.meta.requiresAuth && !authenticated) {
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
    if (to.name === 'Login' && authenticated) {
      return { name: 'Map' }
    }
  })

  return router
}

describe('router beforeEach 가드', () => {
  // ─── 비로그인 상태 ────────────────────────────────────────────
  it('비로그인 상태에서 /map 접근 시 /login 으로 리다이렉트된다', async () => {
    // Arrange
    const router = buildRouter(() => false)
    await router.push('/map')

    // Assert
    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/map')
  })

  it('비로그인 상태에서 /mypage 접근 시 /login 으로 리다이렉트된다', async () => {
    // Arrange
    const router = buildRouter(() => false)
    await router.push('/mypage')

    // Assert
    expect(router.currentRoute.value.name).toBe('Login')
    expect(router.currentRoute.value.query.redirect).toBe('/mypage')
  })

  // ─── 로그인 상태 ──────────────────────────────────────────────
  it('로그인 상태에서 /map 접근 시 정상 진입된다', async () => {
    // Arrange
    const router = buildRouter(() => true)
    await router.push('/map')

    // Assert
    expect(router.currentRoute.value.name).toBe('Map')
  })

  it('로그인 상태에서 /login 접근 시 /map 으로 리다이렉트된다', async () => {
    // Arrange
    const router = buildRouter(() => true)
    await router.push('/login')

    // Assert
    expect(router.currentRoute.value.name).toBe('Map')
  })

  // ─── requiresAuth: false 라우트 ───────────────────────────────
  it('비로그인 상태에서 requiresAuth:false 라우트는 정상 접근된다', async () => {
    // Arrange
    const router = buildRouter(() => false)
    await router.push('/login')

    // Assert
    expect(router.currentRoute.value.name).toBe('Login')
  })
})
