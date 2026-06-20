<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">用户管理</h2>

    <n-card size="small">
      <n-data-table
        :columns="columns"
        :data="users"
        :bordered="false"
        :single-line="false"
        size="small"
      />
      <n-empty v-if="!users.length" description="暂无用户数据" class="py-4" />
    </n-card>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NButton } from 'naive-ui'
import { userApi } from '@/api/users'

const users = ref([])

const columns = [
  { title: '用户名', key: 'username' },
  { title: '显示名称', key: 'displayName' },
  {
    title: '角色',
    key: 'role',
    width: 100,
    render(row) {
      return h(NTag, { size: 'small', type: row.role === 'ADMIN' ? 'success' : 'default', bordered: false }, { default: () => row.role })
    }
  },
  {
    title: '状态',
    key: 'enabled',
    width: 80,
    render(row) {
      return h(NTag, { size: 'small', type: row.enabled ? 'success' : 'error', bordered: false }, { default: () => row.enabled ? '启用' : '禁用' })
    }
  },
  { title: '创建时间', key: 'createdAt', width: 120, render(row) { return row.createdAt?.slice(0, 10) || '' } },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    render(row) {
      if (row.role === 'ADMIN') return h('span', { style: 'color: var(--color-text-secondary); font-size: 12px' }, '-')
      return h(NButton, {
        text: true,
        size: 'tiny',
        type: row.enabled ? 'error' : 'info',
        onClick: () => toggleEnabled(row)
      }, { default: () => row.enabled ? '禁用' : '启用' })
    }
  }
]

async function loadUsers() {
  try {
    const res = await userApi.list()
    users.value = res.data.data || []
  } catch {}
}

async function toggleEnabled(u) {
  try {
    await userApi.setEnabled(u.id, !u.enabled)
    u.enabled = !u.enabled
  } catch {}
}

onMounted(loadUsers)
</script>
