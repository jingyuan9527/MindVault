<template>
  <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 pointer-events-none">
    <transition-group name="toast">
      <div v-for="msg in store.messages" :key="msg.id"
        class="pointer-events-auto px-4 py-3 rounded-lg shadow-lg text-sm max-w-sm flex items-center gap-2 transition-all duration-300"
        :style="bgColor(msg.type)">
        <span v-if="msg.type === 'success'" class="shrink-0">✓</span>
        <span v-else-if="msg.type === 'error'" class="shrink-0">✕</span>
        <span v-else class="shrink-0">ℹ</span>
        <span style="color: white">{{ msg.text }}</span>
        <n-button text size="tiny" @click="store.remove(msg.id)" class="ml-auto shrink-0 !text-white/70 hover:!text-white">✕</n-button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { useToastStore } from '@/stores/toast'
const store = useToastStore()

function bgColor(type) {
  const colors = { success: '#4a7c59', error: '#cf7058', info: '#6b7280' }
  return { backgroundColor: colors[type] || colors.info }
}
</script>

<style scoped>
.toast-enter-from { transform: translateX(100%); opacity: 0 }
.toast-leave-to { transform: translateX(100%); opacity: 0 }
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease }
</style>
