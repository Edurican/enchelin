<template>
  <div class="profile-page container">
    <LoadingSpinner v-if="pageLoading" message="프로필을 불러오는 중..." />
    <ErrorMessage v-else-if="pageError" :message="pageError" :retry="true" @retry="loadProfile" />
    <template v-else-if="profile">
      <section class="profile-card card">
        <img
          v-if="profile.avatarUrl"
          :src="profile.avatarUrl"
          alt="프로필 이미지"
          class="profile-avatar"
        />
        <div v-else class="profile-avatar profile-avatar--placeholder">👤</div>
        <div class="profile-info">
          <h1 class="profile-name">{{ profile.nickname }}</h1>
          <a
            v-if="profile.htmlUrl"
            :href="profile.htmlUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="profile-github"
          >
            GitHub 프로필
          </a>
        </div>
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

      <section class="user-reviews">
        <div class="reviews-header">
          <h2 class="section-title">리뷰</h2>
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
        <ErrorMessage v-else-if="error" :message="error" :retry="true" @retry="loadReviews" />
        <template v-else>
          <ReviewList
            :reviews="reviews"
            :has-next="hasNext"
            :loading="loading"
            :is-owner-fn="() => false"
            @load-more="loadMore"
          />
        </template>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchUserProfile } from '@/api/user'
import { fetchUserReviews, fetchUserStats } from '@/api/review'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ReviewList from '@/components/review/ReviewList.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref(null)
const stats = ref(null)
const reviews = ref([])
const pageLoading = ref(true)
const pageError = ref('')
const loading = ref(false)
const error = ref('')
const hasNext = ref(false)
const offset = ref(0)
const LIMIT = 10
const sort = ref('latest')

const userId = Number(route.params.userId)

async function loadProfile() {
  pageLoading.value = true
  pageError.value = ''
  try {
    profile.value = await fetchUserProfile(userId)
    stats.value = await fetchUserStats(userId).catch(() => null)
  } catch {
    pageError.value = '프로필을 불러올 수 없습니다.'
  } finally {
    pageLoading.value = false
  }
}

async function loadReviews() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchUserReviews(userId, offset.value, LIMIT, sort.value)
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
  loadReviews()
}

function loadMore() {
  offset.value += LIMIT
  loadReviews()
}

onMounted(async () => {
  if (auth.isAuthenticated && auth.user?.id && String(auth.user.id) === String(userId)) {
    router.replace({ name: 'MyPage' })
    return
  }
  await loadProfile()
  loadReviews()
})
</script>

<style scoped>
.profile-page {
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
</style>
