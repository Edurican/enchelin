import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewCard from '../ReviewCard.vue'

const baseReview = {
  reviewId: 1,
  userName: '테스터',
  rating: 4,
  comment: '맛있어요',
  createdAt: '2024-03-15T12:00:00',
  visitNumber: 0,
}

describe('ReviewCard — visitNumber 뱃지', () => {
  it('visitNumber가 양수이면 뱃지를 렌더한다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, visitNumber: 3 }, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.find('.visit-badge').exists()).toBe(true)
    expect(wrapper.find('.visit-badge').text()).toBe('3번째 방문')
  })

  it('visitNumber가 1이면 "1번째 방문"을 표시한다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, visitNumber: 1 }, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.find('.visit-badge').text()).toBe('1번째 방문')
  })

  it('visitNumber가 0이면 뱃지를 렌더하지 않는다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, visitNumber: 0 }, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.find('.visit-badge').exists()).toBe(false)
  })

  it('visitNumber 필드가 없으면 뱃지를 렌더하지 않는다', () => {
    const { visitNumber: _, ...reviewWithout } = baseReview
    const wrapper = mount(ReviewCard, {
      props: { review: reviewWithout, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.find('.visit-badge').exists()).toBe(false)
  })
})

describe('ReviewCard — 소유권 버튼', () => {
  it('isOwner=true 이면 수정/삭제 버튼을 표시한다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, visitNumber: 1 }, isOwner: true },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.text()).toContain('수정')
    expect(wrapper.text()).toContain('삭제')
  })

  it('isOwner=false 이면 수정/삭제 버튼을 표시하지 않는다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, visitNumber: 1 }, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.text()).not.toContain('수정')
    expect(wrapper.text()).not.toContain('삭제')
  })
})

describe('ReviewCard — 날짜 포맷', () => {
  it('createdAt을 YYYY.MM.DD 형식으로 표시한다', () => {
    const wrapper = mount(ReviewCard, {
      props: { review: { ...baseReview, createdAt: '2024-03-15T12:00:00' }, isOwner: false },
      global: { stubs: { StarRating: true } },
    })
    expect(wrapper.find('time').text()).toBe('2024.03.15')
  })
})
