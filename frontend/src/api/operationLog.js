import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

export const operationLogApi = {
  list: (module) => api.get('/operation-logs', { params: module ? { module } : {} })
}