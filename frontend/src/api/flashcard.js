import client from './client'

export const flashcardApi = {
  list: () => client.get('/flashcards'),
  listByKnowledge: (id) => client.get(`/flashcards/knowledge/${id}`),
  generate: (knowledgeId) => client.post(`/flashcards/generate/${knowledgeId}`),
  delete: (id) => client.delete(`/flashcards/${id}`)
}