<template>
  <div class="rating-input">
    <button
      v-for="i in 5"
      :key="i"
      type="button"
      class="rating-star"
      :class="{ 'rating-star--active': i <= modelValue, 'rating-star--hover': i <= hoverValue }"
      :aria-label="`${i}점`"
      @mouseenter="hoverValue = i"
      @mouseleave="hoverValue = 0"
      @click="$emit('update:modelValue', i)"
    >
      ★
    </button>
    <span class="rating-value">{{ modelValue }}/5</span>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  modelValue: { type: Number, default: 0 },
})

defineEmits(['update:modelValue'])

const hoverValue = ref(0)
</script>

<style scoped>
.rating-input {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.rating-star {
  /* 터치 타깃 최소 36×36px */
  min-width: 36px;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  color: var(--color-border);
  transition: color var(--transition), transform var(--transition);
  padding: 0;
  line-height: 1;
}

.rating-star--active {
  color: var(--color-secondary);
}

.rating-star--hover {
  color: var(--color-secondary);
  transform: scale(1.15);
}

.rating-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-left: var(--space-2);
}
</style>
