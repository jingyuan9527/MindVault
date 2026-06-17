import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const modelApi = {
  add: (data) => api.post('/models', data),
  list: () => api.get('/models'),
  setPrimary: (id) => api.patch(`/models/${id}/primary`),
  testConnection: (id) => api.post(`/models/${id}/test`),
  delete: (id) => api.delete(`/models/${id}`)
}