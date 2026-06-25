import client from './client'

export const chatApi = {
  createSession: () => client.post('/chat/sessions'),
  listSessions: () => client.get('/chat/sessions'),
  getMessages: (sessionId) => client.get(`/chat/sessions/${sessionId}/messages`),
  sendMessage: (sessionId, content) =>
    client.post(`/chat/sessions/${sessionId}/messages`, { content }),

  sendMessageStream: (sessionId, content, callbacks) => {
    const controller = new AbortController()
    const { onToken, onDone, onError, onSources, onBlocked } = callbacks

    const token = localStorage.getItem('mindvault_token')
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`
    fetch(`/api/v1/chat/sessions/${sessionId}/messages/stream`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ content }),
      signal: controller.signal
    }).then(async (response) => {
      if (!response.ok) {
        onError(`请求失败: ${response.status}`)
        return
      }
      if (!response.body) { onError('响应体为空'); return }
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        while (buffer.includes('\n\n')) {
          const eventEnd = buffer.indexOf('\n\n')
          const eventBlock = buffer.slice(0, eventEnd)
          buffer = buffer.slice(eventEnd + 2)

          let eventType = ''
          let eventData = ''
          for (const line of eventBlock.split('\n')) {
            if (line.startsWith('event: ')) eventType = line.slice(7)
            else if (line.startsWith('data: ')) eventData = line.slice(6)
          }

          if (eventType === 'token') onToken(eventData)
          else if (eventType === 'done') onDone()
          else if (eventType === 'error') onError(eventData)
          else if (eventType === 'blocked' && onBlocked) onBlocked(eventData)
          else if (eventType === 'sources' && onSources) onSources(JSON.parse(eventData))
        }
      }
      onDone()
    }).catch(err => {
      if (err.name !== 'AbortError') onError(err.message)
    })

    return () => controller.abort()
  }
}