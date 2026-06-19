import client from './client'

export const systemApi = {
  health: () => client.get('/system/health'),
  info: () => client.get('/system/info'),
  metrics: () => client.get('/system/metrics'),
}
