import client from './client'

export const userApi = {
  list: () => client.get('/users'),
  setEnabled: (id, enabled) => client.put(`/users/${id}/enabled`, { enabled }),
}
