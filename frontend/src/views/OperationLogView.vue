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
          <p class="text-xs" style="color: var(--color-text-secondary)">审计所有用户操作行为，支持操作前后数据追溯</p>
        </div>
      </div>
      <div class="flex flex-wrap gap-1">
        <n-button v-for="m in modules" :key="m.value" size="tiny"
          :type="currentModule === m.value ? 'primary' : 'default'"
          @click="currentModule = m.value; page = 0; loadLogs()">{{ m.label }}</n-button>
        <n-button size="tiny" quaternary :loading="loading" class="ml-auto" @click="page = 0; loadLogs()">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
        </n-button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="loading" class="flex justify-center py-12" />
      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">{{ error }}</n-alert>
      <template v-else-if="logs.length">
        <n-data-table :columns="columns" :data="logs" :bordered="false" :single-line="false" size="small"
          class="log-table" />
        <div class="flex justify-end mt-3">
          <n-pagination :page="page + 1" :page-size="size" :item-count="total" @update:page="onPageChangeRaw" />
        </div>
      </template>
      <n-empty v-else description="暂无操作日志" class="py-16">
        <template #extra><p class="text-xs" style="color: var(--color-text-secondary)">进行知识库操作后日志将在此显示</p></template>
      </n-empty>
    </div>

    <n-drawer v-model:show="showDetail" :width="720" placement="right">
      <n-drawer-content title="操作日志详情" closable>
        <template v-if="detailLog">
          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div><span class="text-xs" style="color: var(--color-text-secondary)">模块</span><br><n-tag size="small" :type="tagType(detailLog.module)">{{ detailLog.module }}</n-tag></div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">操作</span><br><span class="font-medium">{{ detailLog.action }}</span></div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">操作人</span><br>{{ detailLog.operator || '-' }}</div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">IP 地址</span><br>{{ detailLog.ipAddress || '-' }}</div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">结果</span><br>
                <n-tag size="small" :type="detailLog.result === 'SUCCESS' ? 'success' : 'error'">{{ detailLog.result }}</n-tag>
              </div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">耗时</span><br>{{ detailLog.durationMs }}ms</div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">摘要</span><br>{{ detailLog.summary }}</div>
              <div><span class="text-xs" style="color: var(--color-text-secondary)">备注</span><br>{{ detailLog.remark || '-' }}</div>
            </div>
            <n-divider />
            <div v-if="errorMessage" class="p-3 rounded-lg" style="background: var(--color-error-bg, #fef2f2)">
              <span class="text-xs" style="color: var(--color-text-secondary)">错误信息</span>
              <p class="text-sm mt-1" style="color: var(--color-error)">{{ errorMessage }}</p>
            </div>
            <div v-if="beforeJson || afterJson">
              <h4 class="text-sm font-medium mb-2">数据快照对比</h4>
              <div class="grid grid-cols-1 gap-3" :class="{ 'md:grid-cols-2': beforeJson && afterJson }">
                <div v-if="beforeJson" class="card p-3">
                  <div class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">操作前</div>
                  <vue-json-pretty :data="beforeJson" :deep="2" showDoubleQuotes />
                </div>
                <div v-if="afterJson" class="card p-3">
                  <div class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">操作后</div>
                  <vue-json-pretty :data="afterJson" :deep="2" showDoubleQuotes />
                </div>
              </div>
            </div>
            <div v-else class="text-sm" style="color: var(--color-text-secondary)">该操作无数据快照</div>
            <div class="text-xs mt-2" style="color: var(--color-text-tertiary)">
              记录时间: {{ detailLog.createdAt ? new Date(detailLog.createdAt).toLocaleString() : '-' }}
            </div>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import { NTag, NTime, NButton } from 'naive-ui'
import { operationLogApi } from '@/api/operationLog'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'

const modules = [
  { value: '', label: '全部' },
  { value: '知识库', label: '知识库' },
  { value: '对话', label: '对话' },
  { value: '模型配置', label: '模型' },
  { value: '认证', label: '认证' },
  { value: '闪卡', label: '闪卡' },
  { value: '复习', label: '复习' },
  { value: '每日回顾', label: '回顾' },
  { value: '写作助手', label: '写作' },
  { value: '系统配置', label: '系统' },
  { value: '备份', label: '备份' },
  { value: '用户管理', label: '用户' }
]
const currentModule = ref('')
const logs = ref([])
const loading = ref(true)
const error = ref('')
const page = ref(0)
const size = ref(20)
const total = ref(0)

const showDetail = ref(false)
const detailLog = ref(null)
const beforeJson = ref(null)
const afterJson = ref(null)
const errorMessage = ref('')

const moduleTagType = {
  '知识库': 'success', '对话': 'info', '模型配置': 'warning',
  '认证': 'default', '闪卡': 'primary', '复习': 'success',
  '每日回顾': 'info', '写作助手': 'warning', '系统配置': 'error',
  '备份': 'default', '用户管理': 'error'
}

function tagType(module) { return moduleTagType[module] || 'default' }

const columns = [
  { title: '模块', key: 'module', width: 80,
    render(row) { return h(NTag, { size: 'small', type: tagType(row.module), bordered: false }, { default: () => row.module }) } },
  { title: '操作', key: 'action', width: 100,
    render(row) { return h('span', { style: 'font-weight: 500' }, row.action) } },
  { title: '摘要', key: 'summary', ellipsis: { tooltip: true } },
  { title: '操作人', key: 'operator', width: 80 },
  { title: '结果', key: 'result', width: 70,
    render(row) { return row.result === 'SUCCESS'
      ? h(NTag, { size: 'tiny', type: 'success', bordered: false }, { default: () => '成功' })
      : h(NTag, { size: 'tiny', type: 'error', bordered: false }, { default: () => '失败' }) } },
  { title: '耗时', key: 'durationMs', width: 70,
    render(row) { return h('span', { style: 'color: var(--color-text-secondary); font-size: 12px' }, `${row.durationMs}ms`) } },
  { title: '时间', key: 'createdAt', width: 155,
    render(row) { return h(NTime, { time: new Date(row.createdAt), format: 'yyyy-MM-dd HH:mm:ss' }) } },
  { title: '', key: 'actions', width: 50,
    render(row) { return h(NButton, { size: 'tiny', quaternary: true, onClick: () => openDetail(row.id) }, { default: () => '详情' }) } }
]

async function loadLogs() {
  loading.value = true; error.value = ''
  try {
    const res = await operationLogApi.list({ module: currentModule.value || undefined, page: page.value, size: size.value })
    const d = res.data.data
    if (Array.isArray(d)) {
      logs.value = d
      total.value = d.length
    } else {
      logs.value = d.records || []
      total.value = d.total || 0
    }
  } catch {
    error.value = '加载操作日志失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(id) {
  try {
    const res = await operationLogApi.detail(id)
    detailLog.value = res.data.data
    errorMessage.value = detailLog.value.errorMessage || ''
    try { beforeJson.value = detailLog.value.beforeSnapshot ? JSON.parse(detailLog.value.beforeSnapshot) : null } catch { beforeJson.value = null }
    try { afterJson.value = detailLog.value.afterSnapshot ? JSON.parse(detailLog.value.afterSnapshot) : null } catch { afterJson.value = null }
    showDetail.value = true
  } catch {
    error.value = '加载日志详情失败'
  }
}

function onPageChange(p) { page.value = p; loadLogs() }
function onPageChangeRaw(p) { page.value = p - 1; loadLogs() }

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
:deep(.vue-json-pretty) { font-size: 12px; font-family: 'JetBrains Mono', monospace; }
</style>