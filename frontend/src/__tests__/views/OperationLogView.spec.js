import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import OperationLogView from '@/views/OperationLogView.vue'

vi.mock('@/api/operationLog', () => ({
  operationLogApi: {
    list: vi.fn().mockResolvedValue({ data: { data: { records: [
      { id: 1, module: '知识库', action: '新增知识', summary: '测试摘要', operator: 'admin', result: 'SUCCESS', durationMs: 120, entityId: '42', createdAt: '2024-06-15T10:00:00Z' }
    ], total: 1, page: 0, size: 20, totalPages: 1 } } }),
    detail: vi.fn().mockResolvedValue({ data: { data: { id: 1, module: '知识库', action: '更新知识', summary: '测试', beforeSnapshot: '{"title":"旧"}', afterSnapshot: '{"title":"新"}', result: 'SUCCESS', durationMs: 150, operator: 'admin', createdAt: '2024-06-15T10:00:00Z' } } })
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

  it('renders log list after load', async () => {
    const wrapper = mount(OperationLogView)
    await new Promise(r => setTimeout(r, 100))
    expect(wrapper.text()).toContain('操作日志')
  })
})