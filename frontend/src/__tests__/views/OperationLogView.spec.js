import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import OperationLogView from '@/views/OperationLogView.vue'

vi.mock('@/api/operationLog', () => ({
  operationLogApi: {
    list: vi.fn().mockResolvedValue({ data: { data: [
      { id: 1, module: 'KNOWLEDGE', action: 'ADD', summary: '测试摘要', entityId: 42, createdAt: '2024-06-15T10:00:00Z' }
    ] } }),
  }
}))

describe('OperationLogView', () => {
  it('renders heading', () => {
    const wrapper = mount(OperationLogView)
    expect(wrapper.text()).toContain('操作日志')
  })

  it('renders module filter buttons', async () => {
    const wrapper = mount(OperationLogView)
    const text = wrapper.text()
    expect(text).toContain('全部')
    expect(text).toContain('知识库')
    expect(text).toContain('对话')
  })
})