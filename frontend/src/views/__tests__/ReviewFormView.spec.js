import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ReviewFormView from '../ReviewFormView.vue'
import * as reviewApi from '@/api/review'

// 공통 스텁
const stubs = {
  RatingInput: {
    template: '<div class="rating-input-stub" />',
    props: ['modelValue'],
    emits: ['update:modelValue'],
  },
  LoadingSpinner: { template: '<div />' },
  ErrorMessage: { template: '<div />' },
}

function makeRouter(query = {}, params = {}, routeName = 'ReviewCreate') {
  const routes = [
    { path: '/reviews/new', name: 'ReviewCreate', component: ReviewFormView },
    { path: '/reviews/:reviewId/edit', name: 'ReviewEdit', component: ReviewFormView },
  ]
  const router = createRouter({ history: createMemoryHistory(), routes })
  return router
}

vi.mock('@/api/review', () => ({
  createReview: vi.fn().mockResolvedValue({}),
  updateReview: vi.fn().mockResolvedValue({}),
  fetchRestaurantReviews: vi.fn().mockResolvedValue({
    contents: [{ reviewId: 42, rating: 3, comment: '보통이에요', visitNumber: 1 }],
    hasNext: false,
  }),
}))

vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

describe('ReviewFormView — kakaoApiId 경유 신규 작성', () => {
  it('kakaoApiId 쿼리 파라미터가 있으면 식당 미리보기 카드를 렌더한다', async () => {
    const router = makeRouter()
    await router.push({
      name: 'ReviewCreate',
      query: {
        kakaoApiId: '12345',
        name: '스타벅스 강남',
        category: '카페',
        address: '서울 강남구',
        placeUrl: 'https://place.map.kakao.com/12345',
        x: '127.0',
        y: '37.5',
      },
    })
    const wrapper = mount(ReviewFormView, {
      global: { plugins: [router], stubs },
    })
    await flushPromises()

    expect(wrapper.find('.restaurant-preview').exists()).toBe(true)
    expect(wrapper.find('.preview-name').text()).toBe('스타벅스 강남')
    expect(wrapper.find('.preview-meta').text()).toBe('카페')
  })

  it('kakaoApiId 쿼리 파라미터가 없으면 식당 미리보기 카드를 렌더하지 않는다', async () => {
    const router = makeRouter()
    await router.push({ name: 'ReviewCreate', query: { restaurantId: '99' } })
    const wrapper = mount(ReviewFormView, {
      global: { plugins: [router], stubs },
    })
    await flushPromises()

    expect(wrapper.find('.restaurant-preview').exists()).toBe(false)
  })

  it('kakaoApiId 경유 제출 시 스냅샷 필드를 포함해 createReview를 호출한다', async () => {
    const router = makeRouter()
    await router.push({
      name: 'ReviewCreate',
      query: {
        kakaoApiId: '12345',
        name: '스타벅스 강남',
        category: '카페',
        address: '서울 강남구',
        placeUrl: 'https://place.map.kakao.com/12345',
        x: '127.0276',
        y: '37.4979',
      },
    })

    const wrapper = mount(ReviewFormView, {
      global: { plugins: [router], stubs },
    })
    await flushPromises()

    // rating 강제 설정 (RatingInput 스텁 우회)
    wrapper.vm.rating = 4
    wrapper.vm.comment = '맛있어요'

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(reviewApi.createReview).toHaveBeenCalledWith(
      expect.objectContaining({
        kakaoApiId: '12345',
        name: '스타벅스 강남',
        category: '카페',
        address: '서울 강남구',
        rating: 4,
        comment: '맛있어요',
      }),
    )
  })
})

describe('ReviewFormView — 수정 모드', () => {
  it('수정 모드 진입 시 fetchRestaurantReviews를 호출하고 폼을 프리필한다', async () => {
    const router = makeRouter()
    await router.push({
      name: 'ReviewEdit',
      params: { reviewId: '42' },
      query: { restaurantId: '99' },
    })

    const wrapper = mount(ReviewFormView, {
      global: { plugins: [router], stubs },
    })
    await flushPromises()

    expect(reviewApi.fetchRestaurantReviews).toHaveBeenCalledWith('99', 0, 50)
    expect(wrapper.vm.rating).toBe(3)
    expect(wrapper.vm.comment).toBe('보통이에요')
  })
})

describe('ReviewFormView — 유효성 검사', () => {
  it('평점 미선택 시 에러 메시지를 표시한다', async () => {
    const router = makeRouter()
    await router.push({
      name: 'ReviewCreate',
      query: { kakaoApiId: '12345', name: '테스트' },
    })

    const wrapper = mount(ReviewFormView, {
      global: { plugins: [router], stubs },
    })
    await flushPromises()

    wrapper.vm.comment = '코멘트'
    // rating은 0 (기본값)
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.vm.errors.rating).toBeTruthy()
    expect(reviewApi.createReview).not.toHaveBeenCalled()
  })
})
