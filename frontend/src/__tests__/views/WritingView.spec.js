import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import WritingView from '@/views/WritingView.vue'

vi.mock('@/api/writing', () => ({
  writingApi: {
    generate: vi.fn().mockResolvedValue({ data: { data: '# 测试文章\n\n这是正文。' } }),
  }
}))

describe('WritingView', () => {
  it('renders heading and extra text', () => {
    const wrapper = mount(WritingView)
    expect(wrapper.text()).toContain('写作辅助')
    expect(wrapper.text()).toContain('AI 会基于你的知识库内容生成文章')
  })

  it('generates article on button click', async () => {
    const wrapper = mount(WritingView)
    const vm = wrapper.vm
    vm.formData.topic = '测试主题'
    await vm.generate()
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('测试主题')
  })

  it('copy button appears after generation', async () => {
    const wrapper = mount(WritingView)
    const vm = wrapper.vm
    vm.formData.topic = 'Java'
    await vm.generate()
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('复制全文')
  })
})