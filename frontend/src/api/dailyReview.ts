import client from './client'

export const dailyReviewApi = {
  getLatest: () => client.get('/daily-review/latest'),
  getByDate: (date) => client.get(`/daily-review/date/${date}`),
  getRecent: (limit = 20) => client.get(`/daily-review/recent?limit=${limit}`),
  generate: (date) => client.post('/daily-review/generate', { date })
}