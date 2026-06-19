import client from './client'

export const writingApi = {
  generate: (data) => client.post('/writing/generate', data)
}