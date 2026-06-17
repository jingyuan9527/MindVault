<template>
  <div class="p-4" :style="{ borderTop: '1px solid var(--color-border)', backgroundColor: 'var(--color-surface)' }">
    <div class="flex items-end gap-2 max-w-4xl mx-auto">
      <textarea
        v-model="text"
        placeholder="输入消息，或粘贴链接/图片..."
        class="flex-1 resize-none rounded-xl px-4 py-2.5 text-sm transition-all duration-150 outline-none"
        :style="{
          border: '1px solid var(--color-border)',
          backgroundColor: 'var(--color-bg)',
          color: 'var(--color-text)'
        }"
        rows="2"
        @focus="$event.target.style.borderColor = 'var(--color-accent)'"
        @blur="$event.target.style.borderColor = 'var(--color-border)'"
        @keydown.enter.exact="handleSend"
      ></textarea>
      <button
        @click="handleSend"
        :disabled="!text.trim() || disabled"
        class="px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
        :style="{
          backgroundColor: text.trim() && !disabled ? 'var(--color-accent)' : 'var(--color-border)',
          color: text.trim() && !disabled ? 'white' : 'var(--color-text-secondary)'
        }"
      >
        发送
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['send'])
defineProps({ disabled: Boolean })

const text = ref('')

function handleSend() {
  if (text.value.trim()) {
    emit('send', text.value.trim())
    text.value = ''
  }
}
</script>