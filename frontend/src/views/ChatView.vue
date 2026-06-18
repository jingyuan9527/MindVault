<template>
  <div class="flex flex-col h-full">
    <div class="flex-1 overflow-y-auto p-4 space-y-4" ref="messageContainer">
      <div v-if="!store.messages.length" class="flex flex-col items-center justify-center h-full text-gray-400">
        <p class="text-4xl mb-4">🧠</p>
        <p class="text-lg font-medium text-gray-600">开始你的第一段对话</p>
        <p class="text-sm mt-1">输入问题或分享知识，知忆会帮你整理和检索</p>
      </div>

      <MessageBubble
        v-for="msg in store.messages"
        :key="msg.id"
        :message="msg.content"
        :isUser="msg.role === 'USER'"
        :time="formatTime(msg.createdAt)"
        :sources="msg.sources"
      />

      <ThinkingIndicator v-if="store.isLoading" />
    </div>

    <ChatInput
      :disabled="store.isLoading"
      @send="handleSend"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ThinkingIndicator from '@/components/chat/ThinkingIndicator.vue'

const store = useChatStore()
const messageContainer = ref(null)

watch(() => store.messages.length, async () => {
  await nextTick()
  if (messageContainer.value) {
    messageContainer.value.scrollTop = messageContainer.value.scrollHeight
  }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

async function handleSend(content) {
  await store.sendMessage(content)
}
</script>