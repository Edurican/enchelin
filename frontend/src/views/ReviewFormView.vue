<template>
  <div class="review-form-page container">
    <h1 class="page-title">{{ isEdit ? '리뷰 수정' : '리뷰 작성' }}</h1>

    <LoadingSpinner v-if="initialLoading" message="리뷰 정보를 불러오는 중..." />
    <ErrorMessage v-else-if="initialError" :message="initialError" />

    <form v-else class="review-form card" @submit.prevent="onSubmit">
      <div class="form-group">
        <label class="form-label">평점</label>
        <RatingInput v-model="rating" />
        <p v-if="errors.rating" class="form-error">{{ errors.rating }}</p>
      </div>

      <div class="form-group">
        <label class="form-label" for="comment">코멘트</label>
        <textarea
          id="comment"
          v-model="comment"
          class="form-textarea"
          rows="4"
          maxlength="100"
          placeholder="식당에 대한 솔직한 리뷰를 남겨주세요. (최대 100자)"
        />
        <div class="form-row">
          <p v-if="errors.comment" class="form-error">{{ errors.comment }}</p>
          <span class="char-count">{{ comment.length }}/100</span>
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" @click="router.back()">취소</button>
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? '저장 중...' : (isEdit ? '수정' : '작성') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import { createReview, updateReview, fetchRestaurantReviews } from '@/api/review'
import RatingInput from '@/components/review/RatingInput.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const isEdit = computed(() => route.name === 'ReviewEdit')
const restaurantId = computed(() => route.query.restaurantId)

const rating = ref(0)
const comment = ref('')
const errors = ref({})
const submitting = ref(false)
const initialLoading = ref(false)
const initialError = ref('')

function validate() {
  const e = {}
  if (rating.value < 1 || rating.value > 5) {
    e.rating = '평점을 선택해주세요. (1~5)'
  }
  if (!comment.value.trim()) {
    e.comment = '코멘트를 입력해주세요.'
  } else if (comment.value.length > 100) {
    e.comment = '코멘트는 100자 이내로 입력해주세요.'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

async function onSubmit() {
  if (!validate()) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateReview(route.params.reviewId, rating.value, comment.value)
      toast.success('리뷰가 수정되었습니다.')
    } else {
      await createReview(restaurantId.value, rating.value, comment.value)
      toast.success('리뷰가 작성되었습니다.')
    }

    if (restaurantId.value) {
      router.replace(`/restaurants/${restaurantId.value}`)
    } else {
      router.back()
    }
  } catch (e) {
    toast.error(e.message || '리뷰 저장에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    initialLoading.value = true
    try {
      // 수정 모드: 기존 리뷰 데이터를 식당 리뷰 목록에서 찾아서 프리필
      // 단일 리뷰 조회 API가 없으므로 restaurantId의 리뷰에서 해당 reviewId를 찾음
      if (restaurantId.value) {
        const page = await fetchRestaurantReviews(restaurantId.value, 0, 50)
        const target = page.contents.find(
          (r) => String(r.reviewId) === String(route.params.reviewId),
        )
        if (target) {
          rating.value = target.rating
          comment.value = target.comment
        } else {
          initialError.value = '수정할 리뷰를 찾을 수 없습니다.'
        }
      } else {
        initialError.value = '식당 정보가 없습니다.'
      }
    } catch (e) {
      initialError.value = e.message || '리뷰를 불러올 수 없습니다.'
    } finally {
      initialLoading.value = false
    }
  }
})
</script>

<style scoped>
.review-form-page {
  padding-top: var(--space-4);
  padding-bottom: var(--space-6);
}

.page-title {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--space-5);
}

.review-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.char-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-left: auto;
}

.form-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}
</style>
