import client from './client'

export const operationLogApi = {
  list: (params = {}) => client.get('/operation-logs', { params }),
  detail: (id) => client.get(`/operation-logs/${id}`)
}