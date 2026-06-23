import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import LoginView from '@/views/LoginView.vue'

const mockRouter = { push: vi.fn() }
const mockRoute = { query: {} }

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => mockRoute,
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    login: vi.fn().mockRejectedValue(new Error('fail')),
    me: vi.fn(),
  }
}))

describe('LoginView', () => {
  it('renders branding and form', () => {
    setActivePinia(createPinia())
    const wrapper = mount(LoginView)
    expect(wrapper.text()).toContain('MindVault')
    expect(wrapper.text()).toContain('知忆')
    expect(wrapper.text()).toContain('登录')
  })

  it('shows error alert after failed login', async () => {
    setActivePinia(createPinia())
    const wrapper = mount(LoginView)
    const vm = wrapper.vm
    vm.formData.username = 'test'
    vm.formData.password = 'wrong'
    await vm.handleLogin()
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('登录失败')
  })
})