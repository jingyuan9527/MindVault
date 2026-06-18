<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <h2 class="font-display text-lg md:text-xl">知识卡片</h2>
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <select v-model="selectedKnowledgeId" class="input-field text-sm py-1.5 w-full sm:w-64">
            <option value="">选择知识生成卡片</option>
            <option v-for="k in store.items" :key="k.id" :value="k.id">{{ k.title }}</option>
          </select>
          <button @click="generateCards" :disabled="!selectedKnowledgeId || generating"
            class="btn-primary text-sm">
            {{ generating ? '生成中...' : '生成卡片' }}
          </button>
        </div>
      </div>
      <p v-if="selectedKnowledgeId" class="text-xs mt-2" style="color: var(--color-text-secondary)">
        基于当前知识自动生成 3-5 个问答式记忆卡片
      </p>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <div v-if="loading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin" style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="!cards.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-10 h-10 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">还没有知识卡片</p>
        <p class="text-sm mt-1">选择一条知识，点击「生成卡片」自动创建问答式记忆卡片</p>
      </div>

      <div v-else class="max-w-2xl mx-auto space-y-4">
        <div v-for="(card, idx) in cards" :key="card.id" class="relative">
          <div class="card cursor-pointer select-none" @click="flipCard(idx)">
            <div class="flex items-start justify-between mb-3">
              <span class="text-xs px-2 py-0.5 rounded" :style="diffStyle(card.difficulty)">
                {{ diffLabel(card.difficulty) }}
              </span>
              <button @click.stop="deleteCard(card.id, idx)"
                class="text-xs transition-colors duration-150"
                style="color: var(--color-text-secondary)"
                @mouseenter="$event.target.style.color = 'var(--color-accent)'"
                @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">删除</button>
            </div>
            <div class="min-h-[120px] flex flex-col justify-center">
              <p class="text-sm font-medium mb-2" style="color: var(--color-text-secondary)">{{ flipped[idx] ? '答案' : '问题' }}</p>
              <p class="text-base leading-relaxed whitespace-pre-wrap" style="color: var(--color-text); line-height: 1.7">
                {{ flipped[idx] ? card.answer : card.question }}
              </p>
            </div>
            <div class="mt-4 pt-3 text-center" style="border-top: 1px solid var(--color-border); color: var(--color-text-secondary)">
              <span class="text-xs">点击翻转卡片</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import { flashcardApi } from '@/api/flashcard'

const store = useKnowledgeStore()
const cards = ref([])
const loading = ref(false)
const generating = ref(false)
const selectedKnowledgeId = ref('')
const flipped = ref([])

const diffStyle = (d) => {
  if (d === 'EASY') return { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }
  if (d === 'HARD') return { backgroundColor: '#f0eeeb', color: 'var(--color-accent)' }
  return { backgroundColor: '#f0eeeb', color: 'var(--color-warm-gray)' }
}

const diffLabel = (d) => {
  if (d === 'EASY') return '简单'
  if (d === 'HARD') return '困难'
  return '中等'
}

function flipCard(idx) {
  flipped.value[idx] = !flipped.value[idx]
}

async function loadCards() {
  loading.value = true
  try {
    const res = await flashcardApi.list()
    cards.value = res.data.data || []
    flipped.value = cards.value.map(() => false)
  } finally {
    loading.value = false
  }
}

async function generateCards() {
  if (!selectedKnowledgeId.value) return
  generating.value = true
  try {
    const res = await flashcardApi.generate(selectedKnowledgeId.value)
    cards.value = res.data.data || []
    flipped.value = cards.value.map(() => false)
  } finally {
    generating.value = false
  }
}

async function deleteCard(id, idx) {
  if (!confirm('确定删除此卡片？')) return
  await flashcardApi.delete(id)
  cards.value.splice(idx, 1)
  flipped.value.splice(idx, 1)
}

onMounted(() => {
  store.loadItems()
  loadCards()
})
</script>