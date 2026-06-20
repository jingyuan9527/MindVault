<template>
  <n-card
    hoverable
    size="small"
    class="note-card"
    @click="$emit('click', note)"
  >
    <template #header>
      <div>
        <div class="text-sm font-medium leading-tight">{{ note.aiTitle || note.title }}</div>
        <div v-if="note.aiTitle && note.title" class="text-xs mt-0.5" style="color: var(--color-text-secondary)">原标题: {{ note.title }}</div>
      </div>
    </template>

    <ContentRenderer :content="note.content" preview class="line-clamp-3 text-sm" style="color: var(--color-warm-gray)" />

    <template v-if="mergedTags.length" #footer>
      <n-space size="small" class="flex-wrap">
        <n-tag v-for="tag in mergedTags" :key="tag" size="tiny" :bordered="false" class="tag-sage">#{{ tag }}</n-tag>
      </n-space>
    </template>

    <template #action>
      <n-space align="center" justify="space-between" class="w-full">
        <n-time :time="new Date(note.createdAt)" format="yyyy-MM-dd HH:mm" class="text-xs" />
        <n-button text size="tiny" type="error" @click.stop="$emit('delete', note.id)">删除</n-button>
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
.tag-sage {
  --n-color: var(--color-sage-light);
  --n-text-color: var(--color-sage);
}
</style>
