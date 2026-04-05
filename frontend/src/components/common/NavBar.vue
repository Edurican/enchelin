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
  padding-bottom: env(safe-area-inset-bottom);
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
  .navbar {
    position: static;
    border-top: none;
    border-bottom: 1px solid var(--color-border);
    height: 48px;
    justify-content: center;
    gap: var(--space-8);
  }

  .nav-item {
    flex-direction: row;
    gap: var(--space-2);
    font-size: var(--font-size-sm);
  }
}
</style>
