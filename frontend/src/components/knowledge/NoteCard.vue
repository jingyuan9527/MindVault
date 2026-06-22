<template>
  <n-card
    hoverable
    size="medium"
    class="note-card"
    :segmented="{ footer: 'soft', action: 'soft' }"
    @click="$emit('click', note)"
  >
    <template #header>
      <div>
        <div class="text-sm font-semibold leading-snug">{{ note.aiTitle || note.title }}</div>
        <div v-if="note.aiTitle && note.title" class="text-xs mt-1" style="color: var(--color-text-secondary)">原标题: {{ note.title }}</div>
      </div>
    </template>

    <ContentRenderer :content="note.content" preview class="line-clamp-3 text-sm leading-relaxed" style="color: var(--color-warm-gray)" />

    <template v-if="mergedTags.length" #footer>
      <div class="flex flex-wrap gap-1">
        <n-tag v-for="tag in mergedTags" :key="tag" size="tiny" type="primary" :bordered="false">#{{ tag }}</n-tag>
      </div>
    </template>

    <template #action>
      <n-space align="center" justify="space-between" class="w-full px-1">
        <n-time :time="new Date(note.createdAt)" format="yyyy-MM-dd HH:mm" class="text-xs" />
        <n-button quaternary size="tiny" type="error" @click.stop="$emit('delete', note.id)">
          <template #icon>
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
          </template>
        </n-button>
      </n-space>
    </template>
  </n-card>
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
</script>

<style scoped>
.note-card {
  cursor: pointer;
}
</style>
