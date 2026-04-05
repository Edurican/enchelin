<template>
  <div class="rating-input">
    <button
      v-for="i in 5"
      :key="i"
      type="button"
      class="rating-star"
      :class="{ 'rating-star--active': i <= modelValue, 'rating-star--hover': i <= hoverValue }"
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
