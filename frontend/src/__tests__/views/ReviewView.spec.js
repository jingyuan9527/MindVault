import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ReviewView from '@/views/ReviewView.vue'

vi.mock('@/api/review', () => ({
  reviewApi: {
    getDue: vi.fn().mockResolvedValue({ data: { data: [
      { knowledgeId: 1, title: 'Test Knowledge', content: 'Some content', tags: '["java","spring"]', intervalDays: 3, reviewCount: 2, easeFactor: 2.5 }
    ] } }),
    getDueCount: vi.fn().mockResolvedValue({ data: { data: { count: 1 } } }),
    perform: vi.fn(),
  }
}))

describe('ReviewView', () => {
  it('renders heading and due count', async () => {
    const wrapper = mount(ReviewView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('间隔复习')
    expect(wrapper.text()).toContain('1 条待复习')
  })

  it('shows first review item after loading', async () => {
    const wrapper = mount(ReviewView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('Test Knowledge')
    expect(wrapper.text()).toContain('Some content')
    expect(wrapper.text()).toContain('1 / 1')
  })
})