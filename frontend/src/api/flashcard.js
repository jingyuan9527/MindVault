import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const flashcardApi = {
  list: () => api.get('/flashcards'),
  listByKnowledge: (knowledgeId) => api.get(`/flashcards/knowledge/${knowledgeId}`),
  generate: (knowledgeId) => api.post(`/flashcards/generate/${knowledgeId}`),
  delete: (id) => api.delete(`/flashcards/${id}`)
}