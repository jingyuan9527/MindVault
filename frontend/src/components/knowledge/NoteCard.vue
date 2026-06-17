<template>
  <div class="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow cursor-pointer"
    @click="$emit('click', note)">
    <h3 class="font-semibold text-gray-800 text-base leading-tight mb-2">{{ note.title }}</h3>
    <p class="text-sm text-gray-500 line-clamp-3 leading-relaxed mb-3">{{ note.content }}</p>
    <div class="flex flex-wrap gap-1.5 mb-2" v-if="parsedTags.length">
      <span v-for="tag in parsedTags" :key="tag"
        class="px-2 py-0.5 text-xs rounded bg-gray-100 text-gray-600">#{{ tag }}</span>
    </div>
    <div class="flex items-center justify-between pt-2 border-t border-gray-100">
      <span class="text-xs text-gray-400">{{ formatTime(note.createdAt) }}</span>
      <button @click.stop="$emit('delete', note.id)"
        class="text-xs text-gray-400 hover:text-red-500 transition-colors">删除</button>
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