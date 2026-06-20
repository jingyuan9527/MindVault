<template>
  <n-dynamic-tags v-model:value="tags" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: String, default: '[]' },
  placeholder: { type: String, default: '输入标签，回车添加' }
})

const emit = defineEmits(['update:modelValue'])

const tags = computed({
  get() {
    try { return JSON.parse(props.modelValue) || [] } catch { return [] }
  },
  set(val) {
    emit('update:modelValue', JSON.stringify(val))
  }
})
</script>
