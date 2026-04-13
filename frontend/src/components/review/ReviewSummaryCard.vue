<template>
  <section v-if="show" class="summary-card card">
    <h2 class="summary-title">AI 리뷰 요약</h2>

    <div v-if="loading" class="skeleton-list">
      <div v-for="n in 3" :key="n" class="skeleton-row" />
    </div>

    <template v-else-if="status === 'available'">
      <div class="category-list">
        <div
          v-for="cat in visibleCategories"
          :key="cat.key"
          class="category-row"
        >
          <span class="category-label">{{ cat.label }}</span>
          <div class="tag-list">
            <span
              v-for="(item, i) in cat.items"
              :key="i"
              class="tag-chip"
            >
              {{ item.tag }}
              <span class="mention-count">{{ item.evidence_review_ids.length }}명 언급</span>
            </span>
          </div>
        </div>
      </div>
      <p class="disclaimer">AI가 생성한 요약입니다</p>
    </template>

    <p v-else class="unavailable-msg">{{ statusMessage }}</p>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchReviewSummary } from '@/api/restaurant'

const props = defineProps({
  restaurantId: { type: [Number, String], required: true },
})

const loading = ref(true)
const status = ref(null)
const summary = ref(null)
const statusMessage = ref('')

const CATEGORY_ORDER = [
  { key: 'signature_menu', label: '대표 메뉴' },
  { key: 'taste', label: '맛' },
  { key: 'atmosphere', label: '분위기' },
  { key: 'service', label: '서비스' },
  { key: 'cost_performance', label: '가성비' },
  { key: 'portion', label: '양' },
  { key: 'visit_purpose', label: '방문 목적' },
  { key: 'companion', label: '동반인' },
  { key: 'parking', label: '주차' },
]

const show = computed(
  () => loading.value || ['available', 'insufficient', 'unavailable'].includes(status.value),
)

const visibleCategories = computed(() => {
  if (!summary.value) return []
  return CATEGORY_ORDER
    .filter((cat) => summary.value[cat.key] != null && summary.value[cat.key].length > 0)
    .map((cat) => ({ ...cat, items: summary.value[cat.key] }))
})

onMounted(async () => {
  try {
    const data = await fetchReviewSummary(props.restaurantId)
    status.value = data.status
    summary.value = data.summary ?? null
    statusMessage.value = data.message ?? ''
  } catch {
    // 요약 로드 실패 시 카드 비노출
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.summary-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.summary-title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.skeleton-row {
  height: 1.25rem;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--color-bg-secondary) 25%, var(--color-border) 50%, var(--color-bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-row:nth-child(2) {
  width: 80%;
}

.skeleton-row:nth-child(3) {
  width: 60%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.category-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-2);
}

.category-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  min-width: 5rem;
  flex-shrink: 0;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: 3px 10px;
  border-radius: var(--radius-full);
  background-color: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.mention-count {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.disclaimer {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-align: right;
}

.unavailable-msg {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
</style>
