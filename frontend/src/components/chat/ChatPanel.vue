<template>
  <div class="flex flex-col h-full bg-white border-l border-gray-200">
    <div class="flex items-center justify-between p-3 border-b border-gray-200 shrink-0">
      <h3 class="text-sm font-semibold text-gray-700">AI 助手</h3>
      <button @click="$emit('close')"
        class="text-gray-400 hover:text-gray-600 text-lg leading-none">&times;</button>
    </div>

    <div class="flex-1 overflow-y-auto p-3 space-y-3" ref="msgContainer">
      <div v-if="!chatStore.messages.length" class="flex flex-col items-center justify-center h-full text-gray-400">
        <p class="text-xs">开始对话，AI 帮你检索和整理笔记</p>
      </div>
      <MessageBubble
        v-for="msg in chatStore.messages"
        :key="msg.id"
        :message="msg.content"
        :isUser="msg.role === 'USER'"
        :time="formatTime(msg.createdAt)"
      />
      <ThinkingIndicator v-if="chatStore.isLoading" />
    </div>

    <ChatInput
      :disabled="chatStore.isLoading"
      @send="handleSend"
      class="shrink-0"
    />
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { useChatStore } from '@/stores/chat'
import MessageBubble from '@/components/chat/MessageBubble.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ThinkingIndicator from '@/components/chat/ThinkingIndicator.vue'

defineEmits(['close'])
const chatStore = useChatStore()
const msgContainer = ref(null)

watch(() => chatStore.messages.length, async () => {
  await nextTick()
  if (msgContainer.value) {
    msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  }
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

async function handleSend(content) {
  await chatStore.sendMessage(content)
}
</script>