<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 px-4 md:px-5 py-3 flex items-center justify-between" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3">
        <div class="chat-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg">AI 对话</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">你的AI知识助手</p>
        </div>
      </div>
      <n-button text size="tiny" style="color: var(--color-text-secondary)" @click="store.clearMessages()">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
        </svg>
      </n-button>
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-4" ref="messageContainer">
      <div v-if="error" class="flex flex-col items-center justify-center h-full">
        <div class="error-icon">!</div>
        <p class="text-sm mt-2" style="color: #ef4444">{{ error }}</p>
        <n-button text size="tiny" type="primary" @click="retry" class="mt-2">重试</n-button>
      </div>

      <div v-else-if="!store.messages.length" class="flex flex-col items-center justify-center h-full px-4">
        <div class="chat-empty-icon">
          <svg class="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
          </svg>
        </div>
        <p class="text-lg font-display font-medium mt-4" style="color: var(--color-text)">开始你的第一段对话</p>
        <p class="text-sm mt-1 text-center max-w-sm" style="color: var(--color-text-secondary)">输入问题或分享知识，知忆会帮你整理和检索。试试问一个关于你知识库的问题。</p>
        <div class="flex flex-wrap gap-2 mt-6 justify-center">
          <n-button secondary size="tiny" @click="handleSend('帮我总结今天学到的知识点')">总结知识点</n-button>
          <n-button secondary size="tiny" @click="handleSend('我最近在学习什么？')">我在学什么</n-button>
          <n-button secondary size="tiny" @click="handleSend('帮我解释一下间隔复习')">解释间隔复习</n-button>
        </div>
      </div>

      <MessageBubble v-for="msg in store.messages" :key="msg.id"
        :message="msg.content" :isUser="msg.role === 'USER'"
        :blocked="msg.blocked"
        :time="formatTime(msg.createdAt)" :sources="msg.sources"
        :toolResults="msg.toolResults || []" />
      <div v-if="store.isLoading">
        <ThinkingIndicator />
        <div v-if="store.streamingToolCall" class="flex items-center gap-2 px-4 py-2">
          <div class="w-3 h-3 rounded-full border-2 border-t-transparent animate-spin"
            :style="{ borderColor: 'var(--color-sage)', borderTopColor: 'transparent' }" />
          <span class="text-xs" style="color: var(--color-text-secondary)">
            正在搜索知识库：{{ store.streamingToolCall.args?.query || '' }}
          </span>
        </div>
      </div>
    </div>

    <ChatInput :disabled="store.isLoading" @send="handleSend" />
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
const error = ref('')

watch(() => store.messages.length, async () => {
  await nextTick()
  if (messageContainer.value) messageContainer.value.scrollTop = messageContainer.value.scrollHeight
})

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

async function handleSend(content) {
  error.value = ''
  try { await store.sendMessage(content) } catch (e) { error.value = e.response?.data?.message || '发送失败，请稍后重试' }
}

function retry() { error.value = '' }
</script>

<style scoped>
.chat-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-sage) 0%, #4a6a47 100%);
}
.chat-empty-icon {
  width: 80px; height: 80px; border-radius: 24px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-sage);
  background-color: var(--color-sage-light);
  opacity: 0.7;
}
.error-icon {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background-color: #fef2f2; color: #ef4444;
  font-weight: bold; font-size: 1.25rem;
}
</style>