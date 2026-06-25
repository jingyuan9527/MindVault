import client from './client'

export const tokenUsageApi = {
  getDaily: (days = 30) => client.get(`/token-usage/daily?days=${days}`),
  getTotal: (start, end) => client.get(`/token-usage/total?start=${start}&end=${end}`),
  getBySource: (days = 30) => client.get(`/token-usage/by-source?days=${days}`)
}