<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 px-4 md:px-5 py-3" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3 mb-3">
        <div class="log-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">操作日志</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">审计所有用户操作行为</p>
        </div>
      </div>
      <div class="flex flex-wrap gap-1">
        <n-button v-for="m in modules" :key="m.value" size="tiny"
          :type="currentModule === m.value ? 'primary' : 'default'"
          @click="currentModule = m.value; loadLogs()">{{ m.label }}</n-button>
        <n-button size="tiny" quaternary :loading="loading" @click="loadLogs" class="ml-auto">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
        </n-button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="loading" class="flex justify-center py-12" />
      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">{{ error }}</n-alert>
      <n-data-table v-else-if="logs.length" :columns="columns" :data="logs" :bordered="false" :single-line="false" size="small" class="log-table" />
      <n-empty v-else description="暂无操作日志" class="py-16">
        <template #extra><p class="text-xs" style="color: var(--color-text-secondary)">进行知识库操作后日志将在此显示</p></template>
      </n-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NTime } from 'naive-ui'
import { operationLogApi } from '@/api/operationLog'

const modules = [{ value: '', label: '全部' }, { value: 'KNOWLEDGE', label: '知识库' }, { value: 'CHAT', label: '对话' }, { value: 'MODEL', label: '模型' }, { value: 'SYSTEM', label: '系统' }]
const currentModule = ref('')
const logs = ref([])
const loading = ref(true)
const error = ref('')

const actionMap = { ADD: '新增', UPDATE: '更新', DELETE: '删除', SEARCH: '搜索', EXPORT: '导出', IMPORT: '导入', TAG: '标记', TEST: '测试', ERROR: '错误', CREATE_SESSION: '创建会话', SEND_MESSAGE: '发送消息', PERFORM: '执行', BACKUP: '备份', SET_PRIMARY: '设为默认', UPDATE_PRIORITY: '更新优先级' }

const columns = [
  { title: '模块', key: 'module', width: 90, render(row) { return h(NTag, { size: 'small', type: (moduleTagType[row.module] || 'default'), bordered: false }, { default: () => row.module }) } },
  { title: '操作', key: 'action', width: 100, render(row) { return h('span', { style: 'font-weight: 500' }, actionMap[row.action] || row.action) } },
  { title: '摘要', key: 'summary', ellipsis: { tooltip: true } },
  { title: 'ID', key: 'entityId', width: 80, render(row) { return row.entityId ? h('span', { style: 'color: var(--color-text-secondary)' }, `#${row.entityId}`) : '' } },
  { title: '时间', key: 'createdAt', width: 160, render(row) { return h(NTime, { time: new Date(row.createdAt), format: 'yyyy-MM-dd HH:mm:ss' }) } }
]

const moduleTagType = { KNOWLEDGE: 'success', CHAT: 'info', MODEL: 'warning', SYSTEM: 'error' }

async function loadLogs() { loading.value = true; error.value = ''; try { const res = await operationLogApi.list(currentModule.value || undefined); logs.value = res.data.data || [] } catch { error.value = '加载操作日志失败' } finally { loading.value = false } }

onMounted(loadLogs)
</script>

<style scoped>
.log-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}
.log-table { --n-merged-td-color: transparent; }
</style>