<template>
  <div class="flex h-full">
    <div class="w-60 shrink-0 hidden md:flex flex-col" style="border-right: 1px solid var(--color-border)">
      <div class="p-3 shrink-0" style="border-bottom: 1px solid var(--color-border)">
        <p class="text-xs font-medium" style="color: var(--color-text-secondary)">历史回顾</p>
      </div>
      <div class="flex-1 overflow-y-auto p-2 space-y-1">
        <div
v-for="r in recentReports" :key="r.id"
          class="history-item" :class="{ active: selectedDate === r.reportDate }"
          tabindex="0" role="button" @click="loadByDate(r.reportDate)" @keydown.enter="loadByDate(r.reportDate)">
          <p class="text-xs font-medium">{{ r.reportDate }}</p>
          <p class="text-xs mt-0.5 opacity-70">{{ r.totalCount || 0 }} 条知识</p>
        </div>
        <p v-if="!recentReports.length && !loadingRecent" class="text-xs px-3 py-4 text-center" style="color: var(--color-text-secondary)">暂无历史记录</p>
      </div>
    </div>

    <div class="flex-1 flex flex-col min-w-0">
      <div class="shrink-0 px-4 md:px-5 py-3" style="border-bottom: 1px solid var(--color-border)">
        <div class="flex items-center gap-3 mb-3">
          <div class="review-daily-icon">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
            </svg>
          </div>
          <div>
            <h2 class="font-display text-lg md:text-xl">每日复盘</h2>
            <p class="text-xs" style="color: var(--color-text-secondary)">AI 自动生成的知识回顾报告</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <n-date-picker v-model:formatted-value="dateInput" type="date" size="small" class="w-36" />
          <n-button type="primary" :loading="generating" class="generate-daily-btn" @click="generateReport">
            {{ generating ? '生成中...' : '生成复盘' }}
          </n-button>
        </div>
      </div>

      <div class="flex-1 overflow-y-auto p-4 md:p-5">
        <n-spin v-if="loading" class="flex justify-center py-12" />
        <n-alert v-else-if="error" type="warning" :show-icon="true" closable @close="error = ''">{{ error }}</n-alert>

        <n-empty v-else-if="!report" description="暂无复盘报告" class="py-16">
          <template #extra><p class="text-sm" style="color: var(--color-text-secondary)">选择日期或点击「生成复盘」</p></template>
        </n-empty>

        <div v-else class="max-w-3xl mx-auto space-y-4">
          <div class="report-header">
            <div>
              <h3 class="font-display text-xl font-bold">{{ report.reportDate }} 复盘</h3>
              <p class="report-count">新增知识 {{ report.totalCount }} 条</p>
            </div>
            <span class="report-date-badge">{{ report.reportDate }}</span>
          </div>

          <div class="report-section">
            <div class="report-section-title">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
              总结
            </div>
            <div class="report-summary">{{ report.summary }}</div>
          </div>

          <div v-if="keyInsights.length" class="report-section insight">
            <div class="report-section-title">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
              关键洞见
            </div>
            <ul class="report-list">
              <li v-for="(insight, idx) in keyInsights" :key="idx" class="report-list-item insight-item">{{ insight }}</li>
            </ul>
          </div>

          <div v-if="recommendations.length" class="report-section rec">
            <div class="report-section-title">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></svg>
              后续建议
            </div>
            <ul class="report-list">
              <li v-for="(rec, idx) in recommendations" :key="idx" class="report-list-item rec-item">{{ rec }}</li>
            </ul>
          </div>

          <div v-if="categoryKeys.length" class="report-section">
            <div class="report-section-title">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/></svg>
              知识分类统计
            </div>
            <div class="flex flex-wrap gap-2 mt-2">
              <span v-for="cat in categoryKeys" :key="cat" class="category-tag">{{ cat }}: {{ categoryBreakdown[cat] }}</span>
            </div>
          </div>
        </div>
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

const keyInsights = computed(() => { if (!report.value?.keyInsights) return []; try { return JSON.parse(report.value.keyInsights) } catch { return [] } })
const recommendations = computed(() => { if (!report.value?.recommendations) return []; try { return JSON.parse(report.value.recommendations) } catch { return [] } })
const categoryBreakdown = computed(() => { if (!report.value?.categoryBreakdown) return {}; try { return JSON.parse(report.value.categoryBreakdown) } catch { return {} } })
const categoryKeys = computed(() => Object.keys(categoryBreakdown.value))

async function loadRecent() { loadingRecent.value = true; try { const res = await dailyReviewApi.getRecent(20); recentReports.value = res.data.data || [] } catch {} finally { loadingRecent.value = false } }

async function loadLatest() { loading.value = true; error.value = ''; try { const res = await dailyReviewApi.getLatest(); report.value = res.data.data || null; if (report.value) { selectedDate.value = report.value.reportDate; dateInput.value = report.value.reportDate } } catch { error.value = '加载复盘报告失败' } finally { loading.value = false } }

async function loadByDate(date) { if (!date) return; selectedDate.value = date; dateInput.value = date; loading.value = true; error.value = ''; try { const res = await dailyReviewApi.getByDate(date); report.value = res.data.data || null } catch (err) { if (err.response?.status === 404) { report.value = null; error.value = `该日期 (${date}) 暂无复盘报告` } else { error.value = '加载复盘报告失败' } } finally { loading.value = false } }

async function generateReport() { generating.value = true; error.value = ''; try { const targetDate = dateInput.value || undefined; const res = await dailyReviewApi.generate(targetDate); report.value = res.data.data; selectedDate.value = report.value.reportDate; dateInput.value = report.value.reportDate; await loadRecent() } catch (err) { error.value = err.response?.data?.message || '生成复盘报告失败' } finally { generating.value = false } }

watch(dateInput, (newDate) => { if (newDate && newDate !== selectedDate.value) loadByDate(newDate) })

onMounted(async () => { await loadRecent(); await loadLatest() })
</script>

<style scoped>
.review-daily-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}
.generate-daily-btn { --n-color: #ca8a04; --n-color-hover: #a16207; --n-color-pressed: #854d0e; }
.history-item { padding: 8px 12px; border-radius: 8px; cursor: pointer; transition: all 0.15s; color: var(--color-text-secondary); }
.history-item:hover { background-color: var(--color-sage-light); }
.history-item.active { background-color: var(--color-sage-light); color: var(--color-sage); font-weight: 500; }
.report-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid var(--color-border); }
.report-count { font-size: 0.8rem; color: var(--color-text-secondary); margin-top: 2px; }
.report-date-badge { font-size: 0.75rem; padding: 4px 12px; border-radius: 999px; background-color: var(--color-sage-light); color: var(--color-sage); white-space: nowrap; }
.report-section { padding: 16px; border-radius: 12px; background-color: var(--color-surface); border: 1px solid var(--color-border); }
.report-section.insight { border-left: 3px solid var(--color-sage); }
.report-section.rec { border-left: 3px solid var(--color-accent); }
.report-section-title { display: flex; align-items: center; gap: 6px; font-size: 0.8rem; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 10px; }
.report-summary { font-size: 0.9rem; line-height: 1.7; color: var(--color-warm-gray); white-space: pre-wrap; }
.report-list { display: flex; flex-direction: column; gap: 6px; }
.report-list-item { font-size: 0.85rem; line-height: 1.6; padding-left: 12px; border-left: 2px solid var(--color-border); color: var(--color-warm-gray); }
.insight-item { border-left-color: var(--color-sage); }
.rec-item { border-left-color: var(--color-accent); }
.category-tag { font-size: 0.75rem; padding: 3px 10px; border-radius: 999px; background-color: var(--color-sage-light); color: var(--color-sage); }
</style>