<template>
  <div class="detail-page container">
    <LoadingSpinner v-if="loading" message="식당 정보를 불러오는 중..." />
    <ErrorMessage
      v-else-if="error"
      :message="error"
      :retry="true"
      @retry="loadReviews"
    />
    <template v-else-if="restaurant">
      <section class="restaurant-info card">
        <h1 class="restaurant-name">{{ restaurant.name }}</h1>
        <p class="restaurant-category">{{ restaurant.category }}</p>
        <p class="restaurant-address">{{ restaurant.address }}</p>
        <div class="restaurant-meta">
          <div v-if="avgRating > 0" class="restaurant-rating">
            <StarRating :rating="Math.round(avgRating)" size="md" />
            <span class="rating-text">{{ avgRating.toFixed(1) }}</span>
          </div>
          <a
            v-if="restaurant.placeUrl"
            :href="restaurant.placeUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="btn btn-sm btn-secondary"
          >
            카카오맵에서 보기
          </a>
        </div>
      </section>

      <section class="reviews-section">
        <div class="reviews-header">
          <h2 class="reviews-title">리뷰</h2>
          <router-link
            v-if="auth.isAuthenticated"
            :to="{ name: 'ReviewCreate', query: { restaurantId: restaurant.id } }"
            class="btn btn-sm btn-primary"
          >
            리뷰 작성
          </router-link>
        </div>

        <ReviewList
          :reviews="reviews"
          :has-next="hasNext"
          :loading="reviewsLoading"
          :is-owner-fn="isOwner"
          @edit="onEdit"
          @delete="onDeleteRequest"
          @load-more="loadMore"
        />
      </section>
    </template>

    <ConfirmDialog
      :visible="deleteTarget !== null"
      message="이 리뷰를 삭제하시겠습니까?"
      @confirm="onDeleteConfirm"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRestaurantStore } from '@/stores/restaurant'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { fetchRestaurantReviews, deleteReview } from '@/api/review'
import { fetchRestaurantDetail } from '@/api/restaurant'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ReviewList from '@/components/review/ReviewList.vue'
import StarRating from '@/components/review/StarRating.vue'

const route = useRoute()
const router = useRouter()
const restaurantStore = useRestaurantStore()
const auth = useAuthStore()
const toast = useToast()

const restaurant = ref(null)
const reviews = ref([])
const loading = ref(true)
const error = ref('')
const reviewsLoading = ref(false)
const hasNext = ref(false)
const offset = ref(0)
const LIMIT = 10
const deleteTarget = ref(null)

const avgRating = computed(() => {
  if (reviews.value.length === 0) return 0
  const sum = reviews.value.reduce((acc, r) => acc + r.rating, 0)
  return sum / reviews.value.length
})

function isOwner(review) {
  return auth.user && review.userName === auth.user.nickname
}

async function loadReviews() {
  reviewsLoading.value = true
  error.value = ''
  try {
    const page = await fetchRestaurantReviews(restaurant.value.id, offset.value, LIMIT)
    if (offset.value === 0) {
      reviews.value = page.contents
    } else {
      reviews.value.push(...page.contents)
    }
    hasNext.value = page.hasNext
  } catch (e) {
    error.value = e.message || '리뷰를 불러올 수 없습니다.'
  } finally {
    reviewsLoading.value = false
  }
}

function loadMore() {
  offset.value += LIMIT
  loadReviews()
}

function onEdit(review) {
  router.push({
    name: 'ReviewEdit',
    params: { reviewId: review.reviewId },
    query: { restaurantId: restaurant.value.id },
  })
}

function onDeleteRequest(review) {
  deleteTarget.value = review
}

async function onDeleteConfirm() {
  if (!deleteTarget.value) return
  try {
    await deleteReview(deleteTarget.value.reviewId)
    reviews.value = reviews.value.filter((r) => r.reviewId !== deleteTarget.value.reviewId)
    toast.success('리뷰가 삭제되었습니다.')
  } catch (e) {
    toast.error(e.message || '삭제에 실패했습니다.')
  } finally {
    deleteTarget.value = null
  }
}

onMounted(async () => {
  const id = route.params.id

  // store에 선택된 식당이 있으면 사용
  if (restaurantStore.selected && String(restaurantStore.selected.id) === String(id)) {
    restaurant.value = restaurantStore.selected
  } else {
    // 직접 URL 접근 시 API로 조회 시도
    try {
      restaurant.value = await fetchRestaurantDetail(id)
    } catch {
      // 단일 조회 API 미구현 시 에러 처리
      error.value = '식당 정보를 불러올 수 없습니다. 지도에서 식당을 선택해주세요.'
      loading.value = false
      return
    }
  }

  loading.value = false
  await loadReviews()
})
</script>

<style scoped>
.detail-page {
  padding-top: var(--space-4);
  padding-bottom: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.restaurant-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.restaurant-name {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
}

.restaurant-category {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.restaurant-address {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.restaurant-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-2);
}

.restaurant-rating {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.rating-text {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--color-secondary-dark);
}

.reviews-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.reviews-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.reviews-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
}
</style>
