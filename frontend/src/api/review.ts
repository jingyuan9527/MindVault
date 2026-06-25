import client from './client'

export const reviewApi = {
  getDue: (limit = 10) => client.get(`/review/due?limit=${limit}`),
  getDueCount: () => client.get('/review/due-count'),
  schedule: (knowledgeId) => client.post(`/review/${knowledgeId}/schedule`),
  perform: (knowledgeId, quality) => client.post(`/review/${knowledgeId}/perform`, { quality })
}