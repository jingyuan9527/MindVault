<template>
  <div class="flex flex-col h-full"
    :style="{ backgroundColor: 'var(--color-surface)', borderLeft: '1px solid var(--color-border)' }">
    <div class="flex items-center justify-between p-4 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <h3 class="text-sm font-semibold" style="color: var(--color-text)">AI 助手</h3>
      <button @click="$emit('close')"
        class="text-lg leading-none transition-colors duration-150 hover-text"
        style="color: var(--color-text-secondary)">&times;</button>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-4" ref="msgContainer">
      <div v-if="!chatStore.messages.length" class="flex flex-col items-center justify-center h-full" style="color: var(--color-text-secondary)">
        <svg class="w-8 h-8 mb-3 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
        </svg>
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