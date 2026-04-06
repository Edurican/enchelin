import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchResults from '@/components/map/SearchResults.vue'

const fixtureResults = [
  {
    kakaoApiId: 'k1',
    name: '스타벅스 강남점',
    category: '카페',
    address: '서울 강남구 테헤란로',
    x: '127.027',
    y: '37.498',
    distance: '120',
  },
  {
    kakaoApiId: 'k2',
    name: '한식당',
    category: '한식',
    address: '서울 강남구 역삼동',
    x: '127.028',
    y: '37.499',
    distance: '1500',
  },
]

describe('SearchResults', () => {
  // ─── visible 조건 ─────────────────────────────────────────────
  it('visible=false 이면 아무것도 렌더링하지 않는다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: fixtureResults, visible: false },
    })

    // Assert
    expect(wrapper.find('.search-results').exists()).toBe(false)
  })

  it('visible=true 이면 컨테이너가 렌더링된다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: fixtureResults, visible: true },
    })

    // Assert
    expect(wrapper.find('.search-results').exists()).toBe(true)
  })

  // ─── 로딩 상태 ────────────────────────────────────────────────
  it('loading=true 이면 로딩 텍스트를 표시한다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: [], loading: true, visible: true },
    })

    // Assert
    expect(wrapper.text()).toContain('검색 중')
    expect(wrapper.find('.search-results__list').exists()).toBe(false)
  })

  // ─── 결과 목록 ────────────────────────────────────────────────
  it('results 배열만큼 항목을 렌더링한다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: fixtureResults, visible: true },
    })

    // Assert
    const items = wrapper.findAll('.search-results__item')
    expect(items).toHaveLength(2)
  })

  it('각 항목에 이름과 카테고리가 표시된다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: fixtureResults, visible: true },
    })

    // Assert
    const items = wrapper.findAll('.search-results__item')
    expect(items[0].find('.search-results__name').text()).toBe('스타벅스 강남점')
    expect(items[0].find('.search-results__category').text()).toBe('카페')
  })

  // ─── 거리 포맷 ────────────────────────────────────────────────
  it('distance가 1000 미만이면 m 단위로 표시한다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: [fixtureResults[0]], visible: true },
    })

    // Assert — 120m
    expect(wrapper.find('.search-results__distance').text()).toBe('120m')
  })

  it('distance가 1000 이상이면 km 단위로 표시한다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: [fixtureResults[1]], visible: true },
    })

    // Assert — 1500 → 1.5km
    expect(wrapper.find('.search-results__distance').text()).toBe('1.5km')
  })

  // ─── 빈 결과 ──────────────────────────────────────────────────
  it('results가 비어있으면 empty 메시지를 표시한다', () => {
    // Arrange & Act
    const wrapper = mount(SearchResults, {
      props: { results: [], visible: true },
    })

    // Assert
    expect(wrapper.text()).toContain('검색 결과가 없습니다')
    expect(wrapper.find('.search-results__list').exists()).toBe(false)
  })

  // ─── select 이벤트 ────────────────────────────────────────────
  it('항목 클릭 시 select 이벤트에 해당 item을 emit한다', async () => {
    // Arrange
    const wrapper = mount(SearchResults, {
      props: { results: fixtureResults, visible: true },
    })

    // Act
    await wrapper.findAll('.search-results__item')[0].trigger('click')

    // Assert
    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')[0][0]).toMatchObject({ kakaoApiId: 'k1', name: '스타벅스 강남점' })
  })
})
