<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3">
        <div class="review-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">间隔复习</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">基于 SM-2 算法智能安排复习计划</p>
        </div>
      </div>
      <div class="mt-3 flex items-center gap-2">
        <div class="flex-1 h-1.5 rounded-full" style="background-color: var(--color-border)">
          <div class="h-full rounded-full transition-all duration-500" :style="{ width: reviewProgress + '%', backgroundColor: reviewProgress === 100 ? 'var(--color-sage)' : 'var(--color-accent)' }"></div>
        </div>
        <span class="text-xs font-medium shrink-0" style="color: var(--color-sage)">{{ dueCount }} 条待复习</span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-alert v-if="error" type="error" :show-icon="true" closable @close="error = ''" class="mb-4">
        <template #action><n-button text type="warning" @click="loadDue">重试</n-button></template>{{ error }}
      </n-alert>

      <n-spin v-else-if="loading" class="flex justify-center py-12" />

      <n-empty v-else-if="!currentReview && !dueItems.length" description="今日无待复习内容">
        <template #extra><p class="text-xs" style="color: var(--color-text-secondary)">添加知识后系统会自动安排复习计划</p></template>
      </n-empty>

      <div v-else-if="!currentReview && dueItems.length" class="flex flex-col items-center justify-center py-16">
        <div class="review-start-icon">
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
        </div>
        <p class="text-lg font-display font-medium mt-4" style="color: var(--color-text)">准备好了吗？</p>
        <p class="text-sm mt-1" style="color: var(--color-text-secondary)">共 {{ dueItems.length }} 条知识待复习</p>
        <n-button type="primary" size="large" class="mt-6 review-start-btn" @click="startReview">开始复习</n-button>
      </div>

      <div v-else class="max-w-2xl mx-auto">
        <div class="review-progress-strip">
          <div class="progress-dot" v-for="(_, i) in dueItems.length" :key="i"
            :class="{ active: i === reviewIndex, done: i < reviewIndex }"></div>
        </div>

        <div class="review-card-wrapper">
          <div class="review-card-header">
            <div class="flex items-start justify-between">
              <h3 class="font-display text-lg font-bold">{{ currentReview.title }}</h3>
              <span class="text-xs px-2 py-0.5 rounded-full" style="background-color: var(--color-sage-light); color: var(--color-sage)">{{ reviewIndex + 1 }} / {{ dueItems.length }}</span>
            </div>
            <div class="flex flex-wrap gap-1 mt-2" v-if="currentReviewTags.length">
              <n-tag v-for="tag in currentReviewTags" :key="tag" size="tiny" type="primary" :bordered="false">#{{ tag }}</n-tag>
            </div>
          </div>

          <div class="review-card-body">
            <div class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--color-warm-gray); line-height: 1.7">{{ currentReview.content }}</div>
          </div>

          <div v-if="currentReview.summary" class="review-summary">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
            <span>{{ currentReview.summary }}</span>
          </div>

          <div class="review-card-stats">
            <span>上轮间隔: {{ currentReview.intervalDays }} 天</span>
            <span>已复习 {{ currentReview.reviewCount }} 次</span>
            <span>难度: {{ currentReview.easeFactor }}</span>
          </div>

          <div class="review-card-actions">
            <p class="text-sm font-medium mb-3">这次记住了吗？</p>
            <div class="quality-grid">
              <n-button v-for="opt in qualityOptions" :key="opt.value" @click="submitReview(opt.value)"
                :class="['quality-btn', 'q-' + opt.value]">{{ opt.label }}</n-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { reviewApi } from '@/api/review'

const loading = ref(true)
const dueItems = ref([])
const dueCount = ref(0)
const reviewIndex = ref(0)
const error = ref('')

const currentReview = computed(() => dueItems.value[reviewIndex.value] || null)
const reviewProgress = computed(() => dueItems.value.length ? ((reviewIndex.value) / dueItems.value.length * 100) : 0)

const currentReviewTags = computed(() => {
  if (!currentReview.value?.tags) return []
  try { return JSON.parse(currentReview.value.tags) } catch { return [] }
})

const qualityOptions = [
  { value: 0, label: '完全忘了' }, { value: 1, label: '很模糊' }, { value: 2, label: '有点印象' },
  { value: 3, label: '勉强记住' }, { value: 4, label: '大部分记住' }, { value: 5, label: '完全记住' }
]

async function loadDue() {
  loading.value = true; error.value = ''
  try { const [dueRes, countRes] = await Promise.all([reviewApi.getDue(20), reviewApi.getDueCount()]); dueItems.value = dueRes.data.data || []; dueCount.value = countRes.data.data?.count || 0 } catch (e) { error.value = e.response?.data?.message || '加载失败' } finally { loading.value = false }
}

function startReview() { reviewIndex.value = 0 }

async function submitReview(quality) {
  const item = currentReview.value; if (!item) return
  await reviewApi.perform(item.knowledgeId, quality)
  dueItems.value.splice(reviewIndex.value, 1); dueCount.value = Math.max(0, dueCount.value - 1)
  if (reviewIndex.value >= dueItems.value.length) reviewIndex.value = Math.max(0, dueItems.value.length - 1)
}

onMounted(loadDue)
</script>

<style scoped>
.review-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
}
.review-start-icon {
  width: 72px; height: 72px; border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-sage); background-color: var(--color-sage-light);
}
.review-start-btn {
  --n-color: rgba(139,92,246,0.2); --n-color-hover: rgba(139,92,246,0.3); --n-color-pressed: rgba(139,92,246,0.35);
}
.review-progress-strip {
  display: flex; align-items: center; gap: 6px; margin-bottom: 20px; justify-content: center;
}
.progress-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background-color: var(--color-border); transition: all 0.3s ease;
}
.progress-dot.active { width: 10px; height: 10px; background-color: var(--color-accent); box-shadow: 0 0 0 3px rgba(198, 95, 57, 0.15); }
.progress-dot.done { background-color: var(--color-sage); }
.review-card-wrapper {
  border-radius: 16px; overflow: hidden;
  background-color: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: 0 2px 12px rgba(45, 42, 36, 0.05);
}
.review-card-header { padding: 20px 20px 0; }
.review-card-body { padding: 16px 20px; }
.review-summary { margin: 0 20px 16px; padding: 12px; border-radius: 8px; display: flex; gap: 8px; align-items: flex-start; font-size: 0.8rem; color: var(--color-warm-gray); background-color: var(--color-sage-light); }
.review-card-stats { margin: 0 20px 16px; display: flex; gap: 12px; flex-wrap: wrap; font-size: 0.75rem; color: var(--color-text-secondary); }
.review-card-actions { padding: 16px 20px; border-top: 1px solid var(--color-border); }
.quality-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
.quality-btn { font-size: 0.75rem !important; }
.q-0, .q-1 { --n-color: #fef2f2; --n-color-hover: #fee2e2; color: #dc2626; }
.q-2, .q-3 { --n-color: rgba(139,92,246,0.1); --n-color-hover: rgba(139,92,246,0.15); color: var(--color-sage); }
.q-4, .q-5 { --n-color: rgba(139,92,246,0.2); --n-color-hover: rgba(139,92,246,0.3); color: white; }
</style>