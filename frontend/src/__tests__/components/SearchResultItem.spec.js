import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchResultItem from '@/components/knowledge/SearchResultItem.vue'

describe('SearchResultItem', () => {
  const baseResult = {
    id: 1,
    title: '微服务架构设计',
    aiTitle: '微服务架构设计',
    content: '本文讨论微服务架构的核心模式，包括服务拆分、API 网关和分布式事务等架构实践。',
    summary: '微服务架构核心模式',
    similarity: 0.8234,
    tags: '["架构","微服务"]',
    userTags: '[]',
    createdAt: '2024-06-15T10:00:00Z'
  }

  it('renders title (aiTitle preferred)', () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    expect(wrapper.text()).toContain('微服务架构设计')
  })

  it('renders similarity score', () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    // 相似度分以百分比或原值展示，必须出现
    expect(wrapper.text()).toContain('82')
  })

  it('renders a snippet containing the keyword', () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    const text = wrapper.text()
    expect(text).toContain('架构')
    // snippet 应是内容片段，不是完整长文
    expect(text.length).toBeLessThan(200)
  })

  it('highlights the keyword in snippet', () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    // 高亮用 <mark> 标签
    expect(wrapper.html()).toContain('<mark')
    expect(wrapper.html()).toContain('架构</mark>')
  })

  it('falls back to summary when content has no keyword match', () => {
    const result = { ...baseResult, content: '完全无关的内容', summary: '微服务架构核心模式总结' }
    const wrapper = mount(SearchResultItem, {
      props: { result, keyword: '架构' }
    })
    expect(wrapper.text()).toContain('微服务架构核心模式总结')
  })

  it('renders tags', () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    expect(wrapper.text()).toContain('#架构')
    expect(wrapper.text()).toContain('#微服务')
  })

  it('emits click when row clicked', async () => {
    const wrapper = mount(SearchResultItem, {
      props: { result: baseResult, keyword: '架构' }
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')[0][0]).toEqual(baseResult)
  })

  it('handles missing similarity gracefully', () => {
    const result = { ...baseResult, similarity: null }
    const wrapper = mount(SearchResultItem, {
      props: { result, keyword: '架构' }
    })
    // 不应崩溃，也不应显示 undefined
    expect(wrapper.text()).not.toContain('undefined')
  })
})
