<template>
  <div v-if="preview" :class="['content-renderer preview', $attrs.class]" v-bind="$attrs">{{ plainText }}</div>
  <div v-else :class="['content-renderer', $attrs.class]" v-bind="$attrs" v-html="rendered"></div>
</template>

<script setup>
import { computed, useAttrs } from 'vue'
import { marked } from 'marked'

const attrs = useAttrs()

const props = defineProps({
  content: { type: String, default: '' },
  preview: { type: Boolean, default: false }
})

marked.setOptions({
  breaks: true,
  gfm: true
})

const rendered = computed(() => {
  if (!props.content) return ''
  return marked.parse(props.content)
})

const plainText = computed(() => {
  if (!props.content) return ''
  const html = rendered.value
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || div.innerText || ''
})
</script>

<style scoped>
.content-renderer {
  line-height: 1.7;
  word-break: break-word;
}
.content-renderer.preview {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}
.content-renderer :deep(p) {
  margin-bottom: 0.75em;
}
.content-renderer :deep(h1),
.content-renderer :deep(h2),
.content-renderer :deep(h3),
.content-renderer :deep(h4) {
  font-weight: 600;
  margin-top: 1.25em;
  margin-bottom: 0.5em;
  line-height: 1.3;
}
.content-renderer :deep(h1) { font-size: 1.25rem; }
.content-renderer :deep(h2) { font-size: 1.1rem; }
.content-renderer :deep(h3) { font-size: 1rem; }
.content-renderer :deep(ul),
.content-renderer :deep(ol) {
  padding-left: 1.5em;
  margin-bottom: 0.75em;
}
.content-renderer :deep(li) {
  margin-bottom: 0.25em;
}
.content-renderer :deep(code) {
  background-color: rgba(0,0,0,0.06);
  padding: 0.15em 0.4em;
  border-radius: 3px;
  font-size: 0.9em;
}
.content-renderer :deep(pre) {
  background-color: #1e1e2e;
  color: #cdd6f4;
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
  margin-bottom: 0.75em;
  font-size: 0.85rem;
}
.content-renderer :deep(pre code) {
  background: none;
  padding: 0;
  border-radius: 0;
  font-size: inherit;
}
.content-renderer :deep(blockquote) {
  border-left: 3px solid var(--color-sage, #a3b18a);
  padding-left: 1em;
  margin-left: 0;
  margin-bottom: 0.75em;
  opacity: 0.85;
}
.content-renderer :deep(a) {
  color: var(--color-sage, #a3b18a);
  text-decoration: underline;
}
.content-renderer :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 0.75em;
  font-size: 0.85rem;
}
.content-renderer :deep(th),
.content-renderer :deep(td) {
  border: 1px solid var(--color-border, #e0e0e0);
  padding: 0.4em 0.6em;
  text-align: left;
}
.content-renderer :deep(th) {
  font-weight: 600;
  background-color: rgba(0,0,0,0.03);
}
.content-renderer :deep(hr) {
  border: none;
  border-top: 1px solid var(--color-border, #e0e0e0);
  margin: 1em 0;
}
.content-renderer :deep(img) {
  max-width: 100%;
  border-radius: 4px;
  margin: 0.5em 0;
}
</style>