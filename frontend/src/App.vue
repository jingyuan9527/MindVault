<template>
  <div class="flex h-screen">
    <AppSidebar />
    <main class="flex-1 flex overflow-hidden">
      <div class="flex-1 flex flex-col overflow-hidden">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
      <button v-if="!chatOpen"
        @click="chatOpen = true"
        class="fixed bottom-6 right-6 w-12 h-12 flex items-center justify-center rounded-full shadow-lg z-40 transition-all duration-200 hover:scale-105"
        :style="{ backgroundColor: 'var(--color-accent)', color: 'white' }">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
        </svg>
      </button>
      <transition name="panel-transition">
        <div v-show="chatOpen" class="w-96 shrink-0">
          <ChatPanel @close="chatOpen = false" />
        </div>
      </transition>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const chatOpen = ref(true)
</script>