<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div v-if="visible" class="dialog-overlay" @click.self="$emit('cancel')">
        <div class="dialog-box">
          <p class="dialog-message">{{ message }}</p>
          <div class="dialog-actions">
            <button class="btn btn-secondary btn-sm" @click="$emit('cancel')">취소</button>
            <button class="btn btn-danger btn-sm" @click="$emit('confirm')">{{ confirmText }}</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  message: { type: String, default: '정말 삭제하시겠습니까?' },
  confirmText: { type: String, default: '삭제' },
})

defineEmits(['confirm', 'cancel'])
</script>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: var(--space-4);
}

.dialog-box {
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  max-width: 320px;
  width: 100%;
  box-shadow: var(--shadow-lg);
}

.dialog-message {
  font-size: var(--font-size-base);
  color: var(--color-text);
  text-align: center;
  margin-bottom: var(--space-5);
}

.dialog-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
}

.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 200ms ease;
}

.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
</style>
