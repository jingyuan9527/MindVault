import client from './client'

export const knowledgeApi = {
  add: (data) => client.post('/knowledge', data),
  list: (params) => client.get('/knowledge', { params }),
  getById: (id) => client.get(`/knowledge/${id}`),
  search: (q, params = {}) => client.get('/knowledge/search', { params: { q, ...params } }),
  update: (id, data) => client.put(`/knowledge/${id}`, data),
  delete: (id) => client.delete(`/knowledge/${id}`),
  parseUrl: (url) => client.post('/knowledge/parse-url', { url }),
  parsePdf: (file) => {
    const form = new FormData()
    form.append('file', file)
    return client.post('/knowledge/parse-pdf', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  exportJson: () => client.get('/knowledge/export/json', { responseType: 'blob' }),
  exportMarkdown: () => client.get('/knowledge/export/markdown', { responseType: 'blob' }),
  exportCsv: () => client.get('/knowledge/export/csv', { responseType: 'blob' }),
  importJson: (json, conflictMode = 'skip') => client.post(`/knowledge/import?conflict=${conflictMode}`, json, {
    headers: { 'Content-Type': 'application/json' }
  }),
  previewImport: (json) => client.post('/knowledge/import/preview', json, {
    headers: { 'Content-Type': 'application/json' }
  }),
  getRelated: (id, limit = 5) => client.get(`/knowledge/${id}/related?limit=${limit}`),
  getTags: () => client.get('/knowledge/tags'),
  batchDelete: (ids) => client.post('/knowledge/batch/delete', ids),
  batchTag: (ids, tag) => client.post('/knowledge/batch/tag', { ids, tag }),
  batchExport: (ids) => client.post('/knowledge/batch/export', ids, { responseType: 'blob' }),
  batchAiTag: (ids) => client.post('/knowledge/batch/ai-tag', ids),
  reprocess: (id) => client.post(`/knowledge/${id}/reprocess`),
  getProcessLogs: (id) => client.get(`/knowledge/${id}/process-logs`)
}