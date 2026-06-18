<template>
  <div class="flex flex-col h-full">
    <div class="p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <h2 class="font-display text-xl mb-4">写作辅助</h2>
      <div class="space-y-3">
        <div>
          <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">写作主题</label>
          <input v-model="topic" placeholder="输入文章主题，如「Spring Boot 最佳实践」" class="input-field" />
        </div>
        <div class="flex gap-4">
          <div class="flex-1">
            <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">写作风格（可选）</label>
            <select v-model="style" class="input-field">
              <option value="">默认</option>
              <option value="正式、专业">正式专业</option>
              <option value="通俗易懂">通俗易懂</option>
              <option value="技术博客风格">技术博客</option>
              <option value="学术论文风格">学术论文</option>
              <option value="教程式、步骤清晰">教程式</option>
            </select>
          </div>
          <div class="flex-1">
            <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">关键词（可选）</label>
            <input v-model="keywords" placeholder="逗号分隔" class="input-field" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="generate" :disabled="!topic.trim() || generating"
            class="btn-primary">
            {{ generating ? '生成中...' : '生成文章' }}
          </button>
        </div>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-5">
      <div v-if="generating" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin" style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="error" class="card p-6" :style="{ borderLeft: '3px solid var(--color-accent)' }">
        <p style="color: var(--color-accent)">{{ error }}</p>
      </div>

      <div v-else-if="article" class="max-w-3xl mx-auto">
        <div class="card p-6 fade-in-enter">
          <div class="flex items-start justify-between mb-4">
            <h3 class="font-display text-lg font-bold" style="color: var(--color-text)">{{ topic }}</h3>
            <button @click="copyArticle"
              class="text-xs px-3 py-1.5 rounded-lg transition-colors duration-150"
              style="color: var(--color-text-secondary); border: 1px solid var(--color-border)"
              @mouseenter="$event.target.style.borderColor = 'var(--color-accent)'; $event.target.style.color = 'var(--color-accent)'"
              @mouseleave="$event.target.style.borderColor = 'var(--color-border)'; $event.target.style.color = 'var(--color-text-secondary)'">
              {{ copied ? '已复制' : '复制全文' }}
            </button>
          </div>
          <div class="prose prose-sm max-w-none text-sm leading-relaxed whitespace-pre-wrap"
            style="color: var(--color-warm-gray); line-height: 1.8">
            {{ article }}
          </div>
        </div>
      </div>

      <div v-else class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-10 h-10 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">输入主题开始写作</p>
        <p class="text-sm mt-1">AI 会基于你的知识库内容生成文章</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { writingApi } from '@/api/writing'

const topic = ref('')
const style = ref('')
const keywords = ref('')
const article = ref('')
const generating = ref(false)
const error = ref('')
const copied = ref(false)

async function generate() {
  if (!topic.value.trim()) return
  generating.value = true
  error.value = ''
  article.value = ''
  try {
    const res = await writingApi.generate(topic.value.trim(), style.value, keywords.value)
    article.value = res.data.data || ''
  } catch (err) {
    error.value = err.response?.data?.message || '文章生成失败'
  } finally {
    generating.value = false
  }
}

async function copyArticle() {
  try {
    await navigator.clipboard.writeText(article.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {}
}
</script>