import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchBar from '@/components/map/SearchBar.vue'

describe('SearchBar', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // ─── 렌더링 ───────────────────────────────────────────────────
  it('input 요소가 렌더링된다', () => {
    // Arrange & Act
    const wrapper = mount(SearchBar)

    // Assert
    expect(wrapper.find('input[type="search"]').exists()).toBe(true)
  })

  it('초기값이 비어있으면 지우기 버튼이 보이지 않는다', () => {
    // Arrange & Act
    const wrapper = mount(SearchBar)

    // Assert
    expect(wrapper.find('.search-clear').exists()).toBe(false)
  })

  it('modelValue prop이 input에 반영된다', () => {
    // Arrange & Act
    const wrapper = mount(SearchBar, { props: { modelValue: '스타벅스' } })

    // Assert
    expect(wrapper.find('input').element.value).toBe('스타벅스')
  })

  // ─── 디바운스 검색 이벤트 ─────────────────────────────────────
  it('2자 이상 입력 후 300ms 뒤 search 이벤트를 emit한다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')

    // Act
    await input.setValue('스타벅스')
    await input.trigger('input')
    expect(wrapper.emitted('search')).toBeFalsy()

    vi.advanceTimersByTime(300)

    // Assert
    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')[0]).toEqual(['스타벅스'])
  })

  it('1자 입력 시 search 이벤트를 emit하지 않는다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)

    // Act
    await wrapper.find('input').setValue('스')
    await wrapper.find('input').trigger('input')
    vi.advanceTimersByTime(300)

    // Assert
    expect(wrapper.emitted('search')).toBeFalsy()
  })

  it('연속 입력 시 마지막 입력 기준 300ms 후에만 search가 emit된다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')

    // Act
    await input.setValue('스타')
    await input.trigger('input')
    vi.advanceTimersByTime(100)

    await input.setValue('스타벅스')
    await input.trigger('input')
    vi.advanceTimersByTime(300)

    // Assert
    expect(wrapper.emitted('search')).toHaveLength(1)
    expect(wrapper.emitted('search')[0]).toEqual(['스타벅스'])
  })

  // ─── Enter 키 즉시 검색 ───────────────────────────────────────
  it('Enter 키 입력 시 디바운스 없이 즉시 search를 emit한다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)
    const input = wrapper.find('input')

    // Act
    await input.setValue('한식당')
    await input.trigger('input')
    await input.trigger('keydown.enter')

    // Assert — 타이머 없이 즉시 발생
    expect(wrapper.emitted('search')).toBeTruthy()
    expect(wrapper.emitted('search')[0]).toEqual(['한식당'])
  })

  // ─── 지우기 버튼 ──────────────────────────────────────────────
  it('값이 있을 때 지우기 버튼이 표시된다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)

    // Act
    await wrapper.find('input').setValue('스타벅스')
    await wrapper.find('input').trigger('input')

    // Assert
    expect(wrapper.find('.search-clear').exists()).toBe(true)
  })

  it('지우기 버튼 클릭 시 clear 이벤트를 emit하고 input을 비운다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)
    await wrapper.find('input').setValue('스타벅스')
    await wrapper.find('input').trigger('input')

    // Act
    await wrapper.find('.search-clear').trigger('click')

    // Assert
    expect(wrapper.emitted('clear')).toBeTruthy()
    expect(wrapper.find('input').element.value).toBe('')
  })

  // ─── Escape 키 ────────────────────────────────────────────────
  it('Escape 키 입력 시 clear 이벤트를 emit한다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)
    await wrapper.find('input').setValue('테스트')
    await wrapper.find('input').trigger('input')

    // Act
    await wrapper.find('input').trigger('keydown.escape')

    // Assert
    expect(wrapper.emitted('clear')).toBeTruthy()
  })

  // ─── v-model ──────────────────────────────────────────────────
  it('입력 시 update:modelValue 이벤트를 emit한다', async () => {
    // Arrange
    const wrapper = mount(SearchBar)

    // Act
    await wrapper.find('input').setValue('맛집')
    await wrapper.find('input').trigger('input')

    // Assert
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual(['맛집'])
  })
})
