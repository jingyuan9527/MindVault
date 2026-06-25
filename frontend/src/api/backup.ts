import client from './client'

export const backupApi = {
  list: () => client.get('/backup'),
  create: () => client.post('/backup'),
  download: (filename) => client.get(`/backup/download/${encodeURIComponent(filename)}`, { responseType: 'blob' })
}