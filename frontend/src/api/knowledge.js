import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const knowledgeApi = {
  add: (data) => api.post('/knowledge', data),
  list: (page = 0, size = 20) => api.get(`/knowledge?page=${page}&size=${size}`),
  getById: (id) => api.get(`/knowledge/${id}`),
  search: (q) => api.get(`/knowledge/search?q=${encodeURIComponent(q)}`),
  update: (id, data) => api.put(`/knowledge/${id}`, data),
  delete: (id) => api.delete(`/knowledge/${id}`),
  parseUrl: (url) => api.post('/knowledge/parse-url', { url }),
  parsePdf: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/knowledge/parse-pdf', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  exportJson: () => api.get('/knowledge/export/json', { responseType: 'blob' }),
  exportMarkdown: () => api.get('/knowledge/export/markdown', { responseType: 'blob' }),
  importJson: (json) => api.post('/knowledge/import', json, {
    headers: { 'Content-Type': 'application/json' }
  }),
  getRelated: (id, limit = 5) => api.get(`/knowledge/${id}/related?limit=${limit}`)
}