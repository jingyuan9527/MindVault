<template>
  <div class="p-4" :style="{ borderTop: '1px solid var(--color-border)', backgroundColor: 'var(--color-surface)' }">
    <div class="flex items-end gap-2 max-w-4xl mx-auto">
      <n-input
        v-model:value="text"
        type="textarea"
        :rows="2"
        placeholder="输入消息，或粘贴链接/图片..."
        class="flex-1"
        @keydown.enter.exact="handleSend"
      />
      <n-button
        type="primary"
        :disabled="!text.trim() || disabled"
        class="shrink-0"
        @click="handleSend"
      >
        发送
      </n-button>
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