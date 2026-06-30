import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import KnowledgeView from '@/views/KnowledgeView.vue'
import { knowledgeApi } from '@/api/knowledge'

vi.mock('@/api/knowledge', () => ({
  knowledgeApi: {
    list: vi.fn().mockResolvedValue({ data: { data: { records: [], total: 0 } } }),
    getById: vi.fn(),
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
    getRelated: vi.fn().mockResolvedValue({ data: { data: [] } }),
    search: vi.fn().mockResolvedValue({ data: { data: [] } }),
    getProcessLogs: vi.fn(),
  }
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ replace: vi.fn() }),
}))

const stubs = {
  NoteCard: {
    name: 'NoteCard',
    template: '<div class="mock-card" :class="{ selected }" @click="$emit(\'click\', note)" />',
    props: ['note', 'selected'],
  },
  NoteEditorModal: { template: '<div class="mock-editor" v-if="visible" />', props: ['visible', 'note'] },
  SearchResultItem: {
    name: 'SearchResultItem',
    template: '<div class="mock-search-item" :class="{ selected }" @click="$emit(\'click\', result)" />',
    props: ['result', 'keyword', 'selected'],
  },
  NoteDrawer: {
    name: 'NoteDrawer',
    template: `<div class="mock-drawer" v-if="visible">
      <button class="mock-navigate" @click="$emit('navigate', { id: 99, title: 'Related' })" />
      <button class="mock-back" @click="$emit('back')" />
      <button class="mock-edit-btn" @click="$emit('edit')" />
      <button class="mock-close-btn" @click="$emit('update:visible', false)" />
    </div>`,
    props: ['visible', 'note', 'relatedNotes', 'canGoBack'],
  },
}

function flush() {
  return new Promise(r => setTimeout(r, 50))
}

function mockListWithNotes(notes) {
  knowledgeApi.list.mockResolvedValue({ data: { data: { records: notes, total: notes.length } } })
}

const sampleNotes = [
  { id: 1, title: 'Note 1', aiTitle: 'AI Note 1', content: 'Content 1', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' },
  { id: 2, title: 'Note 2', aiTitle: 'AI Note 2', content: 'Content 2', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' },
]

describe('KnowledgeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    knowledgeApi.list.mockResolvedValue({ data: { data: { records: [], total: 0 } } })
    knowledgeApi.search.mockResolvedValue({ data: { data: [] } })
    knowledgeApi.getRelated.mockResolvedValue({ data: { data: [] } })
    knowledgeApi.getById.mockResolvedValue({ data: { data: {} } })
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

  // ===== Drawer integration tests =====

  it('opens drawer (not editor) when clicking a note in browse mode', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()

    expect(wrapper.find('.mock-drawer').exists()).toBe(true)
    expect(wrapper.find('.mock-editor').exists()).toBe(false)
  })

  it('fetches related notes when drawer opens', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()

    expect(knowledgeApi.getRelated).toHaveBeenCalledWith(1, expect.any(Number))
  })

  it('highlights the selected note in the list', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    const cards = wrapper.findAll('.mock-card')
    await cards[0].trigger('click')
    await flush()

    expect(cards[0].classes()).toContain('selected')
    expect(cards[1].classes()).not.toContain('selected')
  })

  it('fetches full note by ID when navigating to a related note', async () => {
    mockListWithNotes(sampleNotes)
    knowledgeApi.getById.mockResolvedValue({ data: { data: { id: 99, title: 'Related', content: 'Full content' } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()
    knowledgeApi.getById.mockClear()

    await wrapper.find('.mock-navigate').trigger('click')
    await flush()

    expect(knowledgeApi.getById).toHaveBeenCalledWith(99)
  })

  it('shows back button after navigating to a related note', async () => {
    mockListWithNotes(sampleNotes)
    knowledgeApi.getById.mockResolvedValue({ data: { data: { id: 99, title: 'Related', content: 'Full' } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()
    await wrapper.find('.mock-navigate').trigger('click')
    await flush()

    const drawer = wrapper.findComponent({ name: 'NoteDrawer' })
    expect(drawer.props('canGoBack')).toBe(true)
  })

  it('goes back to previous note when back button clicked', async () => {
    mockListWithNotes(sampleNotes)
    knowledgeApi.getById.mockResolvedValue({ data: { data: { id: 99, title: 'Related', content: 'Full' } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()
    await wrapper.find('.mock-navigate').trigger('click')
    await flush()

    const drawer = wrapper.findComponent({ name: 'NoteDrawer' })
    expect(drawer.props('canGoBack')).toBe(true)

    await wrapper.find('.mock-back').trigger('click')
    await flush()

    const drawerAfter = wrapper.findComponent({ name: 'NoteDrawer' })
    expect(drawerAfter.props('canGoBack')).toBe(false)
    expect(drawerAfter.props('note').id).toBe(1)
  })

  it('opens editor modal when edit button clicked from drawer', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()
    await wrapper.find('.mock-edit-btn').trigger('click')
    await flush()

    expect(wrapper.find('.mock-editor').exists()).toBe(true)
  })

  it('closes drawer when close button clicked', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-card').trigger('click')
    await flush()
    expect(wrapper.find('.mock-drawer').exists()).toBe(true)

    await wrapper.find('.mock-close-btn').trigger('click')
    await flush()

    expect(wrapper.find('.mock-drawer').exists()).toBe(false)
  })
})
