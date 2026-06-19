<template>
  <div class="flex items-center px-5 py-3 cursor-pointer transition-colors duration-150 hover:opacity-80 hover-sage-bg"
    style="border-bottom: 1px solid var(--color-border)"
    @click="$emit('click', note)">
    <div class="flex items-center mr-3" @click.stop>
      <input type="checkbox" :checked="selected" @change="$emit('toggle-select', note.id)"
        class="w-4 h-4 rounded cursor-pointer"
        :style="{ accentColor: 'var(--color-sage)' }" />
    </div>
    <div class="flex-1 min-w-0">
      <p class="text-sm font-medium truncate" style="color: var(--color-text)">{{ note.title }}</p>
      <div class="text-xs mt-0.5 truncate" style="color: var(--color-warm-gray)">
        <ContentRenderer :content="note.summary || note.content" preview />
      </div>
    </div>
    <div class="flex flex-wrap gap-1 mx-3 max-w-[200px]" v-if="parsedTags.length">
      <router-link v-for="tag in parsedTags.slice(0, 3)" :key="tag" :to="{ path: '/', query: { tag } }" @click.stop class="tag-pill text-xs">#{{ tag }}</router-link>
      <span v-if="parsedTags.length > 3" class="text-xs" style="color: var(--color-text-secondary)">+{{ parsedTags.length - 3 }}</span>
    </div>
    <span class="text-xs shrink-0" style="color: var(--color-text-secondary)">{{ formatTime(note.createdAt) }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'

const props = defineProps({
  note: Object,
  selected: Boolean
})
defineEmits(['click', 'toggle-select'])

const parsedTags = computed(() => {
  if (!props.note.tags) return []
  try { return JSON.parse(props.note.tags) } catch { return [] }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
}
</script>