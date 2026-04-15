<template>
  <div class="review-card card">
    <div class="review-header">
      <div class="review-header-left">
        <router-link
          v-if="review.userId"
          :to="{ name: 'UserProfile', params: { userId: review.userId } }"
          class="review-author"
        >{{ review.userName }}</router-link>
        <span v-else class="review-author">{{ review.userName }}</span>
        <span v-if="review.visitNumber" class="visit-badge">{{ review.visitNumber }}번째 방문</span>
      </div>
      <StarRating :rating="review.rating" size="sm" />
    </div>
    <router-link
      v-if="review.restaurantName"
      :to="{ name: 'RestaurantDetail', params: { id: review.restaurantId } }"
      class="review-restaurant"
    >{{ review.restaurantName }}</router-link>
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
  gap: var(--space-2);
}

.review-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.review-author {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  color: inherit;
  text-decoration: none;
}

.review-author:hover {
  text-decoration: underline;
}

.visit-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background-color: var(--color-primary-light, #e8f4fd);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  flex-shrink: 0;
}

.review-restaurant {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-primary);
  text-decoration: none;
}

.review-restaurant:hover {
  text-decoration: underline;
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
