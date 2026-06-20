<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-display text-lg md:text-xl">操作日志</h2>
        <n-button size="small" :loading="loading" @click="loadLogs">刷新</n-button>
      </div>
      <n-space size="small">
        <n-button
          v-for="m in modules" :key="m.value"
          size="tiny"
          :type="currentModule === m.value ? 'primary' : 'default'"
          @click="currentModule = m.value; loadLogs()"
        >
          {{ m.label }}
        </n-button>
      </n-space>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="loading" class="flex justify-center py-12" />

      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">
        {{ error }}
      </n-alert>

      <n-data-table
        v-else-if="logs.length"
        :columns="columns"
        :data="logs"
        :bordered="false"
        :single-line="false"
        size="small"
        class="log-table"
      />

      <n-empty v-else description="暂无操作日志">
        <template #extra>
          <p class="text-xs" style="color: var(--color-text-secondary)">进行知识库操作后日志将在此显示</p>
        </template>
      </n-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NSpace, NTime } from 'naive-ui'
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
  ADD: '新增', UPDATE: '更新', DELETE: '删除',
  SEARCH: '搜索', EXPORT: '导出', IMPORT: '导入',
  TAG: '标记', TEST: '测试', ERROR: '错误',
  CREATE_SESSION: '创建会话', SEND_MESSAGE: '发送消息',
  PERFORM: '执行', BACKUP: '备份',
  SET_PRIMARY: '设为默认', UPDATE_PRIORITY: '更新优先级'
}

const moduleTagType = {
  KNOWLEDGE: 'success',
  CHAT: 'info',
  MODEL: 'warning',
  SYSTEM: 'error'
}

const columns = [
  {
    title: '模块',
    key: 'module',
    width: 90,
    render(row) {
      return h(NTag, { size: 'small', type: moduleTagType[row.module] || 'default', bordered: false }, { default: () => row.module })
    }
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render(row) {
      return h('span', { style: 'font-weight: 500' }, actionMap[row.action] || row.action)
    }
  },
  {
    title: '摘要',
    key: 'summary',
    ellipsis: { tooltip: true }
  },
  {
    title: 'ID',
    key: 'entityId',
    width: 80,
    render(row) {
      return row.entityId ? h('span', { style: 'color: var(--color-text-secondary)' }, `#${row.entityId}`) : ''
    }
  },
  {
    title: '时间',
    key: 'createdAt',
    width: 160,
    render(row) {
      return h(NTime, { time: new Date(row.createdAt), format: 'yyyy-MM-dd HH:mm:ss' })
    }
  }
]

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

<style scoped>
.log-table {
  --n-merged-td-color: transparent;
}
</style>
