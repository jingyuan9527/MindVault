import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/flashcard', () => ({
  flashcardApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [
      { id: 1, question: '测试问题', answer: '测试答案', difficulty: 'EASY' }
    ] } }),
    generate: vi.fn(),
    delete: vi.fn(),
  }
}))

vi.mock('@/api/knowledge', () => ({
  knowledgeApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [] } }),
  }
}))

const dialogMock = { warning: vi.fn(), info: vi.fn(), success: vi.fn(), error: vi.fn() }

vi.mock('naive-ui', () => {
  const comp = (name) => ({
    name,
    template: '<div><slot /><slot name="header" /><slot name="footer" /><slot name="default" /><slot name="extra" /><slot name="action" /></div>',
    props: { title: String, size: String }
  })
  return {
    useDialog: () => dialogMock,
    NCard: comp('NCard'),
    NButton: comp('NButton'),
    NSelect: comp('NSelect'),
    NTag: comp('NTag'),
    NSpace: comp('NSpace'),
    NModal: comp('NModal'),
    NInput: comp('NInput'),
    NSpin: comp('NSpin'),
    NEmpty: comp('NEmpty'),
    NAlert: comp('NAlert'),
    NDataTable: comp('NDataTable'),
    NCheckbox: comp('NCheckbox'),
    NSkeleton: comp('NSkeleton'),
    NRadioGroup: comp('NRadioGroup'),
    NRadio: comp('NRadio'),
    NForm: comp('NForm'),
    NFormItem: comp('NFormItem'),
    NInputNumber: comp('NInputNumber'),
    NButtonGroup: comp('NButtonGroup'),
    NDivider: comp('NDivider'),
  }
})

const FlashCardView = (await import('@/views/FlashCardView.vue')).default

describe('FlashCardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders heading', async () => {
    const wrapper = mount(FlashCardView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('知识卡片')
  })

  it('renders cards after loading', async () => {
    const wrapper = mount(FlashCardView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('测试问题')
    expect(wrapper.text()).toContain('测试答案')
  })
})