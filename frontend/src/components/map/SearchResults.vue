<template>
  <div v-if="visible" class="search-results">
    <div v-if="loading" class="search-results__state">
      <span class="search-results__spinner" />
      검색 중...
    </div>
    <ul v-else-if="results.length > 0" class="search-results__list">
      <li
        v-for="item in results"
        :key="item.kakaoApiId"
        class="search-results__item"
        @click="emit('select', item)"
      >
        <div class="search-results__name">{{ item.name }}</div>
        <div class="search-results__meta">
          <span class="search-results__category">{{ item.category }}</span>
          <span v-if="item.distance" class="search-results__distance">
            {{ formatDistance(item.distance) }}
          </span>
        </div>
        <div class="search-results__address">{{ item.address }}</div>
      </li>
    </ul>
    <div v-else class="search-results__state search-results__empty">
      검색 결과가 없습니다.
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  results: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

function formatDistance(meters) {
  if (!meters && meters !== 0) return ''
  const m = Number(meters)
  if (m >= 1000) return `${(m / 1000).toFixed(1)}km`
  return `${Math.round(m)}m`
}
</script>

<style scoped>
.search-results {
  position: absolute;
  top: calc(44px + var(--space-2));
  left: 0;
  right: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  max-height: 320px;
  overflow-y: auto;
  z-index: 20;
}

.search-results__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.search-results__item {
  padding: var(--space-3) var(--space-4);
  cursor: pointer;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.1s;
}

.search-results__item:last-child {
  border-bottom: none;
}

.search-results__item:hover {
  background: var(--color-bg-secondary, #f9fafb);
}

.search-results__name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium, 500);
  color: var(--color-text);
  margin-bottom: 2px;
}

.search-results__meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: 2px;
}

.search-results__category {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
}

.search-results__distance {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.search-results__address {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.search-results__state {
  padding: var(--space-4);
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

.search-results__spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
