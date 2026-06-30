import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NoteDrawer from '@/components/knowledge/NoteDrawer.vue'

describe('NoteDrawer', () => {
  const note = {
    id: 1,
    title: '微服务架构',
    aiTitle: '微服务架构设计',
    content: '## 概述\n微服务架构是一种将应用拆分为多个小服务的方法。',
    tags: '["架构","微服务"]',
    userTags: '[]',
    createdAt: '2024-06-15T10:00:00Z',
  }

  const relatedNotes = [
    { id: 2, title: '单体架构', summary: '单体架构的优缺点', similarity: 0.85, tags: '["架构"]' },
    { id: 3, title: 'API 网关', summary: 'API 网关模式', similarity: 0.72, tags: '["架构","网关"]' },
  ]

  const baseProps = {
    visible: true,
    note,
    relatedNotes,
    canGoBack: false,
  }

  const stubs = {
    ContentRenderer: { template: '<div class="mock-renderer" />', props: ['content', 'preview'] },
  }

  it('renders note title (aiTitle preferred)', () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    expect(wrapper.text()).toContain('微服务架构设计')
  })

  it('renders note content via ContentRenderer', () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    expect(wrapper.find('.mock-renderer').exists()).toBe(true)
  })

  it('renders related notes section', () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    expect(wrapper.text()).toContain('单体架构')
    expect(wrapper.text()).toContain('API 网关')
  })

  it('emits navigate when clicking a related note', async () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    const items = wrapper.findAll('.related-item')
    await items[0].trigger('click')
    expect(wrapper.emitted('navigate')).toBeTruthy()
    expect(wrapper.emitted('navigate')[0][0]).toEqual(relatedNotes[0])
  })

  it('emits back when back button clicked and canGoBack is true', async () => {
    const wrapper = mount(NoteDrawer, {
      props: { ...baseProps, canGoBack: true },
      global: { stubs },
    })
    await wrapper.find('.drawer-back').trigger('click')
    expect(wrapper.emitted('back')).toBeTruthy()
  })

  it('does not show back button when canGoBack is false', () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    expect(wrapper.find('.drawer-back').exists()).toBe(false)
  })

  it('emits edit when edit button clicked', async () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    await wrapper.find('.drawer-edit').trigger('click')
    expect(wrapper.emitted('edit')).toBeTruthy()
  })

  it('emits update:visible(false) when close button clicked', async () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    await wrapper.find('.drawer-close').trigger('click')
    expect(wrapper.emitted('update:visible')).toBeTruthy()
    expect(wrapper.emitted('update:visible')[0][0]).toBe(false)
  })

  it('renders tags', () => {
    const wrapper = mount(NoteDrawer, { props: baseProps, global: { stubs } })
    expect(wrapper.text()).toContain('#架构')
    expect(wrapper.text()).toContain('#微服务')
  })

  it('shows empty message when no related notes', () => {
    const wrapper = mount(NoteDrawer, {
      props: { ...baseProps, relatedNotes: [] },
      global: { stubs },
    })
    expect(wrapper.text()).toContain('暂无关联')
  })

  it('does not render when visible is false', () => {
    const wrapper = mount(NoteDrawer, {
      props: { ...baseProps, visible: false },
      global: { stubs },
    })
    expect(wrapper.find('.note-drawer').exists()).toBe(false)
  })
})
