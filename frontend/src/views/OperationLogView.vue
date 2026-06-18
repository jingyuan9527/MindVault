<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-display text-lg md:text-xl">操作日志</h2>
        <button @click="loadLogs" :disabled="loading" class="btn-primary text-sm">
          {{ loading ? '加载中...' : '刷新' }}
        </button>
      </div>
      <div class="flex gap-2 flex-wrap">
        <button v-for="m in modules" :key="m.value"
          @click="currentModule = m.value; loadLogs()"
          class="px-3 py-1.5 text-xs rounded-full transition-all duration-150"
          :style="currentModule === m.value
            ? { backgroundColor: 'var(--color-sage)', color: 'white' }
            : { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">
          {{ m.label }}
        </button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin" style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="error" class="card p-6" :style="{ borderLeft: '3px solid var(--color-accent)' }">
        <p style="color: var(--color-accent)">{{ error }}</p>
      </div>

      <div v-else-if="!logs.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-10 h-10 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">暂无操作日志</p>
        <p class="text-sm mt-1">进行知识库操作后日志将在此显示</p>
      </div>

      <div v-else class="max-w-4xl mx-auto space-y-2 stagger-enter">
        <div v-for="log in logs" :key="log.id"
          class="card p-3 md:p-4 flex items-start gap-3 md:gap-4 fade-in-enter">
          <div class="shrink-0 w-2 h-2 mt-2 rounded-full"
            :style="{ backgroundColor: moduleColor(log.module) }"></div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1 flex-wrap">
              <span class="tag-pill text-xs">{{ log.module }}</span>
              <span class="text-xs font-medium" style="color: var(--color-text)">{{ actionLabel(log.action) }}</span>
              <span v-if="log.entityId" class="text-xs" style="color: var(--color-text-secondary)">#{{ log.entityId }}</span>
              <span class="text-xs md:hidden" style="color: var(--color-text-secondary)">{{ formatTime(log.createdAt) }}</span>
            </div>
            <p class="text-xs md:text-sm" style="color: var(--color-warm-gray)">{{ log.summary }}</p>
          </div>
          <div class="hidden md:block shrink-0 text-xs whitespace-nowrap" style="color: var(--color-text-secondary)">
            {{ formatTime(log.createdAt) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { operationLogApi } from '@/api/operationLog'

const modules = [
  { value: '', label: '全部' },
  { value: 'KNOWLEDGE', label: '知识库' },
  { value: 'CHAT', label: '对话' },
  { value: 'MODEL', label: '模型' },
  { value: 'SYSTEM', label: '系统' }
]

const currentModule = ref('')
const logs = ref([])
const loading = ref(true)
const error = ref('')

const actionMap = {
  ADD: '新增',
  UPDATE: '更新',
  DELETE: '删除',
  SEARCH: '搜索',
  EXPORT: '导出',
  IMPORT: '导入',
  TAG: '标记',
  TEST: '测试',
  ERROR: '错误',
  CREATE_SESSION: '创建会话',
  SEND_MESSAGE: '发送消息',
  PERFORM: '执行',
  BACKUP: '备份',
  SET_PRIMARY: '设为默认',
  UPDATE_PRIORITY: '更新优先级'
}

function actionLabel(action) {
  return actionMap[action] || action
}

function moduleColor(module) {
  const colors = {
    KNOWLEDGE: 'var(--color-sage)',
    CHAT: '#4a90d9',
    MODEL: '#d9a84a',
    SYSTEM: 'var(--color-accent)'
  }
  return colors[module] || 'var(--color-text-secondary)'
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    const res = await operationLogApi.list(currentModule.value || undefined)
    logs.value = res.data.data || []
  } catch {
    error.value = '加载操作日志失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>