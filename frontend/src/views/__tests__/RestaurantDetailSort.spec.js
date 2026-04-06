import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import RestaurantDetailView from '../RestaurantDetailView.vue'
import * as reviewApi from '@/api/review'

vi.mock('@/api/review', () => ({
  fetchRestaurantReviews: vi.fn().mockResolvedValue({
    contents: [{ reviewId: 1, userName: '유저', rating: 5, comment: '최고', visitNumber: 1, createdAt: '2024-01-01T00:00:00' }],
    hasNext: false,
  }),
  deleteReview: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/restaurant', () => ({
  fetchRestaurantDetail: vi.fn().mockResolvedValue({
    id: 10,
    kakaoApiId: 'abc',
    name: '테스트 식당',
    category: '한식',
    address: '서울시',
    placeUrl: '',
    x: 127.0,
    y: 37.5,
  }),
}))

vi.mock('@/stores/restaurant', () => ({
  useRestaurantStore: () => ({ selected: null }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({ isAuthenticated: true, user: { nickname: '유저' } }),
}))

vi.mock('@/composables/useToast', () => ({
  useToast: () => ({ success: vi.fn(), error: vi.fn() }),
}))

const stubs = {
  LoadingSpinner: { template: '<div />' },
  ErrorMessage: { template: '<div />' },
  ConfirmDialog: { template: '<div />' },
  ReviewList: {
    template: '<div class="review-list-stub" />',
    props: ['reviews', 'hasNext', 'loading', 'isOwnerFn'],
    emits: ['edit', 'delete', 'load-more'],
  },
  StarRating: { template: '<div />' },
  RouterLink: { template: '<a><slot /></a>' },
}

function makeRouter() {
  const routes = [{ path: '/restaurants/:id', name: 'RestaurantDetail', component: RestaurantDetailView }]
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('RestaurantDetailView — 리뷰 정렬', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    reviewApi.fetchRestaurantReviews.mockResolvedValue({
      contents: [{ reviewId: 1, userName: '유저', rating: 5, comment: '최고', visitNumber: 1, createdAt: '2024-01-01T00:00:00' }],
      hasNext: true,
    })
  })

  it('마운트 시 기본 정렬 latest 로 fetchRestaurantReviews를 호출한다', async () => {
    const router = makeRouter()
    await router.push('/restaurants/10')

    mount(RestaurantDetailView, { global: { plugins: [router], stubs } })
    await flushPromises()

    expect(reviewApi.fetchRestaurantReviews).toHaveBeenCalledWith(10, 0, 10, 'latest')
  })

  it('정렬 드롭다운을 rating_desc 로 변경하면 offset 0 으로 재조회한다', async () => {
    const router = makeRouter()
    await router.push('/restaurants/10')

    const wrapper = mount(RestaurantDetailView, { global: { plugins: [router], stubs } })
    await flushPromises()

    vi.clearAllMocks()
    reviewApi.fetchRestaurantReviews.mockResolvedValue({ contents: [], hasNext: false })

    const select = wrapper.find('.sort-select')
    await select.setValue('rating_desc')
    await select.trigger('change')
    await flushPromises()

    expect(reviewApi.fetchRestaurantReviews).toHaveBeenCalledWith(
      expect.anything(),
      0,
      10,
      'rating_desc',
    )
  })

  it('정렬 드롭다운을 rating_asc 로 변경하면 rating_asc 로 호출한다', async () => {
    const router = makeRouter()
    await router.push('/restaurants/10')

    const wrapper = mount(RestaurantDetailView, { global: { plugins: [router], stubs } })
    await flushPromises()

    vi.clearAllMocks()
    reviewApi.fetchRestaurantReviews.mockResolvedValue({ contents: [], hasNext: false })

    const select = wrapper.find('.sort-select')
    await select.setValue('rating_asc')
    await select.trigger('change')
    await flushPromises()

    expect(reviewApi.fetchRestaurantReviews).toHaveBeenCalledWith(
      expect.anything(),
      0,
      10,
      'rating_asc',
    )
  })
})
