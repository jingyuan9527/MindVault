<template>
  <div class="flex h-full">
    <div class="w-60 shrink-0 hidden md:flex flex-col" style="border-right: 1px solid var(--color-border)">
      <div class="p-3 shrink-0" style="border-bottom: 1px solid var(--color-border)">
        <p class="text-xs font-medium" style="color: var(--color-text-secondary)">历史回顾</p>
      </div>
      <div class="flex-1 overflow-y-auto p-2 space-y-1">
        <div
          v-for="r in recentReports" :key="r.id"
          class="px-3 py-2 rounded-lg text-sm cursor-pointer transition-all duration-150"
          :style="selectedDate === r.reportDate
            ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)', fontWeight: 500 }
            : { color: 'var(--color-text-secondary)' }"
          @click="loadByDate(r.reportDate)"
        >
          <p class="text-xs font-medium">{{ r.reportDate }}</p>
          <p class="text-xs mt-0.5 truncate opacity-70">{{ r.totalCount || 0 }} 条知识</p>
        </div>
        <p v-if="!recentReports.length && !loadingRecent" class="text-xs px-3 py-4 text-center" style="color: var(--color-text-secondary)">
          暂无历史记录
        </p>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0">
      <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
        <div class="flex items-center justify-between flex-wrap gap-3">
          <div class="flex items-center gap-3">
            <h2 class="font-display text-lg md:text-xl">每日复盘</h2>
            <n-date-picker v-model:formatted-value="dateInput" type="date" size="small" class="w-36" />
          </div>
          <n-button type="primary" :loading="generating" @click="generateReport">
            {{ generating ? '生成中...' : '生成复盘' }}
          </n-button>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-4 md:p-5">
        <n-spin v-if="loading" class="flex justify-center py-12" />

        <n-alert v-else-if="error" type="warning" :show-icon="true" closable @close="error = ''">
          {{ error }}
        </n-alert>

        <n-empty v-else-if="!report" description="暂无复盘报告">
          <template #extra>
            <p class="text-sm" style="color: var(--color-text-secondary)">选择日期或点击「生成复盘」</p>
          </template>
        </n-empty>

        <n-card v-else class="max-w-3xl mx-auto" size="large">
          <template #header>
            <div class="flex items-center justify-between">
              <div>
                <h3 class="font-display text-lg font-bold">{{ report.reportDate }} 复盘</h3>
                <p class="text-xs mt-1" style="color: var(--color-text-secondary)">
                  新增知识 {{ report.totalCount }} 条
                </p>
              </div>
              <n-tag size="small" :bordered="false">{{ report.reportDate }}</n-tag>
            </div>
          </template>

          <div class="text-sm leading-relaxed whitespace-pre-wrap mb-6" style="color: var(--color-warm-gray); line-height: 1.7">
            {{ report.summary }}
          </div>

          <n-card v-if="keyInsights.length" size="small" :bordered="false" class="mb-4 insight-card">
            <template #header><span class="text-xs font-medium" style="color: var(--color-text-secondary)">关键洞见</span></template>
            <ul class="space-y-1.5">
              <li v-for="(insight, idx) in keyInsights" :key="idx"
                class="text-sm pl-3" style="border-left: 2px solid var(--color-sage); color: var(--color-warm-gray)">
                {{ insight }}
              </li>
            </ul>
          </n-card>

          <n-card v-if="recommendations.length" size="small" :bordered="false" class="mb-4 rec-card">
            <template #header><span class="text-xs font-medium" style="color: var(--color-text-secondary)">后续建议</span></template>
            <ul class="space-y-1.5">
              <li v-for="(rec, idx) in recommendations" :key="idx"
                class="text-sm pl-3" style="border-left: 2px solid var(--color-accent); color: var(--color-warm-gray)">
                {{ rec }}
              </li>
            </ul>
          </n-card>

          <div v-if="categoryKeys.length" style="border-top: 1px solid var(--color-border)" class="pt-4">
            <span class="text-xs font-medium" style="color: var(--color-text-secondary)">知识分类统计</span>
            <n-space size="small" class="mt-2">
              <n-tag v-for="cat in categoryKeys" :key="cat" size="small" :bordered="false" class="cat-tag">
                {{ cat }}: {{ categoryBreakdown[cat] }}
              </n-tag>
            </n-space>
          </div>
        </n-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { dailyReviewApi } from '@/api/dailyReview'

const report = ref(null)
const loading = ref(true)
const generating = ref(false)
const error = ref('')
const recentReports = ref([])
const loadingRecent = ref(false)
const selectedDate = ref('')
const dateInput = ref(null)

const keyInsights = computed(() => {
  if (!report.value?.keyInsights) return []
  try { return JSON.parse(report.value.keyInsights) } catch { return [] }
})

const recommendations = computed(() => {
  if (!report.value?.recommendations) return []
  try { return JSON.parse(report.value.recommendations) } catch { return [] }
})

const categoryBreakdown = computed(() => {
  if (!report.value?.categoryBreakdown) return {}
  try { return JSON.parse(report.value.categoryBreakdown) } catch { return {} }
})

const categoryKeys = computed(() => Object.keys(categoryBreakdown.value))

async function loadRecent() {
  loadingRecent.value = true
  try {
    const res = await dailyReviewApi.getRecent(20)
    recentReports.value = res.data.data || []
  } catch {} finally {
    loadingRecent.value = false
  }
}

async function loadLatest() {
  loading.value = true
  error.value = ''
  try {
    const res = await dailyReviewApi.getLatest()
    report.value = res.data.data || null
    if (report.value) {
      selectedDate.value = report.value.reportDate
      dateInput.value = report.value.reportDate
    }
  } catch {
    error.value = '加载复盘报告失败'
  } finally {
    loading.value = false
  }
}

async function loadByDate(date) {
  if (!date) return
  selectedDate.value = date
  dateInput.value = date
  loading.value = true
  error.value = ''
  try {
    const res = await dailyReviewApi.getByDate(date)
    report.value = res.data.data || null
  } catch (err) {
    if (err.response?.status === 404) {
      report.value = null
      error.value = `该日期 (${date}) 暂无复盘报告`
    } else {
      error.value = '加载复盘报告失败'
    }
  } finally {
    loading.value = false
  }
}

async function generateReport() {
  generating.value = true
  error.value = ''
  try {
    const targetDate = dateInput.value || undefined
    const res = await dailyReviewApi.generate(targetDate)
    report.value = res.data.data
    selectedDate.value = report.value.reportDate
    dateInput.value = report.value.reportDate
    await loadRecent()
  } catch (err) {
    error.value = err.response?.data?.message || '生成复盘报告失败'
  } finally {
    generating.value = false
  }
}

watch(dateInput, (newDate) => {
  if (newDate && newDate !== selectedDate.value) {
    loadByDate(newDate)
  }
})

onMounted(async () => {
  await loadRecent()
  await loadLatest()
})
</script>

<style scoped>
.insight-card {
  --n-color: var(--color-sage-light);
}
.rec-card {
  --n-color: var(--color-bg);
}
.cat-tag {
  --n-color: var(--color-sage-light);
  --n-text-color: var(--color-sage);
}
</style>
