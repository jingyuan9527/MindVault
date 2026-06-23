import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import DailyReviewView from '@/views/DailyReviewView.vue'

vi.mock('@/api/dailyReview', () => ({
  dailyReviewApi: {
    getLatest: vi.fn().mockResolvedValue({ data: { data: {
      id: 1, reportDate: '2024-06-15', totalCount: 5,
      summary: '今天学习了 Spring Boot',
      keyInsights: '["Spring 依赖注入很有用","AOP 切面编程"]',
      recommendations: '["尝试学习 Spring Security"]',
      categoryBreakdown: '{"Java": 3, "Spring": 2}'
    } } }),
    getByDate: vi.fn(),
    getRecent: vi.fn().mockResolvedValue({ data: { data: [
      { id: 1, reportDate: '2024-06-15', totalCount: 5 }
    ] } }),
    generate: vi.fn(),
  }
}))

describe('DailyReviewView', () => {
  it('renders heading', async () => {
    const wrapper = mount(DailyReviewView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('每日复盘')
  })

  it('renders report after loading', async () => {
    const wrapper = mount(DailyReviewView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('2024-06-15 复盘')
    expect(wrapper.text()).toContain('Spring Boot')
    expect(wrapper.text()).toContain('Spring 依赖注入')
    expect(wrapper.text()).toContain('Spring Security')
  })

  it('renders category breakdown', async () => {
    const wrapper = mount(DailyReviewView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('知识分类')
    expect(wrapper.text()).toContain('Java: 3')
    expect(wrapper.text()).toContain('Spring: 2')
  })
})