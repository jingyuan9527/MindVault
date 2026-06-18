<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between">
        <h2 class="font-display text-lg md:text-xl">每日复盘</h2>
        <button @click="generateReport" :disabled="generating"
          class="btn-primary text-sm">
          {{ generating ? '生成中...' : '生成今日复盘' }}
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

      <div v-else-if="!report" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-10 h-10 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">暂无复盘报告</p>
        <p class="text-sm mt-1">点击「生成今日复盘」查看今日新增知识的总结</p>
      </div>

      <div v-else class="max-w-3xl mx-auto space-y-6">
        <div class="card p-6 fade-in-enter">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="font-display text-lg font-bold" style="color: var(--color-text)">{{ report.reportDate }} 复盘</h3>
              <p class="text-xs mt-1" style="color: var(--color-text-secondary)">
                新增知识 {{ report.totalCount }} 条
              </p>
            </div>
            <span class="tag-pill text-xs">{{ report.reportDate }}</span>
          </div>

          <div class="text-sm leading-relaxed whitespace-pre-wrap mb-6"
            style="color: var(--color-warm-gray); line-height: 1.7">
            {{ report.summary }}
          </div>

          <div v-if="keyInsights.length" class="mb-5">
            <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">关键洞见</p>
            <ul class="space-y-1.5">
              <li v-for="(insight, idx) in keyInsights" :key="idx"
                class="text-sm pl-3" style="border-left: 2px solid var(--color-sage); color: var(--color-warm-gray)">
                {{ insight }}
              </li>
            </ul>
          </div>

          <div v-if="recommendations.length" class="mb-5">
            <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">后续建议</p>
            <ul class="space-y-1.5">
              <li v-for="(rec, idx) in recommendations" :key="idx"
                class="text-sm pl-3" style="border-left: 2px solid var(--color-accent); color: var(--color-warm-gray)">
                {{ rec }}
              </li>
            </ul>
          </div>

          <div v-if="categoryKeys.length" style="border-top: 1px solid var(--color-border); padding-top: 1rem">
            <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">知识分类统计</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="cat in categoryKeys" :key="cat"
                class="px-2.5 py-1 text-xs rounded-full"
                :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">
                {{ cat }}: {{ categoryBreakdown[cat] }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { dailyReviewApi } from '@/api/dailyReview'

const report = ref(null)
const loading = ref(true)
const generating = ref(false)
const error = ref('')

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

async function loadLatest() {
  loading.value = true
  error.value = ''
  try {
    const res = await dailyReviewApi.getLatest()
    report.value = res.data.data || null
  } catch {
    error.value = '加载复盘报告失败'
  } finally {
    loading.value = false
  }
}

async function generateReport() {
  generating.value = true
  error.value = ''
  try {
    const res = await dailyReviewApi.generate()
    report.value = res.data.data
  } catch (err) {
    error.value = err.response?.data?.message || '生成复盘报告失败'
  } finally {
    generating.value = false
  }
}

onMounted(loadLatest)
</script>