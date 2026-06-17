import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const reviewApi = {
  getDue: (limit = 20) => api.get(`/review/due?limit=${limit}`),
  getDueCount: () => api.get('/review/due-count'),
  schedule: (knowledgeId) => api.post(`/review/${knowledgeId}/schedule`),
  perform: (knowledgeId, quality) => api.post(`/review/${knowledgeId}/perform`, { quality })
}