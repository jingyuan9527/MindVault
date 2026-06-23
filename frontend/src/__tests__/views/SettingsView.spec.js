import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SettingsView from '@/views/SettingsView.vue'

vi.mock('@/api/models', () => ({
  modelApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [
      { id: 1, provider: 'OPENAI', modelName: 'gpt-4', modelType: 'chat', priority: 1, isPrimary: true, isEnabled: true }
    ] } }),
    add: vi.fn(),
    delete: vi.fn(),
    setPrimary: vi.fn(),
    updatePriority: vi.fn(),
    testConnection: vi.fn(),
    fetchModels: vi.fn(),
  }
}))

vi.mock('@/api/auth', () => ({
  authApi: {
    listTokens: vi.fn().mockResolvedValue({ data: { data: [
      { id: 1, name: 'test-token', createdAt: '2024-06-15T10:00:00Z' }
    ] } }),
    createToken: vi.fn(),
    deleteToken: vi.fn(),
  }
}))

const dialogMock = { warning: vi.fn(), info: vi.fn(), success: vi.fn(), error: vi.fn() }
const messageMock = { success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }

vi.mock('@/api/knowledge', () => ({
  knowledgeApi: {
    exportJson: vi.fn(),
    exportMarkdown: vi.fn(),
    exportCsv: vi.fn(),
    previewImport: vi.fn(),
    importJson: vi.fn(),
  }
}))

describe('SettingsView', () => {
  it('renders heading and model config', async () => {
    const wrapper = mount(SettingsView)
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('系统设置')
    expect(wrapper.text()).toContain('模型配置')
  })

  it('renders token list', async () => {
    const wrapper = mount(SettingsView)
    await new Promise(r => setTimeout(r, 50))
    expect(wrapper.text()).toContain('API Token')
    expect(wrapper.text()).toContain('test-token')
  })

  it('shows import/export sections', () => {
    const wrapper = mount(SettingsView)
    expect(wrapper.text()).toContain('数据导入/导出')
    expect(wrapper.text()).toContain('数据备份')
  })
})