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
        <div class="flex items-center gap-2">
          <n-input v-model:value="globalSearch" placeholder="全局搜索配置..." clearable size="small" class="!w-48 md:!w-64" />
          <n-button secondary size="small" @click="refreshCache">刷新缓存</n-button>
        </div>
      </div>
    </div>

    <div class="flex-1 flex overflow-hidden px-4 md:px-6 pb-4 md:pb-6 gap-4">
      <!-- Left nav -->
      <div class="w-44 shrink-0 overflow-y-auto space-y-0.5 rounded-lg p-2" style="background-color: var(--color-bg)">
        <button
          v-for="mod in modules"
          :key="mod.id"
          class="w-full text-left px-3 py-2 rounded-md text-sm transition-colors"
          :class="selectedModule === mod.id ? 'font-semibold' : ''"
          :style="selectedModule === mod.id
            ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }
            : { color: 'var(--color-text-secondary)' }"
          @click="selectedModule = mod.id"
        >
          <span class="flex items-center gap-2">
            <span>{{ mod.label }}</span>
            <span v-if="moduleDirtyCount(mod.id)" class="text-xs ml-auto">{{ moduleDirtyCount(mod.id) }}</span>
          </span>
        </button>
      </div>

      <!-- Right panel -->
      <n-spin v-if="loading" class="flex justify-center py-12 w-full" />
      <div v-else class="flex-1 overflow-y-auto min-w-0 space-y-4">
        <!-- Scheduled tasks module -->
        <div v-if="selectedModule === 'scheduler'" class="space-y-3">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-display font-semibold" style="color: var(--color-text)">定时任务</h3>
            <span class="text-xs" style="color: var(--color-text-secondary)">修改后约 30 秒生效</span>
          </div>
          <n-card v-for="task in scheduledTasks" :key="task.id" size="small" :segmented="true">
            <div class="flex items-start justify-between mb-3">
              <div>
                <div class="flex items-center gap-2 mb-0.5">
                  <h4 class="text-sm font-medium" style="color: var(--color-text)">{{ task.label }}</h4>
                  <span
                    class="text-xs px-2 py-0.5 rounded-full font-medium"
                    :style="taskEnabled(task) ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-text-secondary)' }"
                  >
                    {{ taskEnabled(task) ? '运行中' : '已停止' }}
                  </span>
                </div>
                <p class="text-xs mt-0.5" style="color: var(--color-text-secondary)">{{ taskDescription(task.id) }}</p>
              </div>
              <n-switch :value="taskEnabled(task)" @update:value="v => toggleTask(task, v)" />
            </div>
            <div class="flex items-center gap-2 text-sm mb-3" style="color: var(--color-text)">
              <span aria-hidden="true">&#9200;</span>
              <span class="font-medium">{{ taskScheduleText(task) }}</span>
              <code v-if="taskExpanded[task.id]" class="text-xs font-mono" style="color: var(--color-warm-gray)">({{ rawTaskSchedule(task) }})</code>
            </div>
            <div v-if="taskEnabled(task)" class="flex items-center gap-2 flex-wrap">
              <template v-if="task.scheduleType === 'cron'">
                <span class="text-xs" style="color: var(--color-text-secondary)">每天</span>
                <n-select :value="getTaskEditHour(task.id)" :options="hourOptions" size="tiny" class="!w-20" @update:value="v => applyCronEdit(task, v, getTaskEditMin(task.id))" />
                <span class="text-xs" style="color: var(--color-text-secondary)">:</span>
                <n-select :value="getTaskEditMin(task.id)" :options="minuteOptions" size="tiny" class="!w-20" @update:value="v => applyCronEdit(task, getTaskEditHour(task.id), v)" />
                <span class="text-xs" style="color: var(--color-text-secondary)">执行</span>
              </template>
              <template v-else>
                <span class="text-xs" style="color: var(--color-text-secondary)">每</span>
                <n-input-number :value="getTaskEditInterval(task.id)" :min="1" size="tiny" class="!w-24" @update:value="v => applyPollEdit(task, v)" />
                <span class="text-xs" style="color: var(--color-text-secondary)">分钟</span>
              </template>
              <n-button text size="tiny" @click="resetTaskSchedule(task)">重置默认</n-button>
              <n-button text size="tiny" @click="toggleTaskExpand(task.id)">
                {{ taskExpanded[task.id] ? '收起' : '高级' }}
              </n-button>
            </div>
            <n-collapse-transition :show="!!taskExpanded[task.id]">
              <div class="mt-2 pt-2 border-t" style="border-color: var(--color-border-secondary)">
                <div class="text-xs mb-1" style="color: var(--color-text-secondary)">原始值编辑</div>
                <n-input
                  v-if="task.scheduleType === 'cron'"
                  :value="taskRawCache[task.id]"
                  size="tiny"
                  placeholder="0 0 3 * * ?"
                  @update:value="v => taskRawCache[task.id] = v"
                  @blur="applyRawTask(task)"
                />
                <n-input-number
                  v-else
                  :value="taskRawMsCache[task.id]"
                  :min="60000"
                  :step="60000"
                  size="tiny"
                  class="!w-32"
                  @update:value="v => taskRawMsCache[task.id] = v"
                  @blur="applyRawMsTask(task)"
                />
              </div>
            </n-collapse-transition>
          </n-card>
        </div>

        <!-- Feature module panel -->
        <div v-else-if="currentModule">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-display font-semibold" style="color: var(--color-text)">{{ currentModule.label }}</h3>
            <div class="flex items-center gap-2">
              <n-input v-model:value="moduleFilter" placeholder="过滤配置..." clearable size="small" class="!w-36" />
              <n-button type="primary" size="small" :loading="savingModule" :disabled="moduleDirtyCount(selectedModule) === 0" @click="saveModule(selectedModule)">
                保存模块 ({{ moduleDirtyCount(selectedModule) }})
              </n-button>
            </div>
          </div>

          <div v-for="group in filteredGroups" :key="group.id" class="mb-4">
            <h4 class="text-xs font-semibold uppercase tracking-wider mb-2 px-1" style="color: var(--color-text-secondary)">{{ group.label }}</h4>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
              <div
                v-for="item in group.items"
                :key="item.key"
                class="config-card rounded-lg p-3 transition-shadow"
                :class="{ 'ring-2': dirtyItems.has(item.key) }"
                :style="{
                  backgroundColor: 'var(--color-bg)',
                  '--ring-color': 'var(--color-accent)'
                }"
              >
                <div class="flex items-start justify-between gap-2 mb-1">
                  <div class="flex-1 min-w-0">
                    <p class="text-sm font-medium truncate" style="color: var(--color-text)" :title="item.description || ''">
                      {{ item.description || friendlyKey(item.key) }}
                    </p>
                    <code class="text-xs font-mono block truncate" style="color: var(--color-warm-gray)">{{ item.key }}</code>
                  </div>
                  <n-button text size="tiny" style="color: var(--color-text-secondary)" @click="toggleItemExpand(item.key)">
                    {{ expandedItems[item.key] ? '收起' : '高级' }}
                  </n-button>
                </div>

                <!-- Inline editor -->
                <div v-if="item.valueType === 'bool'" class="mt-2">
                  <n-switch
                    :value="dirtyItems.has(item.key) ? dirtyItems.get(item.key) : item.value"
                    @update:value="v => setDirty(item.key, v, item.valueType)"
                  />
                </div>
                <div v-else-if="item.valueType === 'int' || item.valueType === 'double'" class="mt-2">
                  <div class="flex items-center gap-2">
                    <n-input-number
                      :value="Number(dirtyItems.has(item.key) ? dirtyItems.get(item.key) : item.value)"
                      :min="item.validation?.min"
                      :max="item.validation?.max"
                      :step="item.valueType === 'double' ? 0.1 : 1"
                      size="tiny"
                      class="!w-28"
                      @update:value="v => setDirty(item.key, String(v), item.valueType)"
                    />
                    <span class="text-xs" style="color: var(--color-text-secondary)">
                      {{ item.validation ? `[${item.validation.min} – ${item.validation.max}]` : '' }}
                    </span>
                  </div>
                </div>
                <div v-else class="mt-2">
                  <n-input
                    :value="dirtyItems.has(item.key) ? dirtyItems.get(item.key) : item.value"
                    :type="item.valueType === 'prompt' ? 'textarea' : 'text'"
                    :rows="item.valueType === 'prompt' ? 3 : 1"
                    size="tiny"
                    :placeholder="item.defaultValue || ''"
                    @update:value="v => setDirty(item.key, v, item.valueType)"
                  />
                </div>

                <!-- Expand: raw value + default + audit -->
                <n-collapse-transition :show="!!expandedItems[item.key]">
                  <div class="mt-2 pt-2 space-y-2 border-t" style="border-color: var(--color-border-secondary)">
                    <div>
                      <span class="text-xs" style="color: var(--color-text-secondary)">原始值</span>
                      <n-input
                        :value="dirtyItems.has(item.key) ? dirtyItems.get(item.key) : item.value"
                        type="textarea"
                        :rows="4"
                        size="tiny"
                        @update:value="v => setDirty(item.key, v, item.valueType)"
                      />
                    </div>
                    <div v-if="item.defaultValue !== undefined && item.defaultValue !== null" class="flex items-center justify-between">
                      <span class="text-xs" style="color: var(--color-text-secondary)">默认值</span>
                      <n-button v-if="item.value !== item.defaultValue" text size="tiny" style="color: var(--color-sage)" @click="restoreDefault(item)">
                        恢复默认
                      </n-button>
                    </div>
                    <div v-if="item.updatedAt" class="text-xs" style="color: var(--color-text-secondary)">
                      上次修改: {{ timeAgo(item.updatedAt) }}
                    </div>
                    <div v-if="currentAudit[item.key]?.length" class="text-xs" style="color: var(--color-text-secondary)">
                      最近操作: {{ currentAudit[item.key]?.[0]?.operator || 'unknown' }}
                    </div>
                  </div>
                </n-collapse-transition>

                <!-- Validation hint -->
                <div v-if="item.validation?.options" class="mt-1 text-xs" style="color: var(--color-warm-gray)">
                  可选值: {{ item.validation.options.join(', ') }}
                </div>
              </div>
            </div>
          </div>

          <p v-if="!filteredGroups.length" class="text-sm py-8 text-center" style="color: var(--color-text-secondary)">暂无匹配的配置项</p>
        </div>
      </div>
    </div>

    <!-- Restore default diff modal -->
    <n-modal v-model:show="restoreModalShow" preset="card" class="opaque-modal" style="max-width: 640px; background-color: var(--color-bg) !important;" :bordered="false" title="恢复默认值">
      <div v-if="restoreTarget" class="space-y-3">
        <code class="text-xs block" style="color: var(--color-accent)">{{ restoreTarget.key }}</code>
        <div class="text-xs font-semibold" style="color: var(--color-text-secondary)">当前值</div>
        <pre class="text-xs p-2 rounded whitespace-pre-wrap max-h-32 overflow-y-auto" style="background-color: var(--color-bg-deep); color: var(--color-text)">{{ restoreTarget.value }}</pre>
        <div class="text-xs font-semibold" style="color: var(--color-text-secondary)">默认值</div>
        <pre class="text-xs p-2 rounded whitespace-pre-wrap max-h-32 overflow-y-auto" style="background-color: var(--color-bg-deep); color: var(--color-sage)">{{ restoreTarget.defaultValue }}</pre>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="restoreModalShow = false">取消</n-button>
          <n-button type="primary" @click="confirmRestore">确认恢复</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { systemConfigApi } from '@/api/systemConfig'

const dialog = useDialog()
const message = useMessage()

const modules = ref([])
const flatConfigs = ref({})
const loading = ref(false)
const selectedModule = ref('knowledge')
const globalSearch = ref('')
const moduleFilter = ref('')
const savingModule = ref(false)

const dirtyItems = ref(new Map())
const expandedItems = ref({})
const currentAudit = ref({})
const taskExpanded = ref({})
const taskEditCache = ref({})
const taskRawCache = ref({})
const taskRawMsCache = ref({})

const restoreModalShow = ref(false)
const restoreTarget = ref(null)

const scheduledTasksMeta = ref([])

const hourOptions = Array.from({ length: 24 }, (_, i) => ({ label: String(i).padStart(2, '0'), value: i }))
const minuteOptions = Array.from({ length: 60 }, (_, i) => ({ label: String(i).padStart(2, '0'), value: i }))

const TASK_DESCRIPTIONS = {
  'auto-process-round2': '扫描 PENDING 知识，通过向量+标签+LLM 发现关联关系',
  'auto-process-round3': '执行聚合统计，更新标签云，完成自动处理流程',
  'backup': '将全部知识库数据导出为 JSON 备份文件',
  'daily-review': '调用 LLM 生成昨日新增知识的复盘报告',
  'token-usage': '结算前一日 Token 消耗和费用统计',
  'association': '基于向量相似度发现知识间的关联',
  'vector-consistency': '检查所有知识的嵌入向量是否完整一致'
}

const TASK_DEFAULT_VALUES = {
  'auto-process-round2': 300000,
  'auto-process-round3': 1800000,
  'backup': '0 0 3 * * ?',
  'daily-review': '0 30 2 * * ?',
  'token-usage': '0 30 3 * * ?',
  'association': '0 0 2 * * ?',
  'vector-consistency': '0 45 3 * * ?'
}

function friendlyKey(key) {
  const parts = key.split('.')
  const last = parts[parts.length - 1]
  return last
    .replace(/-/g, ' ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/\b\w/g, c => c.toUpperCase())
}

function taskDescription(id) {
  return TASK_DESCRIPTIONS[id] || ''
}

function taskEnabled(task) {
  const key = task.enabledKey
  return flatConfigs.value[key]?.value === 'true'
}

function rawTaskSchedule(task) {
  const key = task.scheduleKey
  return flatConfigs.value[key]?.value || ''
}

function taskScheduleText(task) {
  const raw = rawTaskSchedule(task)
  if (!raw) return '-'
  if (task.scheduleType === 'cron') {
    const p = raw.trim().split(/\s+/)
    if (p.length >= 6 && p[0] === '0') {
      return `每天 ${String(Number(p[2])).padStart(2, '0')}:${String(Number(p[1])).padStart(2, '0')}`
    }
    return raw
  }
  return `每 ${Math.round(parseInt(raw) / 60000)} 分钟`
}

const currentModule = computed(() => {
  return modules.value.find(m => m.id === selectedModule.value)
})

const filteredGroups = computed(() => {
  const mod = currentModule.value
  if (!mod) return []
  const q = (globalSearch.value + moduleFilter.value).toLowerCase()
  if (!q) return mod.groups || []
  return (mod.groups || []).map(g => ({
    ...g,
    items: g.items.filter(item =>
      item.key.toLowerCase().includes(q) ||
      (item.description || '').toLowerCase().includes(q) ||
      (friendlyKey(item.key)).toLowerCase().includes(q)
    )
  })).filter(g => g.items.length > 0)
})

const scheduledTasks = computed(() => {
  return (scheduledTasksMeta.value || []).map(task => {
    const enabledCfg = flatConfigs.value[task.enabledKey]
    const scheduleCfg = flatConfigs.value[task.scheduleKey]
    const defaultValue = TASK_DEFAULT_VALUES[task.id]
    return {
      ...task,
      enabled: enabledCfg?.value === 'true',
      enabledConfig: enabledCfg,
      scheduleConfig: scheduleCfg,
      rawSchedule: scheduleCfg?.value || String(defaultValue || '')
    }
  })
})

function toggleItemExpand(key) {
  expandedItems.value = { ...expandedItems.value, [key]: !expandedItems.value[key] }
  if (expandedItems.value[key] && !currentAudit.value[key]) {
    loadAudit(key)
  }
}

function getTaskEditHour(taskId) {
  return taskEditCache.value[taskId]?.hour ?? 3
}

function getTaskEditMin(taskId) {
  return taskEditCache.value[taskId]?.min ?? 0
}

function getTaskEditInterval(taskId) {
  return taskEditCache.value[taskId]?.interval ?? 5
}

function toggleTaskExpand(id) {
  taskExpanded.value = { ...taskExpanded.value, [id]: !taskExpanded.value[id] }
  if (taskExpanded.value[id]) {
    const task = scheduledTasksMeta.value.find(t => t.id === id)
    if (task) {
      const raw = rawTaskSchedule(task)
      taskRawCache.value[id] = raw
      taskRawMsCache.value[id] = parseInt(raw) || 60000
      const p = raw.trim().split(/\s+/)
      if (p.length >= 6) {
        taskEditCache.value[id] = {
          hour: parseInt(p[2]) || 3,
          min: parseInt(p[1]) || 0,
          interval: Math.round(parseInt(raw) / 60000) || 5
        }
      } else {
        taskEditCache.value[id] = {
          hour: 3,
          min: 0,
          interval: Math.round(parseInt(raw) / 60000) || 5
        }
      }
    }
  }
}

function setDirty(key, value, valueType) {
  const newMap = new Map(dirtyItems.value)
  const original = flatConfigs.value[key]?.value
  if (value === original || (original === undefined && value === '')) {
    newMap.delete(key)
  } else {
    newMap.set(key, value)
  }
  dirtyItems.value = newMap
}

function moduleDirtyCount(moduleId) {
  const mod = modules.value.find(m => m.id === moduleId)
  if (!mod) return 0
  let count = 0
  for (const [key] of dirtyItems.value) {
    if (flatConfigs.value[key]?.module === moduleId) count++
  }
  return count
}

async function saveModule(moduleId) {
  savingModule.value = true
  const items = []
  for (const [key, value] of dirtyItems.value) {
    if (flatConfigs.value[key]?.module === moduleId) {
      items.push({ key, value })
    }
  }
  try {
    await Promise.all(items.map(({ key, value }) =>
      systemConfigApi.update(key, {
        configValue: value,
        valueType: flatConfigs.value[key]?.valueType || 'string',
        description: flatConfigs.value[key]?.description || ''
      })
    ))
    const newMap = new Map(dirtyItems.value)
    for (const { key } of items) newMap.delete(key)
    dirtyItems.value = newMap
    await loadData()
    message.success(`已保存 ${items.length} 项`)
  } catch {
    message.error('保存失败')
  } finally {
    savingModule.value = false
  }
}

function restoreDefault(item) {
  restoreTarget.value = item
  restoreModalShow.value = true
}

async function confirmRestore() {
  if (!restoreTarget.value) return
  const item = restoreTarget.value
  await systemConfigApi.update(item.key, {
    configValue: item.defaultValue,
    valueType: item.valueType || 'string',
    description: item.description || ''
  })
  restoreModalShow.value = false
  restoreTarget.value = null
  await loadData()
  message.success('已恢复默认值')
}

async function toggleTask(task, enabled) {
  await systemConfigApi.update(task.enabledKey, {
    configValue: enabled ? 'true' : 'false',
    valueType: 'bool'
  })
  await loadData()
}

function applyCronEdit(task, hour, min) {
  const cron = `0 ${String(min).padStart(2, '0')} ${String(hour).padStart(2, '0')} * * ?`
  systemConfigApi.update(task.scheduleKey, { configValue: cron, valueType: 'cron' }).then(loadData)
}

function applyPollEdit(task, interval) {
  const ms = String(Math.max(1, interval || 1) * 60000)
  systemConfigApi.update(task.scheduleKey, { configValue: ms, valueType: 'int' }).then(loadData)
}

function applyRawTask(task) {
  const val = taskRawCache.value[task.id]
  if (val) {
    systemConfigApi.update(task.scheduleKey, { configValue: val, valueType: 'cron' }).then(loadData)
  }
}

function applyRawMsTask(task) {
  const val = taskRawMsCache.value[task.id]
  if (val) {
    systemConfigApi.update(task.scheduleKey, { configValue: String(val), valueType: 'int' }).then(loadData)
  }
}

function resetTaskSchedule(task) {
  const def = TASK_DEFAULT_VALUES[task.id]
  if (!def) return
  systemConfigApi.update(task.scheduleKey, { configValue: String(def), valueType: task.scheduleType === 'cron' ? 'cron' : 'int' }).then(loadData)
}

async function loadAudit(key) {
  try {
    const res = await systemConfigApi.getAudit(key)
    currentAudit.value[key] = res.data.data || []
  } catch {}
}

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  return `${days} 天前`
}

async function refreshCache() {
  await systemConfigApi.refresh()
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [modRes, listRes, taskRes] = await Promise.all([
      systemConfigApi.getModules(),
      systemConfigApi.list(),
      systemConfigApi.getScheduledTasks()
    ])
    modules.value = modRes.data.data?.modules || []
    scheduledTasksMeta.value = taskRes.data.data || []
    const cfgMap = {}
    for (const cfg of listRes.data.data || []) {
      cfgMap[cfg.configKey] = {
        key: cfg.configKey,
        value: cfg.configValue,
        valueType: cfg.valueType,
        description: cfg.description,
        module: cfg.module || 'system',
        updatedAt: cfg.updatedAt
      }
    }
    flatConfigs.value = cfgMap
  } catch (e) {
    console.error('加载配置失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.sysconfig-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}

.opaque-modal :deep(.n-card) {
  background-color: var(--color-bg) !important;
}

.config-card {
  position: relative;
}
.config-card.ring-2 {
  box-shadow: 0 0 0 2px var(--ring-color, var(--color-accent));
}
</style>