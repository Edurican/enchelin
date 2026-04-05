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
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.app-main--with-nav {
  padding-bottom: 56px;
}

@media (min-width: 768px) {
  .app-layout {
    flex-direction: column;
  }

  .app-main--with-nav {
    padding-bottom: 0;
  }
}
</style>
