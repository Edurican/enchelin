<template>
  <div class="error-box">
    <p class="error-text">{{ message }}</p>
    <button v-if="retry" class="btn btn-sm btn-secondary" @click="$emit('retry')">
      다시 시도
    </button>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '오류가 발생했습니다.' },
  retry: { type: Boolean, default: false },
})

defineEmits(['retry'])
</script>

<style scoped>
.error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-5);
  text-align: center;
}

.error-text {
  font-size: var(--font-size-sm);
  color: var(--color-error);
}
</style>
