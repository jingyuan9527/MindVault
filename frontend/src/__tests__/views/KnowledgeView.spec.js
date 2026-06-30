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
    searchRewrite: vi.fn().mockResolvedValue({ data: { data: [] } }),
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
  NoteListItem: {
    name: 'NoteListItem',
    template: '<div class="mock-list-item" :class="{ highlighted }" @click="$emit(\'click\', note)" />',
    props: ['note', 'highlighted'],
  },
  NoteEditorModal: {
    template: '<div class="mock-editor" v-if="visible"><button class="mock-editor-save" @click="$emit(\'saved\')" /></div>',
    props: ['visible', 'note'],
    emits: ['saved'],
  },
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
    localStorage.clear()
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1200 })
    knowledgeApi.list.mockResolvedValue({ data: { data: { records: [], total: 0 } } })
    knowledgeApi.search.mockResolvedValue({ data: { data: [] } })
    knowledgeApi.searchRewrite.mockResolvedValue({ data: { data: [] } })
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

  // ===== Density toggle tests =====

  it('renders NoteListItem (not NoteCard) by default in browse mode', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    expect(wrapper.find('.mock-list-item').exists()).toBe(true)
    expect(wrapper.find('.mock-card').exists()).toBe(false)
  })

  it('switches to NoteCard when card density toggle clicked', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.density-card').trigger('click')
    await flush()

    expect(wrapper.find('.mock-card').exists()).toBe(true)
    expect(wrapper.find('.mock-list-item').exists()).toBe(false)
  })

  it('switches back to NoteListItem when list density toggle clicked', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.density-card').trigger('click')
    await flush()
    await wrapper.find('.density-list').trigger('click')
    await flush()

    expect(wrapper.find('.mock-list-item').exists()).toBe(true)
    expect(wrapper.find('.mock-card').exists()).toBe(false)
  })

  it('persists density preference in localStorage', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.density-card').trigger('click')
    await flush()

    expect(localStorage.getItem('knowledge-density')).toBe('card')
  })

  // ===== Drawer integration tests (use NoteListItem as default) =====

  it('opens drawer (not editor) when clicking a note in browse mode', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()

    expect(wrapper.find('.mock-drawer').exists()).toBe(true)
    expect(wrapper.find('.mock-editor').exists()).toBe(false)
  })

  it('fetches related notes when drawer opens', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()

    expect(knowledgeApi.getRelated).toHaveBeenCalledWith(1, expect.any(Number))
  })

  it('highlights the selected note in the list', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    const items = wrapper.findAll('.mock-list-item')
    await items[0].trigger('click')
    await flush()

    const itemsAfter = wrapper.findAll('.mock-list-item')
    expect(itemsAfter[0].classes()).toContain('highlighted')
    expect(itemsAfter[1].classes()).not.toContain('highlighted')
  })

  it('fetches full note by ID when navigating to a related note', async () => {
    mockListWithNotes(sampleNotes)
    knowledgeApi.getById.mockResolvedValue({ data: { data: { id: 99, title: 'Related', content: 'Full content' } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
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

    await wrapper.find('.mock-list-item').trigger('click')
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

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()
    await wrapper.find('.mock-navigate').trigger('click')
    await flush()

    await wrapper.find('.mock-back').trigger('click')
    await flush()

    const drawer = wrapper.findComponent({ name: 'NoteDrawer' })
    expect(drawer.props('canGoBack')).toBe(false)
    expect(drawer.props('note').id).toBe(1)
  })

  it('opens editor modal when edit button clicked from drawer', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()
    await wrapper.find('.mock-edit-btn').trigger('click')
    await flush()

    expect(wrapper.find('.mock-editor').exists()).toBe(true)
  })

  it('closes drawer when close button clicked', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()
    expect(wrapper.find('.mock-drawer').exists()).toBe(true)

    await wrapper.find('.mock-close-btn').trigger('click')
    await flush()

    expect(wrapper.find('.mock-drawer').exists()).toBe(false)
  })

  // ===== Drawer + Modal coexistence tests =====

  it('refreshes drawer note content after saving edit from drawer', async () => {
    mockListWithNotes(sampleNotes)
    knowledgeApi.getById.mockResolvedValue({ data: { data: { id: 1, title: 'Updated', content: 'New content' } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()
    knowledgeApi.getById.mockClear()

    await wrapper.find('.mock-edit-btn').trigger('click')
    await flush()
    await wrapper.find('.mock-editor-save').trigger('click')
    await flush()

    expect(knowledgeApi.getById).toHaveBeenCalledWith(1)
    const drawer = wrapper.findComponent({ name: 'NoteDrawer' })
    expect(drawer.props('note').title).toBe('Updated')
  })

  it('keeps drawer open after saving edit from drawer', async () => {
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.mock-list-item').trigger('click')
    await flush()
    await wrapper.find('.mock-edit-btn').trigger('click')
    await flush()
    await wrapper.find('.mock-editor-save').trigger('click')
    await flush()

    expect(wrapper.find('.mock-drawer').exists()).toBe(true)
  })

  // ===== Header conditional rendering tests =====

  it('shows sort select in browse mode', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.sort-select').exists()).toBe(true)
  })

  it('hides sort select in search mode', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.sort-select').exists()).toBe(true)

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    expect(wrapper.find('.sort-select').exists()).toBe(false)
  })

  it('does not show deep search toggle in browse mode', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.deep-toggle').exists()).toBe(false)
  })

  it('shows deep search toggle in search mode', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    expect(wrapper.find('.deep-toggle').exists()).toBe(true)
  })

  it('calls basic search by default in search mode', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    knowledgeApi.search.mockClear()
    knowledgeApi.searchRewrite.mockClear()

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    expect(knowledgeApi.search).toHaveBeenCalled()
    expect(knowledgeApi.searchRewrite).not.toHaveBeenCalled()
  })

  it('calls rewrite search when deep search toggle is enabled', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    knowledgeApi.search.mockClear()
    knowledgeApi.searchRewrite.mockClear()

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    knowledgeApi.search.mockClear()
    knowledgeApi.searchRewrite.mockClear()

    await wrapper.find('.deep-toggle').trigger('click')
    await flush()

    expect(knowledgeApi.searchRewrite).toHaveBeenCalled()
    expect(knowledgeApi.search).not.toHaveBeenCalled()
  })

  it('persists deep search preference in localStorage', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.search-input').setValue('架构')
    await wrapper.find('.search-btn').trigger('click')
    await flush()

    await wrapper.find('.deep-toggle').trigger('click')
    await flush()

    expect(localStorage.getItem('knowledge-deep-search')).toBe('true')
  })

  // ===== Tag pill bar tests =====

  it('does not render tag pill bar when no tags', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.tag-pill-bar').exists()).toBe(false)
  })

  it('renders tag pills below header when tags exist', async () => {
    knowledgeApi.getTags.mockResolvedValue({ data: { data: ['java', '架构', 'AI'] } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    expect(wrapper.find('.tag-pill-bar').exists()).toBe(true)
    expect(wrapper.findAll('.tag-pill')).toHaveLength(3)
  })

  it('clicking a tag pill filters by that tag in browse mode', async () => {
    knowledgeApi.getTags.mockResolvedValue({ data: { data: ['java', '架构'] } })
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    knowledgeApi.list.mockClear()

    await wrapper.findAll('.tag-pill')[0].trigger('click')
    await flush()

    expect(knowledgeApi.list).toHaveBeenCalledWith(expect.objectContaining({ tags: ['java'] }))
  })

  it('clicking active tag pill deactivates filter', async () => {
    knowledgeApi.getTags.mockResolvedValue({ data: { data: ['java', '架构'] } })
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.findAll('.tag-pill')[0].trigger('click')
    await flush()
    knowledgeApi.list.mockClear()

    await wrapper.findAll('.tag-pill')[0].trigger('click')
    await flush()

    expect(knowledgeApi.list).toHaveBeenCalledWith(expect.objectContaining({ tags: undefined }))
  })

  it('clicking a tag pill in search mode passes tag to search endpoint', async () => {
    knowledgeApi.getTags.mockResolvedValue({ data: { data: ['java', '架构'] } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()

    await wrapper.find('.search-input').setValue('设计')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    knowledgeApi.search.mockClear()

    await wrapper.findAll('.tag-pill')[0].trigger('click')
    await flush()

    expect(knowledgeApi.search).toHaveBeenCalledWith('设计', expect.objectContaining({ tag: 'java' }))
  })

  // ===== Load more pagination tests =====

  it('shows load more button when more items exist in browse mode', async () => {
    const notes = Array.from({ length: 20 }, (_, i) => ({ id: i + 1, title: `N${i+1}`, content: 'C', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' }))
    knowledgeApi.list.mockResolvedValue({ data: { data: { records: notes, total: 50 } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.load-more-btn').exists()).toBe(true)
  })

  it('does not show load more button when all items loaded', async () => {
    const notes = Array.from({ length: 5 }, (_, i) => ({ id: i + 1, title: `N${i+1}`, content: 'C', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' }))
    knowledgeApi.list.mockResolvedValue({ data: { data: { records: notes, total: 5 } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.load-more-btn').exists()).toBe(false)
  })

  it('clicking load more appends items in browse mode', async () => {
    const p1 = Array.from({ length: 20 }, (_, i) => ({ id: i + 1, title: `N${i+1}`, content: 'C', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' }))
    const p2 = Array.from({ length: 20 }, (_, i) => ({ id: i + 21, title: `N${i+21}`, content: 'C', tags: '[]', userTags: '[]', createdAt: '2024-06-15T10:00:00Z' }))
    knowledgeApi.list.mockResolvedValueOnce({ data: { data: { records: p1, total: 50 } } })
    knowledgeApi.list.mockResolvedValueOnce({ data: { data: { records: p2, total: 50 } } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.findAll('.mock-list-item')).toHaveLength(20)

    await wrapper.find('.load-more-btn').trigger('click')
    await flush()

    expect(wrapper.findAll('.mock-list-item')).toHaveLength(40)
    expect(knowledgeApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
  })

  it('shows load more in search mode when more results may exist', async () => {
    const results = Array.from({ length: 20 }, (_, i) => ({ id: i + 1, title: `R${i+1}`, content: 'C', similarity: 0.8 }))
    knowledgeApi.search.mockResolvedValue({ data: { data: results } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').setValue('test')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    expect(wrapper.find('.load-more-btn').exists()).toBe(true)
  })

  it('hides load more and shows refine hint when search reaches 60 cap', async () => {
    const results = Array.from({ length: 60 }, (_, i) => ({ id: i + 1, title: `R${i+1}`, content: 'C', similarity: 0.8 }))
    knowledgeApi.search.mockResolvedValue({ data: { data: results } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').setValue('test')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    expect(wrapper.find('.load-more-btn').exists()).toBe(false)
    expect(wrapper.find('.search-capped-hint').exists()).toBe(true)
  })

  it('clicking load more appends results in search mode with offset', async () => {
    const p1 = Array.from({ length: 20 }, (_, i) => ({ id: i + 1, title: `R${i+1}`, content: 'C', similarity: 0.8 }))
    const p2 = Array.from({ length: 5 }, (_, i) => ({ id: i + 21, title: `R${i+21}`, content: 'C', similarity: 0.7 }))
    knowledgeApi.search.mockResolvedValueOnce({ data: { data: p1 } })
    knowledgeApi.search.mockResolvedValueOnce({ data: { data: p2 } })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').setValue('test')
    await wrapper.find('.search-btn').trigger('click')
    await flush()
    knowledgeApi.search.mockClear()

    await wrapper.find('.load-more-btn').trigger('click')
    await flush()

    expect(knowledgeApi.search).toHaveBeenCalledWith('test', expect.objectContaining({ offset: 20 }))
    expect(wrapper.findAll('.mock-search-item')).toHaveLength(25)
  })

  // ===== Create button + shortcut tests =====

  it('does not render FAB button', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.fab').exists()).toBe(false)
  })

  it('renders create button in header', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.create-btn').exists()).toBe(true)
  })

  it('clicking create button opens editor modal', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.create-btn').trigger('click')
    await flush()
    expect(wrapper.find('.mock-editor').exists()).toBe(true)
  })

  it('pressing N key opens editor when not in input', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.trigger('keydown', { key: 'n' })
    await flush()
    expect(wrapper.find('.mock-editor').exists()).toBe(true)
  })

  it('does not open editor when typing N in search input', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    await wrapper.find('.search-input').trigger('keydown', { key: 'n' })
    await flush()
    expect(wrapper.find('.mock-editor').exists()).toBe(false)
  })

  it('empty state text guides to create button', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.text()).toContain('新建')
  })

  // ===== Mobile responsive tests =====

  it('shows more button on mobile', async () => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 375 })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.more-btn').exists()).toBe(true)
  })

  it('does not show more button on desktop', async () => {
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.more-btn').exists()).toBe(false)
  })

  it('clicking more button toggles overflow menu', async () => {
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 375 })
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.overflow-menu').exists()).toBe(false)
    await wrapper.find('.more-btn').trigger('click')
    await flush()
    expect(wrapper.find('.overflow-menu').exists()).toBe(true)
  })

  it('forces list density on mobile even when card is saved', async () => {
    localStorage.setItem('knowledge-density', 'card')
    Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 375 })
    mockListWithNotes(sampleNotes)
    const wrapper = mount(KnowledgeView, { global: { stubs } })
    await flush()
    expect(wrapper.find('.mock-list-item').exists()).toBe(true)
    expect(wrapper.find('.mock-card').exists()).toBe(false)
  })
})
