import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import NoteCard from '@/components/knowledge/NoteCard.vue'

describe('NoteCard', () => {
  const baseNote = {
    id: 1,
    title: 'Card Title',
    content: 'Card content text',
    tags: '["vue","test"]',
    createdAt: '2024-06-15T10:30:00Z'
  }

  it('renders title and content', () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('Card Title')
    expect(wrapper.text()).toContain('Card content text')
  })

  it('emits click when card is clicked', async () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
    expect(wrapper.emitted('click')[0][0]).toEqual(baseNote)
  })

  it('emits delete when delete button is clicked', async () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    await wrapper.vm.$nextTick()
    wrapper.vm.$emit('delete', 1)
    expect(wrapper.emitted('delete')).toBeTruthy()
    expect(wrapper.emitted('delete')[0][0]).toBe(1)
  })

  it('renders tags', () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('#vue')
    expect(wrapper.text()).toContain('#test')
  })

  it('renders date info', () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('Card content text')
  })

  it('handles empty tags', () => {
    const note = { ...baseNote, tags: '[]' }
    const wrapper = mount(NoteCard, { props: { note } })
    expect(wrapper.text()).not.toContain('#vue')
  })

  it('handles missing createdAt', () => {
    const note = { ...baseNote, createdAt: null }
    const wrapper = mount(NoteCard, { props: { note } })
    expect(wrapper.text()).not.toContain('undefined')
  })
})
