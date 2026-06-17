<template>
  <div class="flex flex-col h-full">
    <div class="p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <h2 class="font-display text-xl">间隔复习</h2>
      <p class="text-sm mt-1" style="color: var(--color-text-secondary)">
        今日待复习: <span style="color: var(--color-accent); font-weight: 600">{{ dueCount }}</span> 条
      </p>
    </div>

    <div class="flex-1 overflow-y-auto p-5">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin"
          style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="!currentReview && !dueItems.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-10 h-10 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">今日无待复习内容</p>
        <p class="text-sm mt-1">添加知识后系统会自动安排复习计划</p>
      </div>

      <div v-else-if="!currentReview && dueItems.length" class="flex flex-col items-center py-12">
        <p class="text-sm mb-4" style="color: var(--color-text-secondary)">
          共 {{ dueItems.length }} 条知识待复习
        </p>
        <button @click="startReview" class="btn-primary">
          开始复习
        </button>
      </div>

      <div v-else class="max-w-2xl mx-auto">
        <div class="card p-6 fade-in-enter">
          <div class="flex items-start justify-between mb-3">
            <h3 class="font-display text-lg font-bold" style="color: var(--color-text)">
              {{ currentReview.title }}
            </h3>
            <span class="tag-pill text-xs shrink-0 ml-3">
              复习 #{{ reviewIndex + 1 }}
            </span>
          </div>
          <div class="flex flex-wrap gap-1.5 mb-4" v-if="currentReviewTags.length">
            <span v-for="tag in currentReviewTags" :key="tag" class="tag-pill">#{{ tag }}</span>
          </div>

          <div class="text-sm leading-relaxed whitespace-pre-wrap mb-6"
            style="color: var(--color-warm-gray); line-height: 1.7">
            {{ currentReview.content }}
          </div>

          <div class="mb-6 p-4 rounded-lg" v-if="currentReview.summary"
            :style="{ backgroundColor: 'var(--color-sage-light)' }">
            <p class="text-xs font-medium mb-1" style="color: var(--color-sage)">摘要</p>
            <p class="text-sm" style="color: var(--color-warm-gray)">{{ currentReview.summary }}</p>
          </div>

          <p class="text-xs mb-4" style="color: var(--color-text-secondary)">
            上轮间隔: {{ currentReview.intervalDays }} 天
            · 已复习 {{ currentReview.reviewCount }} 次
            · 难度系数: {{ currentReview.easeFactor }}
          </p>

          <p class="text-sm font-medium mb-3" style="color: var(--color-text)">这次记住了吗？</p>
          <div class="flex flex-wrap gap-2">
            <button v-for="opt in qualityOptions" :key="opt.value"
              @click="submitReview(opt.value)"
              class="px-4 py-2 rounded-lg text-sm transition-all duration-150"
              :style="{
                backgroundColor: opt.bg,
                color: opt.color
              }"
              @mouseenter="$event.target.style.transform = 'scale(1.05)'"
              @mouseleave="$event.target.style.transform = 'scale(1)'">
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div class="flex items-center justify-between mt-4 text-xs" style="color: var(--color-text-secondary)">
          <span>第 {{ reviewIndex + 1 }} / {{ dueItems.length }} 条</span>
          <button v-if="reviewIndex > 0" @click="prevReview" class="btn-secondary text-xs px-3 py-1">上一条</button>
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

const currentReview = computed(() => dueItems.value[reviewIndex.value] || null)

const currentReviewTags = computed(() => {
  if (!currentReview.value?.tags) return []
  try { return JSON.parse(currentReview.value.tags) } catch { return [] }
})

const qualityOptions = [
  { value: 0, label: '完全忘了', bg: '#f0eeeb', color: 'var(--color-text-secondary)' },
  { value: 1, label: '很模糊', bg: '#f0eeeb', color: 'var(--color-text-secondary)' },
  { value: 2, label: '有点印象', bg: '#f0eeeb', color: 'var(--color-text-secondary)' },
  { value: 3, label: '勉强记住', bg: 'var(--color-sage-light)', color: 'var(--color-sage)' },
  { value: 4, label: '大部分记住', bg: 'var(--color-sage-light)', color: 'var(--color-sage)' },
  { value: 5, label: '完全记住', bg: 'var(--color-sage-light)', color: 'var(--color-sage)' },
]

async function loadDue() {
  loading.value = true
  try {
    const [dueRes, countRes] = await Promise.all([
      reviewApi.getDue(20),
      reviewApi.getDueCount()
    ])
    dueItems.value = dueRes.data.data || []
    dueCount.value = countRes.data.data?.count || 0
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