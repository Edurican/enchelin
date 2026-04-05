<template>
  <div class="review-list">
    <div v-if="reviews.length === 0 && !loading" class="review-empty">
      <p>아직 리뷰가 없습니다.</p>
    </div>
    <ReviewCard
      v-for="review in reviews"
      :key="review.reviewId"
      :review="review"
      :is-owner="isOwnerFn(review)"
      @edit="$emit('edit', $event)"
      @delete="$emit('delete', $event)"
    />
    <button
      v-if="hasNext && !loading"
      class="btn btn-secondary btn-block load-more-btn"
      @click="$emit('load-more')"
    >
      더 보기
    </button>
    <LoadingSpinner v-if="loading" />
  </div>
</template>

<script setup>
import ReviewCard from './ReviewCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

defineProps({
  reviews: { type: Array, default: () => [] },
  hasNext: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  isOwnerFn: { type: Function, default: () => false },
})

defineEmits(['edit', 'delete', 'load-more'])
</script>

<style scoped>
.review-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.review-empty {
  text-align: center;
  padding: var(--space-6);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.load-more-btn {
  margin-top: var(--space-2);
}
</style>
