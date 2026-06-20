<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <h2 class="font-display text-lg md:text-xl">知识卡片</h2>
        <n-space size="small" class="w-full sm:w-auto">
          <n-select v-model:value="selectedKnowledgeId" :options="knowledgeOptions" placeholder="选择知识生成卡片" clearable class="min-w-48" />
          <n-button type="primary" :loading="generating" :disabled="!selectedKnowledgeId" @click="generateCards">
            {{ generating ? '生成中...' : '生成卡片' }}
          </n-button>
        </n-space>
      </div>
      <p v-if="selectedKnowledgeId" class="text-xs mt-2" style="color: var(--color-text-secondary)">
        基于当前知识自动生成 3-5 个问答式记忆卡片
      </p>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-alert v-if="error" type="error" :show-icon="true" closable @close="error = ''" class="mb-4">
        <template #action>
          <n-button text type="warning" @click="loadCards">重试</n-button>
        </template>
        {{ error }}
      </n-alert>

      <n-spin v-else-if="loading" class="flex justify-center py-12" />

      <n-empty v-else-if="!cards.length" description="还没有知识卡片">
        <template #extra>
          <p class="text-sm" style="color: var(--color-text-secondary)">选择一条知识，点击「生成卡片」自动创建问答式记忆卡片</p>
        </template>
      </n-empty>

      <div v-else class="max-w-2xl mx-auto space-y-4">
        <n-card v-for="(card, idx) in cards" :key="card.id" hoverable class="flashcard" @click="flipCard(idx)">
          <div class="flex items-start justify-between mb-3">
            <n-tag size="tiny" :bordered="false" :type="diffType(card.difficulty)">
              {{ diffLabel(card.difficulty) }}
            </n-tag>
            <n-button text size="tiny" type="error" @click.stop="confirmDelete(card.id, idx)">删除</n-button>
          </div>
          <div class="min-h-[120px] flex flex-col justify-center">
            <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">{{ flipped[idx] ? '答案' : '问题' }}</p>
            <p class="text-base leading-relaxed whitespace-pre-wrap" style="line-height: 1.7">
              {{ flipped[idx] ? card.answer : card.question }}
            </p>
          </div>
          <div class="mt-4 pt-3 text-center" style="border-top: 1px solid var(--color-border)">
            <span class="text-xs" style="color: var(--color-text-secondary)">点击翻转卡片</span>
          </div>
        </n-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDialog } from 'naive-ui'
import { useKnowledgeStore } from '@/stores/knowledge'
import { flashcardApi } from '@/api/flashcard'

const dialog = useDialog()
const store = useKnowledgeStore()
const cards = ref([])
const loading = ref(false)
const generating = ref(false)
const selectedKnowledgeId = ref(null)
const flipped = ref([])
const error = ref('')

const knowledgeOptions = computed(() =>
  store.items.map(k => ({ label: k.title, value: k.id }))
)

function diffType(d) {
  if (d === 'EASY') return 'success'
  if (d === 'HARD') return 'error'
  return 'warning'
}

function diffLabel(d) {
  if (d === 'EASY') return '简单'
  if (d === 'HARD') return '困难'
  return '中等'
}

function flipCard(idx) {
  flipped.value[idx] = !flipped.value[idx]
}

async function loadCards() {
  loading.value = true
  error.value = ''
  try {
    const res = await flashcardApi.list()
    cards.value = res.data.data || []
    flipped.value = cards.value.map(() => false)
  } catch (e) {
    error.value = e.response?.data?.message || '加载失败'
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

function confirmDelete(id, idx) {
  dialog.warning({
    title: '删除卡片',
    content: '确定删除此卡片？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await flashcardApi.delete(id)
      cards.value.splice(idx, 1)
      flipped.value.splice(idx, 1)
    },
  })
}

onMounted(() => {
  store.loadItems()
  loadCards()
})
</script>

<style scoped>
.flashcard {
  cursor: pointer;
}
</style>
