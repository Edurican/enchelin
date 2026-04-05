<template>
  <AppLayout>
    <RouterView />
  </AppLayout>
  <AppToast />
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppLayout from '@/components/common/AppLayout.vue'
import AppToast from '@/components/common/AppToast.vue'
</script>
