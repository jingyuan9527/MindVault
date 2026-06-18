import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const dailyReviewApi = {
  getLatest: () => api.get('/daily-review/latest'),
  getByDate: (date) => api.get(`/daily-review/date/${date}`),
  getRecent: (limit = 7) => api.get(`/daily-review/recent?limit=${limit}`),
  generate: (date) => api.post(`/daily-review/generate${date ? `?date=${date}` : ''}`)
}