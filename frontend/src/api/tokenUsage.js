import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const tokenUsageApi = {
  getDaily: (days = 30) => api.get(`/token-usage/daily?days=${days}`),
  getTotal: (start, end) => api.get(`/token-usage/total?start=${start}&end=${end}`)
}