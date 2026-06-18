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
    const deleteBtn = wrapper.find('button')
    await deleteBtn.trigger('click')
    expect(wrapper.emitted('delete')).toBeTruthy()
    expect(wrapper.emitted('delete')[0][0]).toBe(1)
  })

  it('renders tag pills', () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    expect(wrapper.text()).toContain('#vue')
    expect(wrapper.text()).toContain('#test')
  })

  it('formats date with time', () => {
    const wrapper = mount(NoteCard, {
      props: { note: baseNote }
    })
    const d = new Date('2024-06-15T10:30:00Z')
    const expectedTime = `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
    expect(wrapper.text()).toContain('2024-06-15')
    expect(wrapper.text()).toContain(expectedTime)
  })

  it('handles empty tags', () => {
    const note = { ...baseNote, tags: '[]' }
    const wrapper = mount(NoteCard, { props: { note } })
    expect(wrapper.findAll('.tag-pill').length).toBe(0)
  })

  it('handles missing createdAt', () => {
    const note = { ...baseNote, createdAt: null }
    const wrapper = mount(NoteCard, { props: { note } })
    expect(wrapper.text()).not.toContain('undefined')
  })
})