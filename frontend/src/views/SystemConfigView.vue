<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 p-4 md:p-5 pb-0">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <div class="sysconfig-header-icon">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"/>
            </svg>
          </div>
          <div>
            <h2 class="font-display text-lg md:text-xl">系统配置</h2>
            <p class="text-xs" style="color: var(--color-text-secondary)">管理运行参数和定时任务</p>
          </div>
        </div>
        <n-button secondary size="small" @click="refreshCache">刷新缓存</n-button>
      </div>

      <n-radio-group v-model:value="activeTab" class="mb-4 p-1 rounded-lg" style="background-color: var(--color-bg)">
        <n-radio-button v-for="tab in tabs" :key="tab.key" :value="tab.key" size="small">{{ tab.label }}</n-radio-button>
      </n-radio-group>
    </div>

    <n-spin v-if="loading" class="flex justify-center py-12" />
    <div v-else class="flex-1 overflow-y-auto px-4 md:px-6 pb-4 md:pb-6">

    <!-- ==================== Tab: 配置列表 ==================== -->
    <div v-if="activeTab === 'config'">
      <div class="flex items-center gap-2 mb-4">
        <n-input v-model:value="searchQuery" placeholder="搜索 key、描述或模块名..." clearable class="flex-1 min-w-0" />
        <span class="text-xs shrink-0" style="color: var(--color-text-secondary)">{{ filteredConfigs.length }} 项</span>
      </div>

      <div v-if="paginatedConfigs.length" class="space-y-2">
        <n-card v-for="cfg in paginatedConfigs" :key="cfg.configKey" size="small">
          <div class="flex items-start justify-between gap-2">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap mb-1">
                <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                  :style="{ backgroundColor: moduleColor(cfg) + '20', color: moduleColor(cfg) }">
                  {{ configModule(cfg) }}
                </span>
                <span class="text-xs px-1.5 py-0.5 rounded"
                  :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">
                  {{ cfg.valueType }}
                </span>
              </div>
              <p class="text-sm font-medium" style="color: var(--color-text)">{{ configLabel(cfg) }}</p>
              <p v-if="cfg.description" class="text-xs mt-0.5" style="color: var(--color-text-secondary)">{{ cfg.description }}</p>
              <code class="text-xs font-mono block mt-0.5" style="color: var(--color-warm-gray)">{{ cfg.configKey }}</code>
            </div>
            <div class="flex items-center gap-1 shrink-0">
              <n-button text size="tiny" @click="editConfig(cfg)">编辑</n-button>
              <n-button text size="tiny" type="error" @click="deleteConfig(cfg)">删除</n-button>
            </div>
          </div>
          <div class="text-sm break-all whitespace-pre-wrap max-h-24 overflow-y-auto rounded p-2 mt-2"
            :style="{ color: 'var(--color-text)', backgroundColor: 'var(--color-bg)' }">
            {{ displayValue(cfg) }}
          </div>
        </n-card>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center mt-4">
        <n-pagination :page="currentPage" :page-count="totalPages" @update:page="currentPage = $event" size="small" />
      </div>

      <p v-else-if="!paginatedConfigs.length" class="text-sm py-8 text-center" style="color: var(--color-text-secondary)">暂无匹配的配置项</p>
    </div>

    <!-- ==================== Tab: 定时任务 ==================== -->
    <div v-if="activeTab === 'tasks'">
      <p class="text-sm mb-4" style="color: var(--color-text-secondary)">
        修改后约 30 秒生效，无需重启
      </p>
      <div class="space-y-3">
        <n-card v-for="task in tasks" :key="task.key" size="small">
          <div class="flex items-start justify-between mb-3">
            <div>
              <div class="flex items-center gap-2 mb-0.5">
                <h4 class="text-sm font-medium" style="color: var(--color-text)">{{ task.label }}</h4>
                <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                  :style="task.enabled ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-text-secondary)' }">
                  {{ task.enabled ? '运行中' : '已停止' }}
                </span>
              </div>
              <p class="text-xs" style="color: var(--color-text-secondary)">{{ task.desc }}</p>
            </div>
            <n-switch :value="task.enabled" @update:value="toggleTask(task)" />
          </div>

          <!-- Schedule display -->
          <div class="flex items-center gap-2 text-sm mb-3" style="color: var(--color-text)">
            <span aria-hidden="true">&#9200;</span>
            <span v-if="task.cronType === 'cron'" class="font-medium">
              {{ task.humanTime }}
            </span>
            <span v-else class="font-medium">
              每 {{ task.humanInterval }}
            </span>
            <code class="text-xs font-mono" style="color: var(--color-warm-gray)">({{ task.cron }})</code>
          </div>

          <!-- Schedule editor -->
          <div v-if="task.enabled" class="flex items-center gap-2 flex-wrap">
            <!-- Cron task: hour + minute pickers -->
            <template v-if="task.cronType === 'cron'">
              <span class="text-xs" style="color: var(--color-text-secondary)">每天</span>
              <n-select v-model:value="task.editHour" :options="hourOptions" size="tiny" class="!w-20" @update:value="applyCronEdit(task)" />
              <span class="text-xs" style="color: var(--color-text-secondary)">:</span>
              <n-select v-model:value="task.editMin" :options="minuteOptions" size="tiny" class="!w-20" @update:value="applyCronEdit(task)" />
              <span class="text-xs" style="color: var(--color-text-secondary)">执行</span>
            </template>
            <!-- Poll task: minute input -->
            <template v-else>
              <span class="text-xs" style="color: var(--color-text-secondary)">每</span>
              <n-input-number v-model:value="task.editInterval" :min="1" size="tiny" class="!w-24" @update:value="applyPollEdit(task)" />
              <span class="text-xs" style="color: var(--color-text-secondary)">分钟</span>
            </template>
            <n-button text size="tiny" @click="resetTask(task)">重置默认</n-button>
          </div>
        </n-card>
      </div>
    </div>
    </div>

    <!-- Edit modal -->
    <n-modal v-model:show="editingShow" preset="card" style="max-width: 600px" :bordered="false" title="编辑配置">
      <code class="text-xs block mb-4" style="color: var(--color-accent)">{{ editing?.configKey }}</code>
      <n-space vertical size="medium">
        <n-input v-model:value="editValue" type="textarea" :rows="6" placeholder="值" />
        <n-input v-model:value="editDesc" placeholder="描述" />
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button @click="closeEdit">取消</n-button>
          <n-button type="primary" :loading="saving" @click="saveEdit">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { systemConfigApi } from '@/api/systemConfig'

const dialog = useDialog()
const PAGE_SIZE = 20

const configs = ref([])
const loading = ref(false)
const searchQuery = ref('')
const currentPage = ref(1)
const editing = ref(null)
const editingShow = ref(false)
const editValue = ref('')
const editDesc = ref('')
const saving = ref(false)
const activeTab = ref('config')

const hourOptions = Array.from({ length: 24 }, (_, i) => ({ label: String(i).padStart(2, '0'), value: i }))
const minuteOptions = Array.from({ length: 60 }, (_, i) => ({ label: String(i).padStart(2, '0'), value: i }))

const tabs = [
  { key: 'config', label: '配置列表' },
  { key: 'tasks', label: '定时任务' }
]

/* ---- Config helpers ---- */
const MODULE_COLORS = {
  '自动处理': '#7c3aed',
  '关联发现': '#0891b2',
  '每日复盘': '#ca8a04',
  '知识卡片': '#65a30d',
  '写作助手': '#d97706',
  'AI Agent': '#dc2626',
  '搜索增强': '#0284c7',
  'AI 对话': '#0d9488',
  '复习': '#9333ea',
  '数据备份': '#4f46e5',
  '数据导出': '#0891b2',
  '知识库': '#65a30d',
  '内容解析': '#ca8a04',
  'Token 用量': '#0d9488',
  '系统': '#78716c',
  '用户管理': '#4f46e5',
  '聚合': '#0891b2',
  '其他': '#78716c'
}

function moduleColor(cfg) {
  return MODULE_COLORS[configModule(cfg)] || '#78716c'
}

function configModule(cfg) {
  const k = cfg.configKey
  if (k.startsWith('prompt.auto.')) return '自动处理'
  if (k.startsWith('prompt.relation.')) return '关联发现'
  if (k.startsWith('prompt.daily-review.')) return '每日复盘'
  if (k.startsWith('prompt.flashcard.')) return '知识卡片'
  if (k.startsWith('prompt.writing.')) return '写作助手'
  if (k.startsWith('prompt.agent.')) return 'AI Agent'
  if (k.startsWith('prompt.search.')) return '搜索增强'
  if (k.startsWith('task.auto-process.')) return '自动处理'
  if (k.startsWith('task.backup')) return '数据备份'
  if (k.startsWith('task.daily-review')) return '每日复盘'
  if (k.startsWith('task.token-usage')) return 'Token 用量'
  if (k.startsWith('task.association')) return '关联发现'
  if (k.startsWith('task.vector-consistency')) return '系统'
  if (k.startsWith('threshold.auto.')) return '自动处理'
  if (k.startsWith('threshold.relation.')) return '关联发现'
  if (k.startsWith('threshold.aggregation.')) return '聚合'
  if (k.startsWith('threshold.search.')) return '搜索增强'
  if (k.startsWith('threshold.flashcard.')) return '知识卡片'
  if (k.startsWith('threshold.writing.')) return '写作助手'
  if (k.startsWith('threshold.agent.')) return 'AI Agent'
  if (k.startsWith('threshold.circuitbreaker.')) return '系统'
  if (k.startsWith('threshold.chat.')) return 'AI 对话'
  if (k.startsWith('threshold.daily-review.')) return '每日复盘'
  if (k.startsWith('threshold.review.')) return '复习'
  if (k.startsWith('threshold.tokenusage.')) return 'Token 用量'
  if (k.startsWith('threshold.session.')) return '系统'
  if (k.startsWith('threshold.logging.')) return '系统'
  if (k.startsWith('threshold.operationlog.')) return '系统'
  if (k.startsWith('threshold.ratelimit.')) return '系统'
  if (k.startsWith('threshold.system-config.')) return '系统'
  if (k.startsWith('threshold.association.')) return '关联发现'
  if (k.startsWith('default.export.')) return '数据导出'
  if (k.startsWith('default.knowledge.')) return '知识库'
  if (k.startsWith('default.content.')) return '内容解析'
  if (k.startsWith('default.chat.')) return 'AI 对话'
  if (k.startsWith('default.daily-review.')) return '每日复盘'
  if (k.startsWith('default.writing.')) return '写作助手'
  if (k.startsWith('default.agent.')) return 'AI Agent'
  if (k.startsWith('default.backup.')) return '数据备份'
  if (k.startsWith('default.user.')) return '用户管理'
  if (k.startsWith('default.system.')) return '系统'
  if (k.startsWith('default.llm.')) return 'AI Agent'
  if (k.startsWith('default.cors.')) return '系统'
  return '其他'
}

function configLabel(cfg) {
  const k = cfg.configKey
  const parts = k.split('.')
  const last = parts[parts.length - 1]
  return last
    .replace(/-/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\b\w/g, c => c.toUpperCase())
}

function displayValue(cfg) {
  if (!cfg.configValue) return '(空)'
  if (cfg.valueType === 'prompt') {
    const lines = cfg.configValue.split('\n')
    return lines.slice(0, 3).join('\n') + (lines.length > 3 ? '\n...' : '')
  }
  if (cfg.valueType === 'bool') return cfg.configValue === 'true' ? 'true' : 'false'
  const v = cfg.configValue
  return v.length > 200 ? v.slice(0, 200) + '...' : v
}

const filteredConfigs = computed(() => {
  const q = searchQuery.value.toLowerCase()
  if (!q) return configs.value
  return configs.value.filter(c =>
    c.configKey.toLowerCase().includes(q) ||
    (c.description || '').toLowerCase().includes(q) ||
    configModule(c).toLowerCase().includes(q) ||
    configLabel(c).toLowerCase().includes(q)
  )
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredConfigs.value.length / PAGE_SIZE)))

const paginatedConfigs = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE
  return filteredConfigs.value.slice(start, start + PAGE_SIZE)
})

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  if (cur <= 4) return [1, 2, 3, 4, 5, '...', total]
  if (cur >= total - 3) return [1, '...', total - 4, total - 3, total - 2, total - 1, total]
  return [1, '...', cur - 1, cur, cur + 1, '...', total]
})

watch(searchQuery, () => { currentPage.value = 1 })

/* ---- Data loading ---- */
async function loadConfigs() {
  loading.value = true
  try {
    const res = await systemConfigApi.list()
    configs.value = res.data.data || []
  } catch (e) {
    console.error('加载系统配置失败:', e)
  } finally {
    loading.value = false
  }
}

function editConfig(cfg) {
  editing.value = cfg
  editValue.value = cfg.configValue
  editDesc.value = cfg.description || ''
  editingShow.value = true
}

function closeEdit() {
  editing.value = null
  editingShow.value = false
}

async function saveEdit() {
  if (!editing.value) return
  saving.value = true
  try {
    await systemConfigApi.update(editing.value.configKey, {
      configValue: editValue.value,
      description: editDesc.value,
      valueType: editing.value.valueType
    })
    await loadConfigs()
    editing.value = null
    editingShow.value = false
  } finally {
    saving.value = false
  }
}

async function deleteConfig(cfg) {
  dialog.warning({
    title: '删除配置',
    content: `确定删除「${configLabel(cfg)}」(${cfg.configKey})？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await systemConfigApi.delete(cfg.configKey)
        await loadConfigs()
      } catch {}
    }
  })
}

async function refreshCache() {
  await systemConfigApi.refresh()
  await loadConfigs()
}

/* ---- Cron helpers ---- */
function cronToText(expr) {
  if (!expr) return ''
  const p = expr.trim().split(/\s+/)
  if (p.length < 6) return expr
  const [sec, min, hour] = p
  if (sec !== '0') return expr
  const h = String(Number(hour)).padStart(2, '0')
  const m = String(Number(min)).padStart(2, '0')
  return `每天 ${h}:${m}`
}

function msToText(ms) {
  const n = parseInt(ms)
  if (isNaN(n)) return ms
  return Math.round(n / 60000) + ' 分钟'
}

/* ---- Task defaults ---- */
const TASK_DEFAULTS = {
  'task.auto-process.round2': { cronType: 'poll', val: '300000' },
  'task.auto-process.round3': { cronType: 'poll', val: '1800000' },
  'task.backup': { cronType: 'cron', val: '0 0 3 * * ?' },
  'task.daily-review': { cronType: 'cron', val: '0 30 2 * * ?' },
  'task.token-usage': { cronType: 'cron', val: '0 30 3 * * ?' },
  'task.association': { cronType: 'cron', val: '0 0 2 * * ?' },
  'task.vector-consistency': { cronType: 'cron', val: '0 45 3 * * ?' }
}

const taskKeys = Object.keys(TASK_DEFAULTS)

const taskLabels = {
  'task.auto-process.round2': 'R2 关联发现',
  'task.auto-process.round3': 'R3 聚合分析',
  'task.backup': '自动备份',
  'task.daily-review': '每日复盘',
  'task.token-usage': 'Token 用量统计',
  'task.association': '知识关联发现（旧版）',
  'task.vector-consistency': '向量一致性检查'
}

const taskDescs = {
  'task.auto-process.round2': '扫描 PENDING 知识，通过向量+标签+LLM 发现关联关系',
  'task.auto-process.round3': '执行聚合统计，更新标签云，完成自动处理流程',
  'task.backup': '将全部知识库数据导出为 JSON 备份文件',
  'task.daily-review': '调用 LLM 生成昨日新增知识的复盘报告',
  'task.token-usage': '结算前一日 Token 消耗和费用统计',
  'task.association': '基于向量相似度发现知识间的关联',
  'task.vector-consistency': '检查所有知识的嵌入向量是否完整一致'
}

const tasks = computed(() => {
  return taskKeys.map(k => {
    const enabledCfg = configs.value.find(c => c.configKey === k + '.enabled')
    const cronCfg = configs.value.find(c => c.configKey === k + '.cron')
    const pollCfg = configs.value.find(c => c.configKey === k + '.poll-ms')
    const def = TASK_DEFAULTS[k]
    const cronType = def.cronType
    const rawCron = cronCfg ? cronCfg.configValue : (pollCfg ? pollCfg.configValue + 'ms' : '-')

    let editHour = 3, editMin = 0, editInterval = 5
    if (cronType === 'cron') {
      const p = rawCron.trim().split(/\s+/)
      if (p.length >= 6) {
        editHour = parseInt(p[2]) || 3
        editMin = parseInt(p[1]) || 0
      }
    } else {
      const ms = parseInt(rawCron)
      editInterval = ms ? Math.round(ms / 60000) : 5
    }

    return {
      key: k,
      label: taskLabels[k] || k,
      desc: taskDescs[k] || '',
      enabled: enabledCfg ? enabledCfg.configValue === 'true' : true,
      cronType,
      cron: rawCron,
      humanTime: cronType === 'cron' ? cronToText(rawCron) : null,
      humanInterval: cronType === 'poll' ? msToText(rawCron) : null,
      editHour,
      editMin,
      editInterval
    }
  })
})

async function toggleTask(task, enabled) {
  await systemConfigApi.update(task.key + '.enabled', {
    configValue: enabled ? 'true' : 'false',
    valueType: 'bool'
  })
  await loadConfigs()
}

function applyCronEdit(task) {
  const cron = `0 ${String(task.editMin).padStart(2, '0')} ${String(task.editHour).padStart(2, '0')} * * ?`
  doUpdateTask(task.key + '.cron', cron, 'cron')
}

function applyPollEdit(task) {
  const ms = (Math.max(1, task.editInterval || 1)) * 60000
  doUpdateTask(task.key + '.poll-ms', String(ms), 'int')
}

async function doUpdateTask(configKey, value, valueType) {
  try {
    await systemConfigApi.update(configKey, { configValue: value, valueType })
    await loadConfigs()
  } catch {}
}

async function resetTask(task) {
  const def = TASK_DEFAULTS[task.key]
  if (!def) return
  const configKey = task.key + (def.cronType === 'poll' ? '.poll-ms' : '.cron')
  await doUpdateTask(configKey, def.val, def.cronType === 'poll' ? 'int' : 'cron')
}

onMounted(loadConfigs)
</script>

<style scoped>
.sysconfig-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}
</style>
