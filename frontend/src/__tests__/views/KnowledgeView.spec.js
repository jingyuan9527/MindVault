import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import KnowledgeView from '@/views/KnowledgeView.vue'

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
    search: vi.fn(),
    getProcessLogs: vi.fn(),
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}))

describe('KnowledgeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders heading', async () => {
    const wrapper = mount(KnowledgeView, {
      global: {
        stubs: {
          NoteCard: { template: '<div class="mock-card" />', props: ['note'] },
          NoteEditorModal: { template: '<div class="mock-editor" />' },
        }
      }
    })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('还没有笔记')
  })

  it('shows empty state when no notes', async () => {
    const wrapper = mount(KnowledgeView, {
      global: {
        stubs: {
          NoteCard: { template: '<div class="mock-card" />' },
          NoteEditorModal: { template: '<div class="mock-editor" />' },
        }
      }
    })
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('还没有笔记')
  })
})