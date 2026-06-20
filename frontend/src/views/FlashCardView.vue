<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3 mb-3">
        <div class="flashcard-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">知识卡片</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">基于知识自动生成的问答式记忆卡片</p>
        </div>
      </div>
      <div class="flex flex-col sm:flex-row gap-2">
        <n-select v-model:value="selectedKnowledgeId" :options="knowledgeOptions" placeholder="选择知识生成卡片" clearable class="flex-1" />
        <n-button type="primary" :loading="generating" :disabled="!selectedKnowledgeId" @click="generateCards" class="generate-btn">
          {{ generating ? '生成中...' : '生成卡片' }}
        </n-button>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-alert v-if="error" type="error" :show-icon="true" closable @close="error = ''" class="mb-4">
        <template #action><n-button text type="warning" @click="loadCards">重试</n-button></template>{{ error }}
      </n-alert>

      <n-spin v-else-if="loading" class="flex justify-center py-12" />

      <n-empty v-else-if="!cards.length" description="还没有知识卡片">
        <template #extra><p class="text-sm" style="color: var(--color-text-secondary)">选择一条知识，点击「生成卡片」自动创建问答式记忆卡片</p></template>
      </n-empty>

      <div v-else class="max-w-2xl mx-auto space-y-6">
        <div v-for="(card, idx) in cards" :key="card.id" class="flashcard-container" @click="flipCard(idx)">
          <div class="flashcard-inner" :class="{ flipped: flipped[idx] }">
            <div class="flashcard-front">
              <div class="card-label">问题</div>
              <div class="card-content">{{ card.question }}</div>
              <div class="card-hint">点击翻转查看答案</div>
            </div>
            <div class="flashcard-back">
              <div class="card-label" style="color: var(--color-sage)">答案</div>
              <div class="card-content">{{ card.answer }}</div>
              <div class="card-hint">点击翻转回去</div>
            </div>
          </div>
          <div class="flashcard-footer">
            <span class="difficulty-badge" :class="'diff-' + (card.difficulty || 'MEDIUM').toLowerCase()">
              {{ diffLabel(card.difficulty) }}
            </span>
            <n-button text size="tiny" type="error" @click.stop="confirmDelete(card.id, idx)">删除</n-button>
          </div>
        </div>
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

const knowledgeOptions = computed(() => store.items.map(k => ({ label: k.title, value: k.id })))

function diffLabel(d) {
  if (d === 'EASY') return '简单'
  if (d === 'HARD') return '困难'
  return '中等'
}

function flipCard(idx) { flipped.value[idx] = !flipped.value[idx] }

async function loadCards() { loading.value = true; error.value = ''; try { const res = await flashcardApi.list(); cards.value = res.data.data || []; flipped.value = cards.value.map(() => false) } catch (e) { error.value = e.response?.data?.message || '加载失败' } finally { loading.value = false } }

async function generateCards() { if (!selectedKnowledgeId.value) return; generating.value = true; try { const res = await flashcardApi.generate(selectedKnowledgeId.value); cards.value = res.data.data || []; flipped.value = cards.value.map(() => false) } finally { generating.value = false } }

function confirmDelete(id, idx) { dialog.warning({ title: '删除卡片', content: '确定删除此卡片？', positiveText: '确定', negativeText: '取消', onPositiveClick: async () => { await flashcardApi.delete(id); cards.value.splice(idx, 1); flipped.value.splice(idx, 1) } }) }

onMounted(() => { store.loadItems(); loadCards() })
</script>

<style scoped>
.flashcard-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-sage) 0%, #4a6a47 100%);
}
.generate-btn { --n-color: var(--color-sage); --n-color-hover: #4a6a47; --n-color-pressed: #3d5a3a; }
.flashcard-container { perspective: 1200px; cursor: pointer; }
.flashcard-inner { position: relative; width: 100%; min-height: 200px; transition: transform 0.6s cubic-bezier(0.23, 1, 0.32, 1); transform-style: preserve-3d; }
.flashcard-inner.flipped { transform: rotateY(180deg); }
.flashcard-front, .flashcard-back { position: absolute; inset: 0; backface-visibility: hidden; border-radius: 14px; padding: 28px 24px; display: flex; flex-direction: column; background-color: var(--color-surface); border: 1px solid var(--color-border); box-shadow: 0 4px 20px rgba(45, 42, 36, 0.06); }
.flashcard-back { transform: rotateY(180deg); border-color: var(--color-sage-light); }
.card-label { font-size: 0.7rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-accent); margin-bottom: 12px; }
.card-content { flex: 1; font-size: 1rem; line-height: 1.7; white-space: pre-wrap; }
.card-hint { font-size: 0.75rem; color: var(--color-text-secondary); text-align: center; margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--color-border); }
.flashcard-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; padding: 0 4px; }
.difficulty-badge { font-size: 0.7rem; padding: 2px 10px; border-radius: 999px; font-weight: 500; }
.diff-easy { background-color: var(--color-sage-light); color: var(--color-sage); }
.diff-hard { background-color: #fef2f2; color: #dc2626; }
.diff-medium { background-color: #fffbeb; color: #d97706; }
</style>