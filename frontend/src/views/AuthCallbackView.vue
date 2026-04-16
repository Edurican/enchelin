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
import { exchangeToken } from '@/api/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const toast = useToast()
const errorMsg = ref('')

onMounted(async () => {
  if (route.query.error) {
    errorMsg.value = '인증에 실패했습니다.'
    toast.error('로그인에 실패했습니다. 다시 시도해주세요.')
    setTimeout(() => router.replace('/login'), 2000)
    return
  }

  const code = route.query.code
  if (code) {
    try {
      const res = await exchangeToken(code)
      const token = res.data?.token ?? res.data?.data?.token
      if (!token) throw new Error('token missing')
      auth.setToken(token)
      router.replace('/map')
    } catch {
      errorMsg.value = '인증에 실패했습니다.'
      toast.error('로그인에 실패했습니다. 다시 시도해주세요.')
      setTimeout(() => router.replace('/login'), 2000)
    }
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
