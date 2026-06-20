<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <h2 class="font-display text-lg md:text-xl">间隔复习</h2>
      <p class="text-sm mt-1" style="color: var(--color-text-secondary)">
        今日待复习: <span style="color: var(--color-accent); font-weight: 600">{{ dueCount }}</span> 条
      </p>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-alert v-if="error" type="error" :show-icon="true" closable @close="error = ''" class="mb-4">
        <template #action>
          <n-button text type="warning" @click="loadDue">重试</n-button>
        </template>
        {{ error }}
      </n-alert>

      <n-spin v-else-if="loading" class="flex justify-center py-12" />

      <n-empty v-else-if="!currentReview && !dueItems.length" description="今日无待复习内容">
        <template #extra>
          <p class="text-xs" style="color: var(--color-text-secondary)">添加知识后系统会自动安排复习计划</p>
        </template>
      </n-empty>

      <div v-else-if="!currentReview && dueItems.length" class="flex flex-col items-center py-12">
        <p class="text-sm mb-4" style="color: var(--color-text-secondary)">
          共 {{ dueItems.length }} 条知识待复习
        </p>
        <n-button type="primary" size="large" @click="startReview">开始复习</n-button>
      </div>

      <div v-else class="max-w-2xl mx-auto">
        <n-card size="large" :bordered="true" class="review-card">
          <template #header>
            <div class="flex items-start justify-between">
              <h3 class="font-display text-lg font-bold">{{ currentReview.title }}</h3>
              <n-tag size="small" :bordered="false">复习 #{{ reviewIndex + 1 }}</n-tag>
            </div>
          </template>

          <n-space v-if="currentReviewTags.length" size="small" class="mb-4">
            <n-tag v-for="tag in currentReviewTags" :key="tag" size="tiny" :bordered="false" class="tag-sage">#{{ tag }}</n-tag>
          </n-space>

          <div class="text-sm leading-relaxed whitespace-pre-wrap mb-6" style="color: var(--color-warm-gray); line-height: 1.7">
            {{ currentReview.content }}
          </div>

          <n-card v-if="currentReview.summary" size="small" :bordered="false" class="mb-6 summary-card">
            <template #header><span class="text-xs font-medium">摘要</span></template>
            <p class="text-sm" style="color: var(--color-warm-gray)">{{ currentReview.summary }}</p>
          </n-card>

          <p class="text-xs mb-4" style="color: var(--color-text-secondary)">
            上轮间隔: {{ currentReview.intervalDays }} 天 · 已复习 {{ currentReview.reviewCount }} 次 · 难度系数: {{ currentReview.easeFactor }}
          </p>

          <p class="text-sm font-medium mb-3">这次记住了吗？</p>
          <n-space>
            <n-button v-for="opt in qualityOptions" :key="opt.value" size="small" @click="submitReview(opt.value)">
              {{ opt.label }}
            </n-button>
          </n-space>
        </n-card>

        <div class="flex items-center justify-between mt-4 text-xs" style="color: var(--color-text-secondary)">
          <span>第 {{ reviewIndex + 1 }} / {{ dueItems.length }} 条</span>
          <n-button v-if="reviewIndex > 0" text size="tiny" @click="prevReview">上一条</n-button>
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

const currentReviewTags = computed(() => {
  if (!currentReview.value?.tags) return []
  try { return JSON.parse(currentReview.value.tags) } catch { return [] }
})

const qualityOptions = [
  { value: 0, label: '完全忘了' },
  { value: 1, label: '很模糊' },
  { value: 2, label: '有点印象' },
  { value: 3, label: '勉强记住' },
  { value: 4, label: '大部分记住' },
  { value: 5, label: '完全记住' }
]

async function loadDue() {
  loading.value = true
  error.value = ''
  try {
    const [dueRes, countRes] = await Promise.all([
      reviewApi.getDue(20),
      reviewApi.getDueCount()
    ])
    dueItems.value = dueRes.data.data || []
    dueCount.value = countRes.data.data?.count || 0
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function startReview() {
  reviewIndex.value = 0
}

async function submitReview(quality) {
  const item = currentReview.value
  if (!item) return
  await reviewApi.perform(item.knowledgeId, quality)
  dueItems.value.splice(reviewIndex.value, 1)
  dueCount.value = Math.max(0, dueCount.value - 1)
  if (reviewIndex.value >= dueItems.value.length) {
    reviewIndex.value = Math.max(0, dueItems.value.length - 1)
  }
}

function prevReview() {
  if (reviewIndex.value > 0) reviewIndex.value--
}

onMounted(loadDue)
</script>

<style scoped>
.review-card {
  --n-padding-bottom: 20px;
}
.summary-card {
  --n-color: var(--color-sage-light);
}
.tag-sage {
  --n-color: var(--color-sage-light);
  --n-text-color: var(--color-sage);
}
</style>
