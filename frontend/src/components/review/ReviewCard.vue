<template>
  <div class="review-card card">
    <div class="review-header">
      <span class="review-author">{{ review.userName }}</span>
      <StarRating :rating="review.rating" size="sm" />
    </div>
    <p class="review-comment">{{ review.comment }}</p>
    <div class="review-footer">
      <time class="review-date">{{ formatDate(review.createdAt) }}</time>
      <div v-if="isOwner" class="review-actions">
        <button class="btn btn-sm btn-secondary" @click="$emit('edit', review)">수정</button>
        <button class="btn btn-sm btn-danger" @click="$emit('delete', review)">삭제</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import StarRating from './StarRating.vue'

defineProps({
  review: { type: Object, required: true },
  isOwner: { type: Boolean, default: false },
})

defineEmits(['edit', 'delete'])

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.review-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-author {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.review-comment {
  font-size: var(--font-size-base);
  line-height: 1.5;
  color: var(--color-text);
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.review-date {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.review-actions {
  display: flex;
  gap: var(--space-2);
}
</style>
