import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const backupApi = {
  list: () => api.get('/backup'),
  create: () => api.post('/backup'),
  download: (filename) => api.get(`/backup/download/${encodeURIComponent(filename)}`, { responseType: 'blob' })
}