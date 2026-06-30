import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NoteListItem from '@/components/knowledge/NoteListItem.vue'

describe('NoteListItem', () => {
  const baseNote = {
    id: 1,
    title: 'Test Note',
    content: 'Some content',
    tags: '["java","spring","vue"]',
    createdAt: '2024-06-15T10:00:00Z'
  }

  it('renders title and content', () => {
    const wrapper = mount(NoteListItem, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('Test Note')
    expect(wrapper.text()).toContain('Some content')
  })

  it('emits click when row is clicked', async () => {
    const wrapper = mount(NoteListItem, {
      props: { note: baseNote }
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')[0][0]).toEqual(baseNote)
  })

  it('renders tags pills', () => {
    const wrapper = mount(NoteListItem, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('#java')
    expect(wrapper.text()).toContain('#spring')
    expect(wrapper.text()).toContain('#vue')
  })

  it('shows overflow count when more than 3 tags', () => {
    const note = { ...baseNote, tags: '["a","b","c","d","e"]' }
    const wrapper = mount(NoteListItem, {
      props: { note }
    })
    expect(wrapper.text()).toContain('+2')
  })

  it('does not show overflow count when 3 or fewer tags', () => {
    const wrapper = mount(NoteListItem, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).not.toContain('+')
  })

  it('formats date correctly', () => {
    const wrapper = mount(NoteListItem, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('2024-06-15')
  })

  it('handles missing createdAt gracefully', () => {
    const note = { ...baseNote, createdAt: null }
    const wrapper = mount(NoteListItem, {
      props: { note }
    })
    expect(wrapper.text()).not.toContain('undefined')
  })

  it('handles invalid tags JSON', () => {
    const note = { ...baseNote, tags: 'invalid-json' }
    const wrapper = mount(NoteListItem, {
      props: { note }
    })
    expect(wrapper.text()).not.toContain('#')
  })

  it('does not render tag section when tags are empty', () => {
    const note = { ...baseNote, tags: '[]' }
    const wrapper = mount(NoteListItem, {
      props: { note }
    })
    expect(wrapper.text()).not.toContain('#')
  })
})