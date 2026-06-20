import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import UserManagementView from '@/views/UserManagementView.vue'

vi.mock('@/api/users', () => ({
  userApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [{ id: 1, username: 'admin', displayName: '管理员', role: 'ADMIN', enabled: true, createdAt: '2024-01-01T00:00:00' }, { id: 2, username: 'user1', displayName: '用户1', role: 'USER', enabled: true, createdAt: '2024-01-02T00:00:00' }] } }),
    setEnabled: vi.fn().mockResolvedValue({}),
  }
}))

describe('UserManagementView', () => {
  it('renders heading', () => {
    const wrapper = mount(UserManagementView)
    expect(wrapper.text()).toContain('用户管理')
  })

  it('calls userApi.list on mount', async () => {
    const { userApi } = await import('@/api/users')
    mount(UserManagementView)
    await new Promise(r => setTimeout(r, 10))
    expect(userApi.list).toHaveBeenCalled()
  })
})
