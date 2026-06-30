<template>
  <div v-if="visible" class="note-drawer">
    <div class="drawer-header">
      <button v-if="canGoBack" class="drawer-back" @click="$emit('back')">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
        </svg>
      </button>
      <h2 class="drawer-title">{{ displayTitle }}</h2>
      <button class="drawer-close" @click="$emit('update:visible', false)">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <div class="drawer-body">
      <div v-if="mergedTags.length" class="drawer-tags">
        <span v-for="tag in mergedTags" :key="tag" class="tag-pill">#{{ tag }}</span>
      </div>
      <ContentRenderer :content="note?.content || ''" class="drawer-content" />
    </div>

    <div class="drawer-related">
      <h3 class="related-title">关联笔记</h3>
      <div v-if="relatedNotes.length" class="related-list">
        <div
          v-for="related in relatedNotes"
          :key="related.id"
          class="related-item"
          @click="$emit('navigate', related)"
        >
          <div class="related-info">
            <p class="related-name">{{ related.title || related.aiTitle || '无标题' }}</p>
            <p v-if="related.summary" class="related-summary">{{ related.summary }}</p>
          </div>
          <span v-if="related.similarity != null" class="related-score">
            {{ Math.round(related.similarity * 100) }}%
          </span>
        </div>
      </div>
      <p v-else class="related-empty">暂无关联</p>
    </div>

    <div class="drawer-footer">
      <button class="drawer-edit" @click="$emit('edit')">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
        </svg>
        <span>编辑</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  note: { type: Object, default: null },
  relatedNotes: { type: Array, default: () => [] },
  canGoBack: { type: Boolean, default: false },
})
defineEmits(['update:visible', 'navigate', 'back', 'edit'])

const displayTitle = computed(() => props.note?.aiTitle || props.note?.title || '无标题')

const mergedTags = computed(() => {
  if (!props.note) return []
  const ai = parseTags(props.note.tags)
  const user = parseTags(props.note.userTags)
  return [...new Set([...ai, ...user])]
})

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}
</script>

<style scoped>
.note-drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: 420px;
  max-width: 90vw;
  height: 100vh;
  background: var(--color-bg);
  border-left: 1px solid var(--color-border);
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  z-index: 100;
  animation: slideIn 0.2s ease;
}
@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}
.drawer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.drawer-back {
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
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.drawer-back:hover {
  background: var(--color-surface);
  color: var(--color-text);
}
.drawer-title {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
}
.drawer-close {
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
  flex-shrink: 0;
  transition: all 0.15s ease;
}
.drawer-close:hover {
  background: var(--color-surface);
  color: var(--color-text);
}
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.drawer-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}
.tag-pill {
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: 2px 8px;
  border-radius: 4px;
}
.drawer-content {
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.7;
}
.drawer-related {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
  max-height: 35vh;
  overflow-y: auto;
}
.related-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin: 0 0 8px 0;
}
.related-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.related-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
}
.related-item:hover {
  background: var(--color-surface);
}
.related-info {
  flex: 1;
  min-width: 0;
}
.related-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.related-summary {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 2px 0 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.related-score {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  flex-shrink: 0;
}
.related-empty {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  padding: 8px 0;
}
.drawer-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}
.drawer-edit {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
  background: var(--color-primary-light);
  border: 1px solid var(--color-primary);
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
}
.drawer-edit:hover {
  opacity: 0.9;
}
@media (max-width: 640px) {
  .note-drawer {
    width: 100vw;
    max-width: 100vw;
  }
}
</style>
