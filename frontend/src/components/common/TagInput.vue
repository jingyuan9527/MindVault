<template>
  <div class="flex flex-wrap items-center gap-1.5 p-2 rounded-lg min-h-[38px] transition-colors duration-150"
    :class="{ 'ring-2': focused }"
    :style="{
      backgroundColor: 'var(--color-bg)',
      border: '1px solid ' + (focused ? 'var(--color-sage)' : 'var(--color-border)'),
      '--tw-ring-color': 'var(--color-sage)'
    }"
    @click.stop="inputRef?.focus()">
    <span v-for="(tag, idx) in tags" :key="idx"
      class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs"
      :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">
      {{ tag }}
      <button @click.stop="removeTag(idx)" class="hover:opacity-60 leading-none">&times;</button>
    </span>
    <input ref="inputRef" v-model="input"
      @keydown.enter.prevent="addTag"
      @keydown.backspace.prevent="handleBackspace"
      @keydown.,.prevent="addTag"
      @blur="focused = false"
      @focus="focused = true"
      :placeholder="tags.length ? '' : placeholder"
      class="flex-1 min-w-[80px] outline-none text-sm bg-transparent"
      :style="{ color: 'var(--color-text)' }" />
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '[]' },
  placeholder: { type: String, default: '输入标签，回车添加' }
})

const emit = defineEmits(['update:modelValue'])

const input = ref('')
const focused = ref(false)
const inputRef = ref(null)

const tags = ref(parseTags(props.modelValue))

function parseTags(str) {
  try { return JSON.parse(str) || [] } catch { return [] }
}

function emitTags() {
  emit('update:modelValue', JSON.stringify(tags.value))
}

function addTag() {
  const val = input.value.trim().replace(/[,\s]+$/, '')
  if (!val) return
  if (!tags.value.includes(val)) {
    tags.value.push(val)
    emitTags()
  }
  input.value = ''
}

function removeTag(idx) {
  tags.value.splice(idx, 1)
  emitTags()
}

function handleBackspace() {
  if (!input.value && tags.value.length) {
    removeTag(tags.value.length - 1)
  }
}

function setTags(str) {
  tags.value = parseTags(str)
}
</script>
