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
      <button class="btn btn-sm btn-secondary" @click="onLogout">로그아웃</button>
    </section>

    <section class="my-reviews">
      <h2 class="section-title">내 리뷰</h2>

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
import { fetchUserReviews, deleteReview } from '@/api/review'
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

async function loadMyReviews() {
  if (!auth.user?.id) return
  loading.value = true
  error.value = ''
  try {
    const page = await fetchUserReviews(auth.user.id, offset.value, LIMIT)
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

onMounted(() => {
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
}

.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
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
}

.profile-github {
  font-size: var(--font-size-sm);
  color: var(--color-primary);
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  margin-bottom: var(--space-3);
}
</style>
