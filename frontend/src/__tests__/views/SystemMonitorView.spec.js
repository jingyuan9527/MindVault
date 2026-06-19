import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SystemMonitorView from '@/views/SystemMonitorView.vue'

vi.mock('@/api/system', () => ({
  systemApi: {
    health: vi.fn().mockResolvedValue({ data: { status: 'UP', uptime: 86400, checks: { database: { status: 'UP' }, disk: { free: '10000MB', total: '50000MB', freePercent: '20.0%' } } } }),
    info: vi.fn().mockResolvedValue({ data: { data: { javaVersion: '21', availableProcessors: 8, heapUsed: '256MB', freeMemory: '512MB', totalMemory: '1024MB', maxMemory: '2048MB' } } }),
    metrics: vi.fn().mockResolvedValue({ data: { data: { 'jvm.threads.live': 42, activeConnections: 3 } } }),
  }
}))

describe('SystemMonitorView', () => {
  it('renders heading', () => {
    const wrapper = mount(SystemMonitorView)
    expect(wrapper.text()).toContain('系统监控')
  })

  it('renders status after loading', async () => {
    const wrapper = mount(SystemMonitorView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('UP')
    expect(wrapper.text()).toContain('21')
    expect(wrapper.text()).toContain('42')
  })
})
