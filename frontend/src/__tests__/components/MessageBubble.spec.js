import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageBubble from '@/components/chat/MessageBubble.vue'

describe('MessageBubble', () => {
  it('renders user message on the right', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: 'Hello', isUser: true, time: '2024-01-01' }
    })
    expect(wrapper.find('.justify-end').exists()).toBe(true)
    expect(wrapper.text()).toContain('Hello')
  })

  it('renders assistant message on the left', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: 'Hi there', isUser: false, time: '2024-01-01' }
    })
    expect(wrapper.find('.justify-start').exists()).toBe(true)
    expect(wrapper.text()).toContain('Hi there')
  })

  it('displays the formatted timestamp', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: 'test', isUser: true, time: '2024-06-15 14:30' }
    })
    expect(wrapper.text()).toContain('2024-06-15 14:30')
  })

  it('renders source citations when sources are provided', () => {
    const sources = JSON.stringify([
      { id: 1, title: 'Article 1', url: 'http://example.com/1' },
      { id: 2, title: 'Article 2', url: 'http://example.com/2' }
    ])
    const wrapper = mount(MessageBubble, {
      props: { message: 'Answer with sources', isUser: false, time: '12:00', sources }
    })
    expect(wrapper.text()).toContain('Article 1')
    expect(wrapper.text()).toContain('Article 2')
  })

  it('does not render sources section when sources is empty array', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: 'No sources', isUser: true, time: '12:00', sources: '[]' }
    })
    expect(wrapper.text()).not.toContain('知识')
  })

  it('handles invalid sources JSON gracefully', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: 'Bad sources', isUser: false, time: '12:00', sources: 'not-json' }
    })
    expect(wrapper.text()).not.toContain('知识')
  })

  it('renders fallback text when source has no title', () => {
    const sources = JSON.stringify([
      { id: 5 }
    ])
    const wrapper = mount(MessageBubble, {
      props: { message: 'test', isUser: false, time: '12:00', sources }
    })
    expect(wrapper.text()).toContain('知识 #5')
  })

  it('renders source link with # when url is missing', () => {
    const sources = JSON.stringify([
      { id: 1, title: 'No URL' }
    ])
    const wrapper = mount(MessageBubble, {
      props: { message: 'test', isUser: false, time: '12:00', sources }
    })
    expect(wrapper.text()).toContain('No URL')
  })

  it('renders blocked message with warning style', () => {
    const wrapper = mount(MessageBubble, {
      props: { message: '消息包含受限内容', blocked: true, time: '12:00' }
    })
    expect(wrapper.find('.justify-center').exists()).toBe(true)
    expect(wrapper.text()).toContain('消息包含受限内容')
  })

  
})