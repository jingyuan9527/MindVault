import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const chatApi = {
  createSession: () => api.post('/chat/sessions'),
  listSessions: () => api.get('/chat/sessions'),
  getMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
  sendMessage: (sessionId, content) =>
    api.post(`/chat/sessions/${sessionId}/messages`, { content })
}