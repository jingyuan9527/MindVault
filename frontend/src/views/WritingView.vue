<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <n-form label-placement="top" :model="formData">
        <n-form-item label="写作主题">
          <n-input v-model:value="formData.topic" placeholder="输入文章主题，如「Spring Boot 最佳实践」" />
        </n-form-item>
        <n-space size="medium">
          <n-form-item label="写作风格（可选）" class="flex-1">
            <n-select v-model:value="formData.style" :options="styleOptions" placeholder="默认" clearable />
          </n-form-item>
          <n-form-item label="关键词（可选）" class="flex-1">
            <n-input v-model:value="formData.keywords" placeholder="逗号分隔" />
          </n-form-item>
        </n-space>
        <div class="flex justify-end">
          <n-button type="primary" :loading="generating" :disabled="!formData.topic.trim()" @click="generate">
            {{ generating ? '生成中...' : '生成文章' }}
          </n-button>
        </div>
      </n-form>
    </div>

    <div class="flex-1 overflow-y-auto p-4 md:p-5">
      <n-spin v-if="generating" class="flex justify-center py-12" />

      <n-alert v-else-if="error" type="error" :show-icon="true" closable @close="error = ''">
        {{ error }}
      </n-alert>

      <n-card v-else-if="article" class="max-w-3xl mx-auto" size="small">
        <template #header>
          <div class="flex items-start justify-between">
            <h3 class="font-display text-lg font-bold">{{ formData.topic }}</h3>
            <n-button text size="tiny" type="primary" @click="copyArticle">
              {{ copied ? '已复制' : '复制全文' }}
            </n-button>
          </div>
        </template>
        <div class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--color-warm-gray); line-height: 1.8">
          {{ article }}
        </div>
      </n-card>

      <n-empty v-else description="输入主题开始写作">
        <template #extra>
          <p class="text-xs" style="color: var(--color-text-secondary)">AI 会基于你的知识库内容生成文章</p>
        </template>
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
  { label: '默认', value: '' },
  { label: '正式专业', value: '正式、专业' },
  { label: '通俗易懂', value: '通俗易懂' },
  { label: '技术博客', value: '技术博客风格' },
  { label: '学术论文', value: '学术论文风格' },
  { label: '教程式', value: '教程式、步骤清晰' }
]

async function generate() {
  if (!formData.topic.trim()) return
  generating.value = true
  error.value = ''
  article.value = ''
  try {
    const res = await writingApi.generate(formData.topic.trim(), formData.style || '', formData.keywords)
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
