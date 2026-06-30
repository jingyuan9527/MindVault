<template>
  <div
class="flex items-center px-5 py-3 cursor-pointer transition-colors duration-150 hover:opacity-80 hover-sage-bg note-list-item"
    :class="{ highlighted }"
    style="border-bottom: 1px solid var(--color-border)"
    @click="$emit('click', note)">
    <div class="flex-1 min-w-0">
      <p class="text-sm font-medium truncate" style="color: var(--color-text)">{{ note.aiTitle || note.title }}</p>
      <p v-if="note.aiTitle && note.title" class="text-xs truncate" style="color: var(--color-text-secondary)">原标题: {{ note.title }}</p>
      <div class="text-xs mt-0.5 truncate" style="color: var(--color-warm-gray)">
        <ContentRenderer :content="note.summary || note.content" preview />
      </div>
    </div>
    <div v-if="mergedTags.length" class="flex flex-wrap gap-1 mx-3 max-w-[200px]">
      <router-link v-for="tag in mergedTags.slice(0, 3)" :key="tag" :to="{ path: '/', query: { tag } }" @click.stop><n-tag size="tiny" type="primary" :bordered="false">#{{ tag }}</n-tag></router-link>
      <span v-if="mergedTags.length > 3" class="text-xs" style="color: var(--color-text-secondary)">+{{ mergedTags.length - 3 }}</span>
    </div>
    <span class="text-xs shrink-0" style="color: var(--color-text-secondary)">{{ formatTime(note.createdAt) }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'

const props = defineProps({
  note: Object,
  highlighted: { type: Boolean, default: false },
})
defineEmits(['click'])

const parsedTags = computed(() => {
  if (!props.note.tags) return []
  try { return JSON.parse(props.note.tags) } catch { return [] }
})

const mergedTags = computed(() => {
  const ai = parsedTags.value
  let user = []
  if (props.note.userTags) {
    try { user = JSON.parse(props.note.userTags) } catch {}
  }
  const merged = new Set([...ai, ...user])
  return [...merged]
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
}
</script>

<style scoped>
.note-list-item.highlighted {
  background: var(--color-primary-light);
  border-left: 3px solid var(--color-primary);
}
</style>