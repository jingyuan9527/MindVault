<template>
  <div class="p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">用户管理</h2>

    <div class="card p-4 md:p-6">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr style="border-bottom: 1px solid var(--color-border); color: var(--color-text-secondary)">
              <th class="text-left py-2 font-medium">用户名</th>
              <th class="text-left py-2 font-medium">显示名称</th>
              <th class="text-left py-2 font-medium">角色</th>
              <th class="text-center py-2 font-medium">状态</th>
              <th class="text-left py-2 font-medium">创建时间</th>
              <th class="text-center py-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" style="border-bottom: 1px solid var(--color-border)">
              <td class="py-3" style="color: var(--color-text)">{{ u.username }}</td>
              <td class="py-3" style="color: var(--color-text)">{{ u.displayName }}</td>
              <td class="py-3">
                <span class="px-1.5 py-0.5 text-xs rounded"
                  :style="u.role === 'ADMIN' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-text-secondary)' }">
                  {{ u.role }}
                </span>
              </td>
              <td class="py-3 text-center">
                <span class="px-1.5 py-0.5 text-xs rounded"
                  :style="u.enabled ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-accent)' }">
                  {{ u.enabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-3" style="color: var(--color-warm-gray)">{{ u.createdAt?.slice(0, 10) }}</td>
              <td class="py-3 text-center">
                <button v-if="u.role !== 'ADMIN'"
                  @click="toggleEnabled(u)"
                  class="text-xs transition-colors duration-150"
                  :style="{ color: u.enabled ? 'var(--color-accent)' : 'var(--color-sage)' }"
                  @mouseenter="$event.target.style.opacity = '0.7'"
                  @mouseleave="$event.target.style.opacity = '1'">
                  {{ u.enabled ? '禁用' : '启用' }}
                </button>
                <span v-else class="text-xs" style="color: var(--color-text-secondary)">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!users.length" class="text-sm py-4" style="color: var(--color-text-secondary)">暂无用户数据</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/users'

const users = ref([])

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
