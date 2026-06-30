import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import KnowledgeView from '@/views/KnowledgeView.vue'
import { knowledgeApi } from '@/api/knowledge'

vi.mock('@/api/knowledge', () => ({
  knowledgeApi: {
    list: vi.fn().mockResolvedValue({ data: { data: { records: [], total: 0 } } }),
    getTags: vi.fn().mockResolvedValue({ data: { data: [] } }),
    add: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    parseUrl: vi.fn(),
    parsePdf: vi.fn(),
    exportJson: vi.fn(),
    exportMarkdown: vi.fn(),
    exportCsv: vi.fn(),
    importJson: vi.fn(),
    previewImport: vi.fn(),
    batchDelete: vi.fn(),
    batchTag: vi.fn(),
    batchExport: vi.fn(),
    reprocess: vi.fn(),
    getRelated: vi.fn(),
    search: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getProcessLogs: vi.fn(),
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}))

const stubs = {
  NoteCard: { template: '<div class="mock-card" />', props: ['note'] },
  NoteEditorModal: { template: '<div class="mock-editor" />' },
  SearchResultItem: { template: '<div class="mock-search-item" />', props: ['result', 'keyword'] },
}

function flush() {
  return new Promise(r => setTimeout(r, 50))
}

describe('KnowledgeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    knowledgeApi.list.mockResolvedValue({ data: { data: { records: [], total: 0 } } })
    knowledgeApi.search.mockResolvedValue({ data: { data: [] } })
  })

  it('renders heading', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.text()).toContain('还没有笔记')
  })

  it('shows empty state when no notes', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.text()).toContain('还没有笔记')
  })

  it('calls list endpoint on mount when no keyword (browse mode)', async () => {
    mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(knowledgeApi.list).toHaveBeenCalled()
    expect(knowledgeApi.search).not.toHaveBeenCalled()
  })

  it('calls search endpoint when keyword submitted', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    knowledgeApi.list.mockClear()
    knowledgeApi.search.mockClear()

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    expect(knowledgeApi.search).toHaveBeenCalledWith('架构', expect.objectContaining({ topN: expect.any(Number) }))
    expect(knowledgeApi.list).not.toHaveBeenCalled()
  })

  it('switches back to list endpoint when search is cleared', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    knowledgeApi.list.mockClear()
    knowledgeApi.search.mockClear()

    await wrapper.find('.search-clear').trigger('click')
    await flush()

    expect(knowledgeApi.list).toHaveBeenCalled()
    expect(knowledgeApi.search).not.toHaveBeenCalled()
  })

  it('renders SearchResultItem (not NoteCard) in search mode', async () => {
    knowledgeApi.search.mockResolvedValue({
      data: { data: [{ id: 1, title: '架构', content: '内容', similarity: 0.82 }] }
    })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    expect(wrapper.find('.mock-search-item').exists()).toBe(true)
    expect(wrapper.find('.mock-card').exists()).toBe(false)
  })
})
