<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <h2 class="font-display text-lg md:text-xl">用量统计</h2>
        <n-radio-group v-model:value="currentTab" size="small" @update:value="loadData">
          <n-radio-button v-for="t in tabs" :key="t.key" :value="t.key">{{ t.label }}</n-radio-button>
        </n-radio-group>
      </div>
      <div v-if="currentTab === 'daily'" class="flex items-center gap-2 mt-3">
        <span class="text-xs" style="color: var(--color-text-secondary)">最近</span>
        <n-select v-model:value="days" :options="dayOptions" size="tiny" class="w-24" @update:value="loadDaily" />
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="loading" class="flex justify-center py-12" />

      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">
        {{ error }}
      </n-alert>

      <template v-else-if="currentTab === 'daily'">
        <n-empty v-if="!groupedData.length" description="暂无用量数据" class="py-16" />

        <n-space v-else vertical size="large" class="max-w-5xl mx-auto">
          <n-card size="small">
            <template #header><span class="text-xs font-medium">每日 Token 消耗</span></template>
            <div class="space-y-1">
              <div v-for="row in groupedData" :key="row.date" class="flex items-center gap-2">
                <span class="text-xs shrink-0 w-16" style="color: var(--color-text-secondary)">{{ formatDateShort(row.date) }}</span>
                <div class="flex-1 h-6 rounded flex overflow-hidden" style="background-color: var(--color-bg)">
                  <div v-for="seg in row.segments" :key="seg.provider"
                    class="h-full transition-all duration-300"
                    :style="{ width: seg.pct + '%', backgroundColor: providerColor(seg.provider), minWidth: seg.pct > 0 ? '2px' : '0' }"
                    :title="`${seg.provider}: ${(seg.totalTokens / 1000).toFixed(1)}K`">
                  </div>
                </div>
                <span class="text-xs shrink-0 w-16 text-right" style="color: var(--color-text-secondary)">{{ (row.totalTokens / 1000).toFixed(0) }}K</span>
              </div>
            </div>
            <div class="flex items-center gap-4 mt-3 pt-3" style="border-top: 1px solid var(--color-border)">
              <span v-for="p in providers" :key="p" class="flex items-center gap-1.5 text-xs" style="color: var(--color-text-secondary)">
                <span class="w-2.5 h-2.5 rounded-sm" :style="{ backgroundColor: providerColor(p) }"></span>
                {{ p }}
              </span>
            </div>
          </n-card>

          <n-data-table
            :columns="dailyColumns"
            :data="dailyData"
            :bordered="false"
            :single-line="false"
            size="small"
          />
        </n-space>
      </template>

      <template v-else>
        <n-empty v-if="!totalStats" description="暂无统计数据" class="py-16" />

        <n-space v-else vertical size="large" class="max-w-4xl mx-auto">
          <n-grid :cols="4" :x-gap="12" :y-gap="12">
            <n-gi>
              <n-card size="small" class="stats-card">
                <n-statistic label="总 Token 数" :value="totalStats.totalTokens || 0" />
              </n-card>
            </n-gi>
            <n-gi>
              <n-card size="small" class="stats-card">
                <n-statistic label="总费用" :value="Number(totalStats.totalCost || 0).toFixed(4)" precision="4">
                  <template #prefix>$</template>
                </n-statistic>
              </n-card>
            </n-gi>
            <n-gi>
              <n-card size="small" class="stats-card">
                <n-statistic label="请求次数" :value="totalStats.requestCount || 0" />
              </n-card>
            </n-gi>
            <n-gi>
              <n-card size="small" class="stats-card">
                <n-statistic label="平均每次" :value="avgTokens" />
              </n-card>
            </n-gi>
          </n-grid>

          <n-card size="small" title="统计区间">
            <span class="text-sm" style="color: var(--color-warm-gray)">{{ totalStats.startDate }} ~ {{ totalStats.endDate }}</span>
          </n-card>
        </n-space>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { NTime } from 'naive-ui'
import { tokenUsageApi } from '@/api/tokenUsage'

const tabs = [
  { key: 'daily', label: '每日用量' },
  { key: 'total', label: '总计统计' }
]

const dayOptions = [
  { label: '7 天', value: 7 },
  { label: '14 天', value: 14 },
  { label: '30 天', value: 30 },
  { label: '90 天', value: 90 }
]

const currentTab = ref('daily')
const loading = ref(true)
const error = ref('')
const days = ref(30)
const dailyData = ref([])
const totalStats = ref(null)
const providers = ref([])
const groupedData = ref([])

const providerColors = {
  OPENAI: '#4a90d9',
  DEEPSEEK: '#4a9a8a',
  ALIYUN: '#d9a84a',
  GEMINI: '#8b5cf6'
}

function providerColor(p) {
  return providerColors[p] || 'var(--color-text-secondary)'
}

const avgTokens = computed(() => {
  if (!totalStats.value || !totalStats.value.requestCount) return 0
  return Math.round(totalStats.value.totalTokens / totalStats.value.requestCount)
})

const dailyColumns = [
  { title: '日期', key: 'date', width: 100 },
  {
    title: '提供商',
    key: 'provider',
    width: 100
  },
  { title: '模型', key: 'modelName' },
  {
    title: '输入',
    key: 'promptTokens',
    width: 90,
    sorter: (a, b) => a.promptTokens - b.promptTokens,
    render(row) { return h('span', row.promptTokens.toLocaleString()) }
  },
  {
    title: '输出',
    key: 'completionTokens',
    width: 90,
    sorter: (a, b) => a.completionTokens - b.completionTokens,
    render(row) { return h('span', row.completionTokens.toLocaleString()) }
  },
  {
    title: '总计',
    key: 'totalTokens',
    width: 90,
    sorter: (a, b) => a.totalTokens - b.totalTokens,
    render(row) { return h('span', { style: 'font-weight: 500' }, row.totalTokens.toLocaleString()) }
  },
  {
    title: '调用次数',
    key: 'requestCount',
    width: 80,
    sorter: (a, b) => a.requestCount - b.requestCount
  },
  {
    title: '费用 ($)',
    key: 'cost',
    width: 110,
    sorter: (a, b) => a.cost - b.cost,
    render(row) { return h('span', { style: 'font-family: monospace; color: var(--color-accent)' }, Number(row.cost).toFixed(6)) }
  }
]

function formatDateShort(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr + 'T00:00:00')
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function buildGrouped(data) {
  const byDate = {}
  const provSet = new Set()
  for (const item of data) {
    provSet.add(item.provider)
    const key = item.date
    if (!byDate[key]) byDate[key] = { date: key, segments: [], totalTokens: 0 }
    byDate[key].segments.push({ provider: item.provider, totalTokens: item.totalTokens })
    byDate[key].totalTokens += item.totalTokens
  }
  providers.value = Array.from(provSet)
  const maxTokens = Math.max(...Object.values(byDate).map(d => d.totalTokens), 1)
  const result = Object.values(byDate).sort((a, b) => a.date.localeCompare(b.date))
  for (const row of result) {
    row.segments.sort((a, b) => a.provider.localeCompare(b.provider))
    for (const seg of row.segments) {
      seg.pct = (seg.totalTokens / maxTokens) * 100
    }
  }
  return result
}

async function loadDaily() {
  loading.value = true
  error.value = ''
  try {
    const res = await tokenUsageApi.getDaily(days.value)
    dailyData.value = res.data.data || []
    groupedData.value = buildGrouped(dailyData.value)
  } catch {
    error.value = '加载每日用量失败'
  } finally {
    loading.value = false
  }
}

async function loadTotal() {
  loading.value = true
  error.value = ''
  try {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - days.value)
    const fmt = (d) => d.toISOString().slice(0, 10)
    const res = await tokenUsageApi.getTotal(fmt(start), fmt(end))
    totalStats.value = res.data.data || null
  } catch {
    error.value = '加载总计统计失败'
  } finally {
    loading.value = false
  }
}

function loadData() {
  if (currentTab.value === 'daily') loadDaily()
  else loadTotal()
}

onMounted(loadDaily)
</script>

<style scoped>
.stats-card {
  --n-padding-bottom: 16px;
}
</style>
