import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useChatStore } from '@/stores/chat'

vi.mock('@/api/chat', () => ({
  chatApi: {
    listSessions: vi.fn(),
    createSession: vi.fn(),
    getMessages: vi.fn(),
    sendMessage: vi.fn(),
    sendMessageStream: vi.fn()
  }
}))

import { chatApi } from '@/api/chat'

describe('Chat Store', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useChatStore()
    vi.clearAllMocks()
  })

  it('initial state is empty', () => {
    expect(store.sessions).toEqual([])
    expect(store.currentSessionId).toBeNull()
    expect(store.messages).toEqual([])
    expect(store.isLoading).toBe(false)
    expect(store.streamingContent).toBe('')
    expect(store.streamingSources).toEqual([])
    expect(store.cancelStream).toBeNull()
  })

  it('loadSessions sets sessions from API', async () => {
    const mockSessions = [{ id: 1, title: 'Session 1' }]
    chatApi.listSessions.mockResolvedValue({ data: { data: mockSessions } })

    await store.loadSessions()

    expect(store.sessions).toEqual(mockSessions)
  })

  it('createSession creates a new session', async () => {
    const newSession = { id: 42, title: 'New Session' }
    chatApi.createSession.mockResolvedValue({ data: { data: newSession } })

    const result = await store.createSession()

    expect(result).toEqual(newSession)
    expect(store.currentSessionId).toBe(42)
    expect(store.sessions[0]).toEqual(newSession)
    expect(store.messages).toEqual([])
  })

  it('loadMessages sets messages from API', async () => {
    const mockMessages = [
      { id: 1, role: 'USER', content: 'hi' },
      { id: 2, role: 'ASSISTANT', content: 'hello' }
    ]
    chatApi.getMessages.mockResolvedValue({ data: { data: mockMessages } })

    await store.loadMessages(5)

    expect(store.currentSessionId).toBe(5)
    expect(store.messages).toEqual(mockMessages)
  })

  it('sendMessage creates session if none exists', async () => {
    const newSession = { id: 99, title: 'Auto' }
    chatApi.createSession.mockResolvedValue({ data: { data: newSession } })
    chatApi.sendMessageStream.mockImplementation((sessionId, content, callbacks) => {
      return vi.fn()
    })

    await store.sendMessage('Hello')

    expect(chatApi.createSession).toHaveBeenCalled()
    expect(store.currentSessionId).toBe(99)
    expect(chatApi.sendMessageStream).toHaveBeenCalledWith(99, 'Hello', expect.any(Object))
  })

  it('sendMessage adds user message and tracks streaming', async () => {
    store.currentSessionId = 1
    let capturedCallbacks = {}
    chatApi.sendMessageStream.mockImplementation((sessionId, content, callbacks) => {
      capturedCallbacks = callbacks
      return vi.fn()
    })

    await store.sendMessage('User input')

    expect(store.messages.length).toBe(2)
    expect(store.messages[0].role).toBe('USER')
    expect(store.messages[0].content).toBe('User input')
    expect(store.messages[1].role).toBe('ASSISTANT')
    expect(store.isLoading).toBe(true)

    capturedCallbacks.onToken('Hello ')
    capturedCallbacks.onToken('world')

    expect(store.streamingContent).toBe('Hello world')
    expect(store.messages[1].content).toBe('Hello world')

    capturedCallbacks.onSources([{ id: 1, title: 'Doc1' }])

    expect(store.streamingSources).toEqual([{ id: 1, title: 'Doc1' }])
    expect(store.messages[1].sources).toBe(JSON.stringify([{ id: 1, title: 'Doc1' }]))
  })

  it('sendMessage handles done callback', async () => {
    store.currentSessionId = 1
    let capturedCallbacks = {}
    chatApi.sendMessageStream.mockImplementation((sessionId, content, callbacks) => {
      capturedCallbacks = callbacks
      return vi.fn()
    })

    store.streamingContent = 'temp'
    store.streamingSources = [{ id: 1 }]
    store.isLoading = true
    store.cancelStream = vi.fn()

    chatApi.getMessages.mockResolvedValue({ data: { data: [] } })

    await store.sendMessage('done test')
    capturedCallbacks.onDone()

    expect(store.isLoading).toBe(false)
    expect(store.streamingContent).toBe('')
    expect(store.streamingSources).toEqual([])
    expect(store.cancelStream).toBeNull()
  })

  it('sendMessage handles error callback', async () => {
    store.currentSessionId = 1
    let capturedCallbacks = {}
    chatApi.sendMessageStream.mockImplementation((sessionId, content, callbacks) => {
      capturedCallbacks = callbacks
      return vi.fn()
    })

    await store.sendMessage('error test')
    capturedCallbacks.onError('Network issue')

    expect(store.isLoading).toBe(false)
    expect(store.messages[1].content).toContain('Network issue')
  })

  it('sendMessage returns abort function as cancelStream', async () => {
    store.currentSessionId = 1
    const abortFn = vi.fn()
    chatApi.sendMessageStream.mockReturnValue(abortFn)

    await store.sendMessage('test')

    expect(store.cancelStream).toBe(abortFn)
  })
})