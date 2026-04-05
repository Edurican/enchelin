<template>
  <div class="star-rating" :class="`star-rating--${size}`">
    <span
      v-for="i in 5"
      :key="i"
      class="star"
      :class="{ 'star--filled': i <= rating }"
    >★</span>
  </div>
</template>

<script setup>
defineProps({
  rating: { type: Number, required: true },
  size: { type: String, default: 'sm', validator: (v) => ['sm', 'md'].includes(v) },
})
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  gap: 1px;
}

.star {
  color: var(--color-border);
  line-height: 1;
}

.star--filled {
  color: var(--color-secondary);
}

.star-rating--sm .star {
  font-size: 0.875rem;
}

.star-rating--md .star {
  font-size: 1.25rem;
}
</style>
