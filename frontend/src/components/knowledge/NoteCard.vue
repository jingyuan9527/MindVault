<template>
  <div class="card p-5 cursor-pointer flex flex-col" @click="$emit('click', note)">
    <h3 class="font-display text-base font-bold leading-tight mb-2 card-title" style="color: var(--color-text)">{{ note.aiTitle || note.title }}</h3>
    <p v-if="note.aiTitle && note.title" class="text-xs mb-1 truncate" style="color: var(--color-text-secondary)">原标题: {{ note.title }}</p>
    <div class="flex-1 min-h-0 mb-3">
      <ContentRenderer :content="note.content" preview class="line-clamp-3 text-sm" style="color: var(--color-warm-gray)" />
    </div>
    <div class="flex flex-wrap gap-1.5 mb-2" v-if="mergedTags.length">
      <router-link v-for="tag in mergedTags" :key="tag" :to="{ path: '/', query: { tag } }" @click.stop class="tag-pill">#{{ tag }}</router-link>
    </div>
    <div class="flex items-center justify-between pt-2 text-xs" style="border-top: 1px solid var(--color-border)">
      <span style="color: var(--color-text-secondary)">{{ formatTime(note.createdAt) }}</span>
      <button @click.stop="$emit('delete', note.id)"
        class="transition-colors duration-150 hover-accent"
        style="color: var(--color-text-secondary)">删除</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'

const props = defineProps({ note: { type: Object, required: true } })
defineEmits(['click', 'delete'])

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
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>