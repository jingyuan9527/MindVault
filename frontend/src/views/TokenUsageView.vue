<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 px-4 md:px-5 py-3" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3 mb-3">
        <div class="token-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">用量统计</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">跟踪 Token 消耗和 API 调用情况</p>
        </div>
      </div>
      <div class="flex items-center justify-between gap-3">
        <n-radio-group v-model:value="currentTab" size="small" @update:value="loadData">
          <n-radio-button v-for="t in tabs" :key="t.key" :value="t.key">{{ t.label }}</n-radio-button>
        </n-radio-group>
        <n-select v-if="currentTab === 'daily'" v-model:value="days" :options="dayOptions" size="tiny" class="w-24" @update:value="loadDaily" />
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="loading" class="flex justify-center py-12" />
      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">{{ error }}</n-alert>

      <template v-else-if="currentTab === 'daily'">
        <n-empty v-if="!groupedData.length" description="暂无用量数据" class="py-16" />
        <div v-else class="max-w-5xl mx-auto space-y-4">
          <div class="chart-card">
            <div class="chart-title">每日 Token 消耗趋势</div>
            <div class="chart-bars">
              <div v-for="row in groupedData" :key="row.date" class="chart-bar-col">
                <div class="chart-bar-stack">
                  <div v-for="seg in row.segments" :key="seg.provider"
                    class="chart-bar-seg"
                    :style="{ height: seg.pct + '%', backgroundColor: providerColor(seg.provider), minHeight: seg.pct > 0 ? '2px' : '0' }"
                    :title="`${seg.provider}: ${(seg.totalTokens / 1000).toFixed(1)}K`">
                  </div>
                </div>
                <span class="chart-bar-label">{{ formatDateShort(row.date) }}</span>
              </div>
            </div>
            <div class="chart-legend">
              <span v-for="p in providers" :key="p" class="legend-item">
                <span class="legend-dot" :style="{ backgroundColor: providerColor(p) }"></span>{{ p }}
              </span>
            </div>
          </div>
          <n-data-table :columns="dailyColumns" :data="dailyData" :bordered="false" :single-line="false" size="small" />
        </div>
      </template>

      <template v-else>
        <n-empty v-if="!totalStats" description="暂无统计数据" class="py-16" />
        <div v-else class="max-w-4xl mx-auto space-y-4">
          <div class="stats-grid">
            <div class="stat-box">
              <p class="stat-label">总 Token 数</p>
              <p class="stat-value">{{ (totalStats.totalTokens || 0).toLocaleString() }}</p>
            </div>
            <div class="stat-box">
              <p class="stat-label">总费用</p>
              <p class="stat-value" style="color: var(--color-accent)">${{ Number(totalStats.totalCost || 0).toFixed(4) }}</p>
            </div>
            <div class="stat-box">
              <p class="stat-label">请求次数</p>
              <p class="stat-value">{{ (totalStats.requestCount || 0).toLocaleString() }}</p>
            </div>
            <div class="stat-box">
              <p class="stat-label">平均每次</p>
              <p class="stat-value">{{ avgTokens.toLocaleString() }}</p>
            </div>
          </div>
          <div class="date-range">{{ totalStats.startDate }} ~ {{ totalStats.endDate }}</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { tokenUsageApi } from '@/api/tokenUsage'

const tabs = [{ key: 'daily', label: '每日用量' }, { key: 'total', label: '总计统计' }]
const dayOptions = [{ label: '7 天', value: 7 }, { label: '14 天', value: 14 }, { label: '30 天', value: 30 }, { label: '90 天', value: 90 }]
const currentTab = ref('daily')
const loading = ref(true)
const error = ref('')
const days = ref(30)
const dailyData = ref([])
const totalStats = ref(null)
const providers = ref([])
const groupedData = ref([])

const providerColors = { OPENAI: '#4a90d9', DEEPSEEK: '#4a9a8a', ALIYUN: '#d9a84a', GEMINI: '#8b5cf6' }
function providerColor(p) { return providerColors[p] || 'var(--color-text-secondary)' }

const avgTokens = computed(() => { if (!totalStats.value || !totalStats.value.requestCount) return 0; return Math.round(totalStats.value.totalTokens / totalStats.value.requestCount) })

const dailyColumns = [
  { title: '日期', key: 'date', width: 100 },
  { title: '提供商', key: 'provider', width: 100 },
  { title: '模型', key: 'modelName' },
  { title: '输入', key: 'promptTokens', width: 90, sorter: (a, b) => a.promptTokens - b.promptTokens, render(row) { return h('span', row.promptTokens.toLocaleString()) } },
  { title: '输出', key: 'completionTokens', width: 90, sorter: (a, b) => a.completionTokens - b.completionTokens, render(row) { return h('span', row.completionTokens.toLocaleString()) } },
  { title: '总计', key: 'totalTokens', width: 90, sorter: (a, b) => a.totalTokens - b.totalTokens, render(row) { return h('span', { style: 'font-weight: 500' }, row.totalTokens.toLocaleString()) } },
  { title: '调用次数', key: 'requestCount', width: 80, sorter: (a, b) => a.requestCount - b.requestCount },
  { title: '费用 ($)', key: 'cost', width: 110, sorter: (a, b) => a.cost - b.cost, render(row) { return h('span', { style: 'font-family: monospace; color: var(--color-accent)' }, Number(row.cost).toFixed(6)) } }
]

function formatDateShort(dateStr) { if (!dateStr) return ''; const d = new Date(dateStr + 'T00:00:00'); return `${d.getMonth() + 1}/${d.getDate()}` }

function buildGrouped(data) {
  const byDate = {}; const provSet = new Set()
  for (const item of data) { provSet.add(item.provider); const key = item.date; if (!byDate[key]) byDate[key] = { date: key, segments: [], totalTokens: 0 }; byDate[key].segments.push({ provider: item.provider, totalTokens: item.totalTokens }); byDate[key].totalTokens += item.totalTokens }
  providers.value = Array.from(provSet)
  const maxTokens = Math.max(...Object.values(byDate).map(d => d.totalTokens), 1)
  const result = Object.values(byDate).sort((a, b) => a.date.localeCompare(b.date))
  for (const row of result) { row.segments.sort((a, b) => a.provider.localeCompare(b.provider)); for (const seg of row.segments) seg.pct = (seg.totalTokens / maxTokens) * 100 }
  return result
}

async function loadDaily() { loading.value = true; error.value = ''; try { const res = await tokenUsageApi.getDaily(days.value); dailyData.value = res.data.data || []; groupedData.value = buildGrouped(dailyData.value) } catch { error.value = '加载每日用量失败' } finally { loading.value = false } }

async function loadTotal() { loading.value = true; error.value = ''; try { const end = new Date(); const start = new Date(); start.setDate(start.getDate() - days.value); const fmt = (d) => d.toISOString().slice(0, 10); const res = await tokenUsageApi.getTotal(fmt(start), fmt(end)); totalStats.value = res.data.data || null } catch { error.value = '加载总计统计失败' } finally { loading.value = false } }

function loadData() { if (currentTab.value === 'daily') loadDaily(); else loadTotal() }

onMounted(loadDaily)
</script>

<style scoped>
.token-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, #0891b2 0%, #065f73 100%);
}
.chart-card { padding: 20px; border-radius: 14px; background-color: var(--color-surface); border: 1px solid var(--color-border); }
.chart-title { font-size: 0.8rem; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 16px; }
.chart-bars { display: flex; align-items: flex-end; gap: 4px; height: 120px; }
.chart-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.chart-bar-stack { width: 100%; max-width: 28px; height: 100%; border-radius: 4px 4px 0 0; overflow: hidden; display: flex; flex-direction: column-reverse; background-color: var(--color-bg); }
.chart-bar-seg { width: 100%; transition: height 0.5s ease; }
.chart-bar-label { font-size: 0.6rem; color: var(--color-text-secondary); white-space: nowrap; }
.chart-legend { display: flex; gap: 16px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--color-border); }
.legend-item { display: flex; align-items: center; gap: 6px; font-size: 0.75rem; color: var(--color-text-secondary); }
.legend-dot { width: 8px; height: 8px; border-radius: 2px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-box { padding: 20px; border-radius: 14px; background-color: var(--color-surface); border: 1px solid var(--color-border); text-align: center; }
.stat-label { font-size: 0.75rem; color: var(--color-text-secondary); margin-bottom: 6px; }
.stat-value { font-size: 1.25rem; font-weight: 700; color: var(--color-text); font-variant-numeric: tabular-nums; }
.date-range { font-size: 0.8rem; color: var(--color-warm-gray); text-align: center; padding: 12px; background-color: var(--color-surface); border-radius: 10px; border: 1px solid var(--color-border); }
</style>