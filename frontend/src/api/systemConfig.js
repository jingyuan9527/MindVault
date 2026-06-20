import client from './client'

export const systemConfigApi = {
  list() { return client.get('/system-config') },
  get(key) { return client.get(`/system-config/${key}`) },
  update(key, config) { return client.put(`/system-config/${key}`, config) },
  delete(key) { return client.delete(`/system-config/${key}`) },
  refresh() { return client.post('/system-config/refresh') }
}
