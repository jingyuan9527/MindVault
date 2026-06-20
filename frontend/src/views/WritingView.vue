<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 px-4 md:px-5 py-3" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3 mb-3">
        <div class="writing-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">写作辅助</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">让 AI 基于你的知识库生成高质量文章</p>
        </div>
      </div>
    </div>

    <div class="shrink-0 px-4 md:px-5 py-4 writing-form-area">
      <n-form label-placement="top" :model="formData">
        <div class="flex flex-col sm:flex-row gap-3 items-start sm:items-end">
          <n-form-item label="写作主题" class="flex-1 w-full sm:w-auto" style="margin-bottom: 0">
            <n-input v-model:value="formData.topic" placeholder="输入文章主题，如「Spring Boot 最佳实践」" size="large" />
          </n-form-item>
          <n-button type="primary" :loading="generating" :disabled="!formData.topic.trim()" @click="generate" class="generate-writing-btn shrink-0">
            <template #icon>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>
            </template>
            {{ generating ? '生成中...' : '生成文章' }}
          </n-button>
        </div>
        <div class="flex flex-col sm:flex-row gap-3 mt-3">
          <n-form-item label="写作风格（可选）" class="flex-1" style="margin-bottom: 0">
            <n-select v-model:value="formData.style" :options="styleOptions" placeholder="默认" clearable />
          </n-form-item>
          <n-form-item label="关键词（可选）" class="flex-1" style="margin-bottom: 0">
            <n-input v-model:value="formData.keywords" placeholder="逗号分隔" />
          </n-form-item>
        </div>
      </n-form>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="generating" class="flex justify-center py-16">
        <template #description>
          <div class="text-sm mt-2" style="color: var(--color-sage)">AI 正在创作中...</div>
        </template>
      </n-spin>

      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">{{ error }}</n-alert>

      <div v-else-if="article" class="max-w-3xl mx-auto">
        <div class="article-paper">
          <div class="article-meta">
            <h3 class="article-title">{{ formData.topic }}</h3>
            <n-button text size="tiny" type="primary" @click="copyArticle">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3"/></svg>
              {{ copied ? '已复制' : '复制全文' }}
            </n-button>
          </div>
          <div class="article-body">
            <div v-for="(paragraph, i) in article.split('\n\n')" :key="i" class="article-paragraph">{{ paragraph }}</div>
          </div>
        </div>
      </div>

      <n-empty v-else description="输入主题开始写作" class="py-16">
        <template #extra><p class="text-xs" style="color: var(--color-text-secondary)">AI 会基于你的知识库内容生成文章</p></template>
      </n-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { writingApi } from '@/api/writing'

const formData = reactive({ topic: '', style: null, keywords: '' })
const article = ref('')
const generating = ref(false)
const error = ref('')
const copied = ref(false)

const styleOptions = [
  { label: '默认', value: '' }, { label: '正式专业', value: '正式、专业' }, { label: '通俗易懂', value: '通俗易懂' },
  { label: '技术博客', value: '技术博客风格' }, { label: '学术论文', value: '学术论文风格' }, { label: '教程式', value: '教程式、步骤清晰' }
]

async function generate() {
  if (!formData.topic.trim()) return; generating.value = true; error.value = ''; article.value = ''
  try { const res = await writingApi.generate(formData.topic.trim(), formData.style || '', formData.keywords); article.value = res.data.data || '' } catch (err) { error.value = err.response?.data?.message || '文章生成失败' } finally { generating.value = false }
}

async function copyArticle() { try { await navigator.clipboard.writeText(article.value); copied.value = true; setTimeout(() => { copied.value = false }, 2000) } catch {} }
</script>

<style scoped>
.writing-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
}
.generate-writing-btn { --n-color: #d97706; --n-color-hover: #b45309; --n-color-pressed: #92400e; }
.writing-form-area { background-color: var(--color-surface); border-bottom: 1px solid var(--color-border); }
.article-paper {
  background-color: var(--color-surface); border: 1px solid var(--color-border); border-radius: 16px;
  padding: 32px; box-shadow: 0 4px 24px rgba(45, 42, 36, 0.06);
}
.article-meta { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 2px solid var(--color-border); }
.article-title { font-family: 'Playfair Display', Georgia, serif; font-size: 1.5rem; font-weight: 700; color: var(--color-text); line-height: 1.3; }
.article-body { line-height: 1.8; }
.article-paragraph { margin-bottom: 16px; color: var(--color-warm-gray); font-size: 0.9rem; }
</style>