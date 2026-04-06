<template>
  <div class="app-layout">
    <NavBar v-if="showNav" />
    <main class="app-main" :class="{ 'app-main--with-nav': showNav }">
      <slot />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import NavBar from './NavBar.vue'

const route = useRoute()
const hideNavRoutes = ['Login', 'AuthCallback']
const showNav = computed(() => !hideNavRoutes.includes(route.name))
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  /* iOS 노치/홈바 safe-area: 좌우 패딩 */
  padding-left: env(safe-area-inset-left, 0px);
  padding-right: env(safe-area-inset-right, 0px);
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 모바일: NavBar가 하단 fixed이므로 본문 하단에 NavBar 높이 + safe-area 여백 확보 */
.app-main--with-nav {
  padding-bottom: calc(56px + env(safe-area-inset-bottom, 0px));
}

@media (min-width: 768px) {
  .app-layout {
    flex-direction: column;
  }

  /* 데스크톱: NavBar가 상단에 위치하므로 하단 패딩 불필요 */
  .app-main--with-nav {
    padding-bottom: 0;
  }
}
</style>
