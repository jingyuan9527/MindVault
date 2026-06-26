import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SystemConfigView from '@/views/SystemConfigView.vue'

const mockModules = vi.hoisted(() => ({
  data: {
    data: {
      modules: [
        {
          id: 'knowledge',
          label: '知识库',
          groups: [
            {
              id: 'prompt',
              label: '提示词',
              items: [
                { key: 'prompt.search.query-rewrite', value: 'rewrite query', valueType: 'prompt', description: '查询改写', defaultValue: 'default rewrite' }
              ]
            }
          ]
        }
      ]
    }
  }
}))

const mockList = vi.hoisted(() => ({
  data: {
    data: [
      { configKey: 'prompt.search.query-rewrite', configValue: 'rewrite query', valueType: 'prompt', description: '查询改写', module: 'knowledge', updatedAt: '2026-06-25T10:00:00' }
    ]
  }
}))

const mockTasks = vi.hoisted(() => ({
  data: {
    data: [
      { id: 'backup', label: '自动备份', enabledKey: 'task.backup.enabled', scheduleKey: 'task.backup.cron', scheduleType: 'cron' }
    ]
  }
}))

vi.mock('@/api/systemConfig', () => ({
  systemConfigApi: {
    list: vi.fn().mockResolvedValue(mockList),
    getModules: vi.fn().mockResolvedValue(mockModules),
    getScheduledTasks: vi.fn().mockResolvedValue(mockTasks),
    refresh: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getAudit: vi.fn().mockResolvedValue({ data: { data: [] } }),
  }
}))

describe('SystemConfigView', () => {
  it('renders heading and module nav', async () => {
    const wrapper = mount(SystemConfigView)
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.text()).toContain('系统配置')
    expect(wrapper.text()).toContain('知识库')
  })

  it('renders config items in module panel', async () => {
    const wrapper = mount(SystemConfigView)
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.text()).toContain('prompt.search.query-rewrite')
    expect(wrapper.text()).toContain('查询改写')
  })
})