import client from './client'

export const systemConfigApi = {
  list() { return client.get('/system-config') },
  get(key) { return client.get(`/system-config/${key}`) },
  update(key, config) { return client.put(`/system-config/${key}`, config) },
  delete(key) { return client.delete(`/system-config/${key}`) },
  refresh() { return client.post('/system-config/refresh') },
  getModules() { return client.get('/system-config/modules') },
  getModuleDetail(moduleId) { return client.get(`/system-config/modules/${moduleId}`) },
  getItemDetail(key) { return client.get(`/system-config/items/${encodeURIComponent(key)}`) },
  getDefault(key) { return client.get(`/system-config/items/${encodeURIComponent(key)}/default`) },
  getValidation(key) { return client.get(`/system-config/items/${encodeURIComponent(key)}/validation`) },
  getAudit(key) { return client.get(`/system-config/items/${encodeURIComponent(key)}/audit`) },
  getScheduledTasks() { return client.get('/system-config/tasks') }
}
