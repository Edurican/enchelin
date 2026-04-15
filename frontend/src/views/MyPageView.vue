<template>
  <div class="mypage container">
    <section class="profile-card card">
      <img
        v-if="auth.user?.avatarUrl"
        :src="auth.user.avatarUrl"
        alt="프로필 이미지"
        class="profile-avatar"
      />
      <div v-else class="profile-avatar profile-avatar--placeholder">👤</div>
      <div class="profile-info">
        <h1 class="profile-name">{{ auth.user?.nickname || '사용자' }}</h1>
        <a
          v-if="auth.user?.htmlUrl"
          :href="auth.user.htmlUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="profile-github"
        >
          GitHub 프로필
        </a>
      </div>
      <button class="btn btn-sm btn-secondary logout-btn" @click="onLogout">로그아웃</button>
    </section>

    <section v-if="stats" class="stats-card card">
      <div class="stat-item">
        <span class="stat-value">{{ stats.reviewCount }}</span>
        <span class="stat-label">작성한 리뷰</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-value">{{ stats.averageRating.toFixed(1) }}</span>
        <span class="stat-label">평균 별점</span>
      </div>
    </section>

    <section class="my-reviews">
      <div class="reviews-header">
        <h2 class="section-title">내 리뷰</h2>
        <select
          v-model="sort"
          class="sort-select"
          aria-label="리뷰 정렬"
          @change="resetAndLoad"
        >
          <option value="latest">최신순</option>
          <option value="rating">별점 높은순</option>
          <option value="name">음식점 이름순</option>
        </select>
      </div>

      <LoadingSpinner v-if="loading && reviews.length === 0" message="리뷰를 불러오는 중..." />
      <ErrorMessage
        v-else-if="error"
        :message="error"
        :retry="true"
        @retry="loadMyReviews"
      />
      <template v-else>
        <ReviewList
          :reviews="reviews"
          :has-next="hasNext"
          :loading="loading"
          :is-owner-fn="() => true"
          @edit="onEdit"
          @delete="onDeleteRequest"
          @load-more="loadMore"
        />
      </template>
    </section>

    <ConfirmDialog
      :visible="deleteTarget !== null"
      message="이 리뷰를 삭제하시겠습니까?"
      @confirm="onDeleteConfirm"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/composables/useToast'
import { fetchUserReviews, fetchUserStats, deleteReview } from '@/api/review'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import ReviewList from '@/components/review/ReviewList.vue'

const router = useRouter()
const auth = useAuthStore()
const toast = useToast()

const reviews = ref([])
const loading = ref(false)
const error = ref('')
const hasNext = ref(false)
const offset = ref(0)
const LIMIT = 10
const deleteTarget = ref(null)
const sort = ref('latest')
const stats = ref(null)

async function loadMyReviews() {
  if (!auth.user?.id) return
  loading.value = true
  error.value = ''
  try {
    const page = await fetchUserReviews(auth.user.id, offset.value, LIMIT, sort.value)
    if (offset.value === 0) {
      reviews.value = page.contents
    } else {
      reviews.value.push(...page.contents)
    }
    hasNext.value = page.hasNext
  } catch (e) {
    error.value = e.message || '리뷰를 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

function resetAndLoad() {
  offset.value = 0
  reviews.value = []
  loadMyReviews()
}

function loadMore() {
  offset.value += LIMIT
  loadMyReviews()
}

function onEdit(review) {
  router.push({
    name: 'ReviewEdit',
    params: { reviewId: review.reviewId },
    query: { restaurantId: review.restaurantId },
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

function onLogout() {
  auth.logout()
  router.replace('/login')
}

onMounted(async () => {
  if (auth.user?.id) {
    stats.value = await fetchUserStats(auth.user.id).catch(() => null)
  }
  loadMyReviews()
})
</script>

<style scoped>
.mypage {
  padding-top: var(--space-4);
  padding-bottom: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.profile-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  max-width: 100%;
}

.profile-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  font-size: 1.5rem;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-github {
  font-size: var(--font-size-sm);
  color: var(--color-primary);
}

.logout-btn {
  flex-shrink: 0;
  margin-left: auto;
}

.stats-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-6);
  padding: var(--space-4);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-primary);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--color-border);
}

.reviews-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
}

.sort-select {
  appearance: none;
  -webkit-appearance: none;
  padding: 6px 28px 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background-color: var(--color-bg);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  font-size: var(--font-size-sm);
  color: var(--color-text);
  cursor: pointer;
  min-height: 36px;
}

.sort-select:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* 모바일: 프로필 카드 정렬 조정 */
@media (max-width: 480px) {
  .profile-card {
    gap: var(--space-3);
  }

  .logout-btn {
    margin-left: 0;
    align-self: flex-end;
  }
}
</style>
