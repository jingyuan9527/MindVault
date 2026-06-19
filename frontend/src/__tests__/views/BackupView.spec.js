import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BackupView from '@/views/BackupView.vue'

vi.mock('@/api/backup', () => ({
  backupApi: {
    list: vi.fn().mockResolvedValue({ data: { data: ['backup-2024-01-01.sql', 'backup-2024-01-02.sql'] } }),
    create: vi.fn().mockResolvedValue({}),
    download: vi.fn().mockResolvedValue({ data: new Blob() }),
  }
}))

describe('BackupView', () => {
  it('renders heading', () => {
    const wrapper = mount(BackupView)
    expect(wrapper.text()).toContain('数据备份')
  })

  it('renders backup list', async () => {
    const wrapper = mount(BackupView)
    // wait for onMounted to resolve
    await new Promise(r => setTimeout(r, 10))
    expect(wrapper.text()).toContain('backup-2024-01-01.sql')
  })
})
