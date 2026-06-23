import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ChatView from '@/views/ChatView.vue'

vi.mock('@/api/chat', () => ({
  chatApi: {
    listSessions: vi.fn(),
    createSession: vi.fn(),
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
    sendMessageStream: vi.fn(),
  }
}))

describe('ChatView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders heading', () => {
    const wrapper = mount(ChatView, {
      global: {
        stubs: {
          MessageBubble: { template: '<div class="mock-bubble" />' },
          ChatInput: { template: '<div class="mock-input" />' },
          ThinkingIndicator: { template: '<div class="mock-thinking" />' },
        }
      }
    })
    expect(wrapper.text()).toContain('AI 对话')
  })

  it('shows empty state when no messages', () => {
    const wrapper = mount(ChatView, {
      global: {
        stubs: {
          MessageBubble: { template: '<div class="mock-bubble" />' },
          ChatInput: { template: '<div class="mock-input" />' },
          ThinkingIndicator: { template: '<div class="mock-thinking" />' },
        }
      }
    })
    expect(wrapper.text()).toContain('开始你的第一段对话')
    expect(wrapper.text()).toContain('总结知识点')
  })
})