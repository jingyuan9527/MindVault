import client from './client'

export const modelApi = {
  add: (data) => client.post('/models', data),
  list: () => client.get('/models'),
  setPrimary: (id) => client.patch(`/models/${id}/primary`),
  updatePriority: (id, priority) => client.patch(`/models/${id}/priority`, priority, {
    headers: { 'Content-Type': 'application/json' }
  }),
  fetchModels: (data) => client.post('/models/fetch', data),
  testConnection: (id) => client.post(`/models/${id}/test`),
  delete: (id) => client.delete(`/models/${id}`)
}