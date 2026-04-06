<template>
  <div class="search-bar">
    <div class="search-input-wrap">
      <input
        ref="inputRef"
        v-model="query"
        class="search-input"
        type="search"
        placeholder="식당 이름으로 검색"
        autocomplete="off"
        @input="onInput"
        @keydown.enter.prevent="onEnter"
        @keydown.escape="onEscape"
        @focus="showResults = true"
      />
      <button
        v-if="query"
        class="search-clear"
        type="button"
        aria-label="검색어 지우기"
        @click="onClear"
      >
        ✕
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

const inputRef = ref(null)
const query = ref(props.modelValue)
const showResults = ref(false)

let debounceTimer = null

watch(
  () => props.modelValue,
  (val) => {
    query.value = val
  },
)

function onInput() {
  emit('update:modelValue', query.value)
  clearTimeout(debounceTimer)
  if (query.value.length >= 2) {
    debounceTimer = setTimeout(() => {
      emit('search', query.value)
    }, 300)
  } else if (query.value.length === 0) {
    emit('clear')
  }
}

function onEnter() {
  if (query.value.length >= 2) {
    clearTimeout(debounceTimer)
    emit('search', query.value)
  }
}

function onEscape() {
  emit('clear')
  inputRef.value?.blur()
}

function onClear() {
  query.value = ''
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

function focus() {
  inputRef.value?.focus()
}

defineExpose({ focus })
</script>

<style scoped>
.search-bar {
  width: 100%;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 100%;
  height: 44px;
  padding: 0 var(--space-4);
  padding-right: 36px;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  background: var(--color-bg);
  color: var(--color-text);
  outline: none;
  transition: border-color 0.15s;
}

.search-input:focus {
  border-color: var(--color-primary);
}

.search-input::-webkit-search-cancel-button {
  display: none;
}

.search-clear {
  position: absolute;
  right: var(--space-3);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1;
  padding: 2px;
}

.search-clear:hover {
  color: var(--color-text);
}
</style>
