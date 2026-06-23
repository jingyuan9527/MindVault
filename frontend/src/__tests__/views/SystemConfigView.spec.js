import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SystemConfigView from '@/views/SystemConfigView.vue'

vi.mock('@/api/systemConfig', () => ({
  systemConfigApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [
      { configKey: 'test.key', configValue: 'test-value', valueType: 'STRING', description: '测试配置项', module: 'KNOWLEDGE' }
    ] } }),
    refresh: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  }
}))

describe('SystemConfigView', () => {
  it('renders heading and tabs', async () => {
    const wrapper = mount(SystemConfigView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('系统配置')
  })

  it('renders config list after loading', async () => {
    const wrapper = mount(SystemConfigView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('test.key')
    expect(wrapper.text()).toContain('测试配置项')
  })
})