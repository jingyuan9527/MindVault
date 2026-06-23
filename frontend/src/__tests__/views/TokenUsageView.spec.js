import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TokenUsageView from '@/views/TokenUsageView.vue'

vi.mock('@/api/tokenUsage', () => ({
  tokenUsageApi: {
    getDaily: vi.fn().mockResolvedValue({ data: { data: [
      { date: '2024-06-15', provider: 'OPENAI', modelName: 'gpt-4', promptTokens: 500, completionTokens: 300, totalTokens: 800, requestCount: 2, cost: 0.02 }
    ] } }),
    getTotal: vi.fn(),
  }
}))

describe('TokenUsageView', () => {
  it('renders heading', () => {
    const wrapper = mount(TokenUsageView)
    expect(wrapper.text()).toContain('用量统计')
  })

  it('renders chart after data loads', async () => {
    const wrapper = mount(TokenUsageView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('OPENAI')
    expect(wrapper.text()).toContain('6/15')
  })
})