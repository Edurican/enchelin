<template>
  <div class="callback-page">
    <LoadingSpinner v-if="!errorMsg" message="로그인 처리 중..." />
    <ErrorMessage v-else :message="errorMsg" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToast()
const errorMsg = ref('')

onMounted(() => {
  const token = route.query.token
  if (token) {
    auth.setToken(token)
    router.replace('/map')
  } else {
    errorMsg.value = '인증에 실패했습니다.'
    toast.error('로그인에 실패했습니다. 다시 시도해주세요.')
    setTimeout(() => router.replace('/login'), 2000)
  }
})
</script>

<style scoped>
.callback-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
