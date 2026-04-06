<template>
  <nav class="navbar">
    <router-link
      v-for="item in navItems"
      :key="item.to"
      :to="item.to"
      class="nav-item"
      active-class="nav-item--active"
    >
      <span class="nav-icon">{{ item.icon }}</span>
      <span class="nav-label">{{ item.label }}</span>
    </router-link>
  </nav>
</template>

<script setup>
const navItems = [
  { to: '/map', icon: '🗺', label: '지도' },
  { to: '/mypage', icon: '👤', label: '마이' },
]
</script>

<style scoped>
.navbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 56px;
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
  z-index: 100;
  /* iOS 홈바(safe-area) 반영: 하단 여백 확보 */
  padding-bottom: env(safe-area-inset-bottom, 0px);
  /* iOS 사이드 safe-area 반영 */
  padding-left: env(safe-area-inset-left, 0px);
  padding-right: env(safe-area-inset-right, 0px);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  font-size: var(--font-size-xs);
  color: var(--color-neutral);
  transition: color var(--transition);
  padding: var(--space-1) var(--space-4);
}

.nav-item--active {
  color: var(--color-primary);
}

.nav-icon {
  font-size: 1.25rem;
}

.nav-label {
  font-weight: var(--font-weight-medium);
}

@media (min-width: 768px) {
  /* 데스크톱: 하단 fixed → 상단 sticky 전환 */
  .navbar {
    position: sticky;
    top: 0;
    bottom: auto;
    border-top: none;
    border-bottom: 1px solid var(--color-border);
    height: 48px;
    justify-content: center;
    gap: var(--space-8);
    /* 상단 safe-area 반영 (노치 있는 기기 가로 모드 등) */
    padding-top: env(safe-area-inset-top, 0px);
    padding-bottom: 0;
    padding-left: 0;
    padding-right: 0;
  }

  .nav-item {
    flex-direction: row;
    gap: var(--space-2);
    font-size: var(--font-size-sm);
  }
}
</style>
