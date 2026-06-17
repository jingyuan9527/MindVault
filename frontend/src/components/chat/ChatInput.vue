<template>
  <div class="border-t border-gray-200 bg-white p-4">
    <div class="flex items-end gap-2 max-w-4xl mx-auto">
      <textarea
        v-model="text"
        placeholder="输入消息，或粘贴链接/图片..."
        class="flex-1 resize-none rounded-xl border border-gray-300 px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
        rows="2"
        @keydown.enter.exact="handleSend"
      ></textarea>
      <button
        @click="handleSend"
        :disabled="!text.trim() || disabled"
        class="px-4 py-2.5 bg-blue-600 text-white rounded-xl text-sm hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
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