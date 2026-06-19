<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between">
        <h2 class="font-display text-lg md:text-xl">用量统计</h2>
        <div class="flex items-center gap-1 p-0.5 rounded-lg" style="background-color: var(--color-bg)">
          <button v-for="t in tabs" :key="t.key" @click="currentTab = t.key; loadData()"
            class="px-3 py-1.5 text-sm rounded-lg transition-all duration-150"
            :style="currentTab === t.key
              ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)', fontWeight: 500 }
              : { color: 'var(--color-text-secondary)' }">
            {{ t.label }}
          </button>
        </div>
      </div>
      <div v-if="currentTab === 'daily'" class="flex items-center gap-2 mt-3">
        <span class="text-xs" style="color: var(--color-text-secondary)">最近</span>
        <select v-model.number="days" @change="loadDaily"
          class="text-xs px-2 py-1 rounded" style="border: 1px solid var(--color-border); background-color: var(--color-surface); color: var(--color-text)">
          <option :value="7">7 天</option>
          <option :value="14">14 天</option>
          <option :value="30">30 天</option>
          <option :value="90">90 天</option>
        </select>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin" style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="error" class="card p-6" :style="{ borderLeft: '3px solid var(--color-accent)' }">
        <p style="color: var(--color-accent)">{{ error }}</p>
      </div>

      <template v-else-if="currentTab === 'daily'">
        <div v-if="!groupedData.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
          <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">暂无用量数据</p>
        </div>

        <div v-else class="max-w-5xl mx-auto space-y-6">
          <div class="card p-4 md:p-5">
            <p class="text-xs font-medium mb-3" style="color: var(--color-text-secondary)">每日 Token 消耗</p>
            <div class="space-y-1">
              <div v-for="row in groupedData" :key="row.date" class="flex items-center gap-2">
                <span class="text-xs shrink-0 w-16" style="color: var(--color-text-secondary)">{{ formatDateShort(row.date) }}</span>
                <div class="flex-1 h-6 rounded flex" style="background-color: var(--color-bg); overflow: hidden;">
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
          </div>

          <div class="card">
            <div class="overflow-x-auto">
              <table class="w-full text-xs md:text-sm">
                <thead>
                  <tr style="border-bottom: 1px solid var(--color-border); background-color: var(--color-bg)">
                    <th class="text-left p-3 font-medium" style="color: var(--color-text-secondary)">日期</th>
                    <th class="text-left p-3 font-medium" style="color: var(--color-text-secondary)">提供商</th>
                    <th class="text-left p-3 font-medium" style="color: var(--color-text-secondary)">模型</th>
                    <th class="text-right p-3 font-medium" style="color: var(--color-text-secondary)">输入</th>
                    <th class="text-right p-3 font-medium" style="color: var(--color-text-secondary)">输出</th>
                    <th class="text-right p-3 font-medium" style="color: var(--color-text-secondary)">总计</th>
                    <th class="text-right p-3 font-medium" style="color: var(--color-text-secondary)">调用次数</th>
                    <th class="text-right p-3 font-medium" style="color: var(--color-text-secondary)">费用 ($)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, idx) in dailyData" :key="idx"
                    class="transition-colors duration-150 hover-sage-bg"
                    style="border-bottom: 1px solid var(--color-border)">
                    <td class="p-3" style="color: var(--color-text)">{{ formatDateShort(item.date) }}</td>
                    <td class="p-3">
                      <span class="tag-pill text-xs">{{ item.provider }}</span>
                    </td>
                    <td class="p-3" style="color: var(--color-warm-gray)">{{ item.modelName }}</td>
                    <td class="p-3 text-right" style="color: var(--color-warm-gray)">{{ (item.promptTokens).toLocaleString() }}</td>
                    <td class="p-3 text-right" style="color: var(--color-warm-gray)">{{ (item.completionTokens).toLocaleString() }}</td>
                    <td class="p-3 text-right font-medium" style="color: var(--color-text)">{{ (item.totalTokens).toLocaleString() }}</td>
                    <td class="p-3 text-right" style="color: var(--color-warm-gray)">{{ item.requestCount }}</td>
                    <td class="p-3 text-right font-mono" style="color: var(--color-accent)">{{ Number(item.cost).toFixed(6) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <div v-if="!totalStats" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
          <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">暂无统计数据</p>
        </div>

        <div v-else class="max-w-4xl mx-auto space-y-6">
          <div class="grid grid-cols-2 md:grid-cols-4 gap-3 md:gap-4">
            <div class="card p-4 text-center">
              <p class="text-xs font-medium mb-1" style="color: var(--color-text-secondary)">总 Token 数</p>
              <p class="text-xl md:text-2xl font-bold font-mono" style="color: var(--color-text)">{{ (totalStats.totalTokens || 0).toLocaleString() }}</p>
            </div>
            <div class="card p-4 text-center">
              <p class="text-xs font-medium mb-1" style="color: var(--color-text-secondary)">总费用</p>
              <p class="text-xl md:text-2xl font-bold font-mono" style="color: var(--color-accent)">${{ (totalStats.totalCost || 0).toFixed(4) }}</p>
            </div>
            <div class="card p-4 text-center">
              <p class="text-xs font-medium mb-1" style="color: var(--color-text-secondary)">请求次数</p>
              <p class="text-xl md:text-2xl font-bold font-mono" style="color: var(--color-text)">{{ (totalStats.requestCount || 0).toLocaleString() }}</p>
            </div>
            <div class="card p-4 text-center">
              <p class="text-xs font-medium mb-1" style="color: var(--color-text-secondary)">平均每次</p>
              <p class="text-xl md:text-2xl font-bold font-mono" style="color: var(--color-text)">
                {{ totalStats.requestCount > 0 ? Math.round(totalStats.totalTokens / totalStats.requestCount).toLocaleString() : 0 }}
              </p>
            </div>
          </div>

          <div class="card p-4 md:p-5">
            <p class="text-xs font-medium mb-1" style="color: var(--color-text-secondary)">统计区间</p>
            <p class="text-sm" style="color: var(--color-warm-gray)">
              {{ totalStats.startDate }} ~ {{ totalStats.endDate }}
            </p>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { tokenUsageApi } from '@/api/tokenUsage'

const tabs = [
  { key: 'daily', label: '每日用量' },
  { key: 'total', label: '总计统计' }
]

const currentTab = ref('daily')
const loading = ref(true)
const error = ref('')
const days = ref(30)
const dailyData = ref([])
const totalStats = ref(null)

const providerColors = {
  OPENAI: '#4a90d9',
  DEEPSEEK: '#4a9a8a',
  ALIYUN: '#d9a84a',
  GEMINI: '#8b5cf6'
}

function providerColor(p) {
  return providerColors[p] || 'var(--color-text-secondary)'
}

const providers = ref([])

const groupedData = ref([])

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
    byDate[key].segments.push({
      provider: item.provider,
      totalTokens: item.totalTokens
    })
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