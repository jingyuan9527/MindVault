<template>
  <div class="card p-5 cursor-pointer" @click="$emit('click', note)">
    <h3 class="font-display text-base font-bold leading-tight mb-2" style="color: var(--color-text)">{{ note.title }}</h3>
    <p class="text-sm leading-relaxed mb-3 line-clamp-3" style="color: var(--color-warm-gray); line-height: 1.6">{{ note.content }}</p>
    <div class="flex flex-wrap gap-1.5 mb-2" v-if="parsedTags.length">
      <span v-for="tag in parsedTags" :key="tag" class="tag-pill">#{{ tag }}</span>
    </div>
    <div class="flex items-center justify-between pt-2 text-xs" style="border-top: 1px solid var(--color-border)">
      <span style="color: var(--color-text-secondary)">{{ formatTime(note.createdAt) }}</span>
      <button @click.stop="$emit('delete', note.id)"
        class="transition-colors duration-150"
        style="color: var(--color-text-secondary)"
        @mouseenter="$event.target.style.color = 'var(--color-accent)'"
        @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">删除</button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ note: { type: Object, required: true } })
defineEmits(['click', 'delete'])

const parsedTags = computed(() => {
  if (!props.note.tags) return []
  try { return JSON.parse(props.note.tags) } catch { return [] }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}
</script>