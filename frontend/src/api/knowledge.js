import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const knowledgeApi = {
  add: (data) => api.post('/knowledge', data),
  list: (page = 0, size = 20) => api.get(`/knowledge?page=${page}&size=${size}`),
  getById: (id) => api.get(`/knowledge/${id}`),
  search: (q) => api.get(`/knowledge/search?q=${encodeURIComponent(q)}`),
  update: (id, data) => api.put(`/knowledge/${id}`, data),
  delete: (id) => api.delete(`/knowledge/${id}`)
}