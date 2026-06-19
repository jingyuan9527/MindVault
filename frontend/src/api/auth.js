import client from './client'

export const authApi = {
  login: (data) => client.post('/auth/login', data),
  logout: () => client.post('/auth/logout'),
  me: () => client.get('/auth/me'),
  changePassword: (data) => client.put('/auth/password', data),
  createToken: (data) => client.post('/auth/tokens', data),
  listTokens: () => client.get('/auth/tokens'),
  deleteToken: (id) => client.delete(`/auth/tokens/${id}`)
}
