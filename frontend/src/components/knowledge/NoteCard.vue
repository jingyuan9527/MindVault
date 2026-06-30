<template>
  <div
    class="note-card"
    :class="{ editing: isEditing, selected }"
    @click="$emit('click', note)"
  >
    <template v-if="!isEditing">
      <div class="card-header">
        <span class="card-time">{{ formatTime(note.createdAt) }}</span>
        <div class="flex flex-wrap gap-1">
          <span v-for="tag in mergedTags" :key="tag" class="tag-pill">#{{ tag }}</span>
        </div>
      </div>
      <div class="card-title-row">
        <h3 class="card-title">{{ note.aiTitle || note.title || '无标题' }}</h3>
        <span v-if="note.aiTitle && note.title" class="card-original-title">原标题: {{ note.title }}</span>
      </div>
      <div class="card-content">
        <ContentRenderer :content="note.content" preview class="line-clamp-4" />
      </div>
      <div class="card-actions" @click.stop>
        <button class="action-btn" title="编辑" @click="$emit('edit', note)">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
        </button>
        <button class="action-btn action-btn-danger" title="删除" @click="$emit('delete', note.id)">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </button>
      </div>
    </template>
    <div v-else class="p-2">
      <slot name="edit-form" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'

const props = defineProps({
  note: { type: Object, required: true },
  isEditing: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
})
defineEmits(['click', 'edit', 'delete'])

const mergedTags = computed(() => {
  const ai = parseTags(props.note.tags)
  const user = parseTags(props.note.userTags)
  return [...new Set([...ai, ...user])]
})

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
}
</script>

<style scoped>
.note-card {
  background: var(--color-surface);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
  transition: all 0.15s ease;
  cursor: pointer;
}
.note-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
.note-card.selected {
  border: 2px solid var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary-light);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.card-time { font-size: 12px; color: var(--color-text-secondary); }
.card-title-row { margin-bottom: 8px; }
.card-title { font-size: 16px; font-weight: 600; line-height: 1.4; color: var(--color-text); }
.card-original-title { display: block; font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.card-content { color: var(--color-text); line-height: 1.7; }
.card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.action-btn:hover { background: var(--color-bg); color: var(--color-text); }
.action-btn-danger:hover { color: var(--color-danger); background: rgba(239, 68, 68, 0.08); }
</style>