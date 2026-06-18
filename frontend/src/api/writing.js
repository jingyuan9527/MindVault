import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const writingApi = {
  generate: (topic, style, keywords) => api.post('/writing/generate', { topic, style, keywords })
}