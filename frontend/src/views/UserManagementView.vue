<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6">
    <div class="flex items-center gap-3 mb-4 md:mb-6">
      <div class="users-header-icon">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
        </svg>
      </div>
      <div>
        <h2 class="font-display text-xl md:text-2xl" style="color: var(--color-text)">用户管理</h2>
        <p class="text-xs" style="color: var(--color-text-secondary)">管理系统用户与权限</p>
      </div>
    </div>
    <n-spin v-if="loading" class="flex justify-center py-12" />
    <div v-else class="users-card">
      <div v-for="u in users" :key="u.id" class="user-row">
        <div class="user-avatar" :style="{ backgroundColor: userColor(u.username) }">{{ initials(u.username) }}</div>
        <div class="user-info">
          <p class="user-name">{{ u.username }}</p>
          <p class="user-display" v-if="u.displayName && u.displayName !== u.username">{{ u.displayName }}</p>
        </div>
        <div class="user-badges">
          <span class="user-role" :class="u.role === 'ADMIN' ? 'role-admin' : 'role-user'">{{ u.role }}</span>
          <span class="user-status" :class="u.enabled ? 'status-enabled' : 'status-disabled'">{{ u.enabled ? '启用' : '禁用' }}</span>
        </div>
        <span class="user-created">{{ u.createdAt?.slice(0, 10) || '' }}</span>
        <div class="user-actions">
          <n-button v-if="u.role !== 'ADMIN'" text size="tiny" :type="u.enabled ? 'error' : 'info'" @click="toggleEnabled(u)">
            {{ u.enabled ? '禁用' : '启用' }}
          </n-button>
          <span v-else class="text-xs" style="color: var(--color-text-secondary)">-</span>
        </div>
      </div>
      <n-empty v-if="!users.length" description="暂无用户数据" class="py-8" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '@/api/users'

const users = ref([])
const loading = ref(true)

const avatarColors = ['#8B5CF6', '#c65f39', '#4f46e5', '#0891b2', '#ca8a04', '#7c3aed', '#dc2626', '#0d9488']
function userColor(name) { let hash = 0; for (let i = 0; i < (name || '').length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash); return avatarColors[Math.abs(hash) % avatarColors.length] }
function initials(name) { if (!name) return '?'; return name.slice(0, 2).toUpperCase() }

async function loadUsers() { loading.value = true; try { const res = await userApi.list(); users.value = res.data.data || [] } catch (e) { console.error('加载用户列表失败:', e) } finally { loading.value = false } }
async function toggleEnabled(u) { try { await userApi.setEnabled(u.id, !u.enabled); u.enabled = !u.enabled } catch {} }

onMounted(loadUsers)
</script>

<style scoped>
.users-header-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; background: var(--gradient-brand); }
.users-card { border-radius: 14px; background-color: var(--color-surface); border: 1px solid var(--color-border); overflow: hidden; }
.user-row { display: flex; align-items: center; gap: 12px; padding: 14px 20px; border-bottom: 1px solid var(--color-border); }
.user-row:last-child { border-bottom: none; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
.user-info { flex: 1; min-width: 0; }
.user-name { font-size: 0.9rem; font-weight: 500; color: var(--color-text); }
.user-display { font-size: 0.75rem; color: var(--color-text-secondary); }
.user-badges { display: flex; gap: 6px; align-items: center; }
.user-role { font-size: 0.65rem; padding: 2px 8px; border-radius: 999px; font-weight: 500; }
.role-admin { background-color: var(--color-sage-light); color: var(--color-sage); }
.role-user { background-color: rgba(79, 70, 229, 0.1); color: #4f46e5; }
.user-status { font-size: 0.65rem; padding: 2px 8px; border-radius: 999px; font-weight: 500; }
.status-enabled { background-color: var(--color-sage-light); color: var(--color-sage); }
.status-disabled { background-color: #fef2f2; color: #dc2626; }
.user-created { font-size: 0.75rem; color: var(--color-text-secondary); white-space: nowrap; }
.user-actions { width: 60px; text-align: right; }
</style>