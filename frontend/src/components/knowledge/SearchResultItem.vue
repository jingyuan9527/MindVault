<template>
  <div class="search-result-item" @click="$emit('click', result)">
    <div class="result-main">
      <div class="result-title-row">
        <span class="result-title">{{ displayTitle }}</span>
        <span v-if="similarityPercent != null" class="result-score">{{ similarityPercent }}%</span>
      </div>
      <p class="result-snippet" v-html="highlightedSnippet"></p>
      <div v-if="mergedTags.length" class="result-tags">
        <span v-for="tag in mergedTags.slice(0, 4)" :key="tag" class="tag-pill">#{{ tag }}</span>
        <span v-if="mergedTags.length > 4" class="tag-overflow">+{{ mergedTags.length - 4 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  result: { type: Object, required: true },
  keyword: { type: String, default: '' },
})
defineEmits(['click'])

const displayTitle = computed(() => props.result.aiTitle || props.result.title || '无标题')

const similarityPercent = computed(() => {
  if (props.result.similarity == null) return null
  return Math.round(props.result.similarity * 100)
})

const mergedTags = computed(() => {
  const ai = parseTags(props.result.tags)
  const user = parseTags(props.result.userTags)
  return [...new Set([...ai, ...user])]
})

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function stripMarkdown(text) {
  if (!text) return ''
  return text
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[.*?\]\(.*?\)/g, ' ')
    .replace(/\[([^\]]*)\]\(.*?\)/g, '$1')
    .replace(/[#>*_~-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

const highlightedSnippet = computed(() => {
  const kw = props.keyword?.trim()
  const content = stripMarkdown(props.result.content)
  const summary = props.result.summary || ''

  let snippet = ''
  if (kw && content) {
    const idx = content.toLowerCase().indexOf(kw.toLowerCase())
    if (idx >= 0) {
      const start = Math.max(0, idx - 30)
      const end = Math.min(content.length, idx + kw.length + 30)
      snippet = (start > 0 ? '…' : '') + content.slice(start, end) + (end < content.length ? '…' : '')
    }
  }
  if (!snippet && summary) {
    snippet = summary.slice(0, 80) + (summary.length > 80 ? '…' : '')
  }
  if (!snippet && content) {
    snippet = content.slice(0, 80) + (content.length > 80 ? '…' : '')
  }

  snippet = escapeHtml(snippet || '')

  if (kw && snippet) {
    const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const re = new RegExp(`(${escaped})`, 'gi')
    snippet = snippet.replace(re, '<mark>$1</mark>')
  }
  return snippet
})
</script>

<style scoped>
.search-result-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s ease;
}
.search-result-item:hover {
  background: var(--color-surface);
}
.result-main {
  flex: 1;
  min-width: 0;
}
.result-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}
.result-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.result-score {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-surface);
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}
.result-snippet {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.result-snippet :deep(mark) {
  background: rgba(255, 235, 59, 0.35);
  color: var(--color-text);
  border-radius: 2px;
  padding: 0 1px;
}
.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}
.tag-pill {
  font-size: 11px;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: 1px 6px;
  border-radius: 4px;
}
.tag-overflow {
  font-size: 11px;
  color: var(--color-text-secondary);
}
</style>
