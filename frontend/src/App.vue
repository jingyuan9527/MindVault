<template>
  <div class="flex h-screen" :style="{ backgroundColor: 'var(--color-bg)' }">
    <!-- Mobile overlay -->
    <transition name="fade">
      <div v-if="sidebarOpen" class="fixed inset-0 z-30 lg:hidden"
        style="background-color: rgba(45,42,36,0.3); backdrop-filter: blur(2px)"
        @click="sidebarOpen = false"></div>
    </transition>

    <!-- Sidebar -->
    <transition name="sidebar-slide">
      <div v-if="sidebarOpen || isDesktop" class="sidebar-drawer">
        <AppSidebar @close="sidebarOpen = false" />
      </div>
    </transition>

    <!-- Mobile top bar -->
    <div class="fixed top-0 left-0 right-0 z-20 flex items-center gap-3 px-4 py-3 lg:hidden"
      :style="{ backgroundColor: 'var(--color-surface)', borderBottom: '1px solid var(--color-border)' }">
      <button @click="sidebarOpen = true" class="p-1 -ml-1 rounded-lg transition-colors duration-150"
        style="color: var(--color-text-secondary)"
        @mouseenter="$event.target.style.color = 'var(--color-text)'"
        @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      </button>
      <h1 class="font-display text-lg font-bold" style="color: var(--color-accent)">MindVault</h1>
      <div class="ml-auto flex items-center gap-2">
        <button v-if="!chatOpen" @click="chatOpen = true"
          class="p-1.5 rounded-lg transition-colors duration-150"
          style="color: var(--color-text-secondary)"
          @mouseenter="$event.target.style.color = 'var(--color-accent)'"
          @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
          </svg>
        </button>
        <button v-else @click="chatOpen = false"
          class="p-1.5 rounded-lg transition-colors duration-150"
          style="color: var(--color-accent)">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Main content -->
    <main class="flex-1 flex overflow-hidden pt-[57px] lg:pt-0">
      <div class="flex-1 flex flex-col overflow-hidden">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>

      <!-- Chat panel -->
      <transition name="panel-transition">
        <div v-if="chatOpen" class="chat-panel-wrapper">
          <ChatPanel @close="chatOpen = false" />
        </div>
      </transition>
    </main>

    <!-- Desktop chat FAB (outside main to avoid overflow clipping) -->
    <button v-if="!chatOpen && isDesktop"
      @click="chatOpen = true"
      class="fixed bottom-6 right-6 w-12 h-12 flex items-center justify-center rounded-full shadow-lg z-50 transition-all duration-200 hover:scale-105 hover:shadow-xl"
      :style="{ backgroundColor: 'var(--color-accent)', color: 'white' }">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'

const chatOpen = ref(true)
const sidebarOpen = ref(false)
const isDesktop = ref(true)

function checkScreen() {
  isDesktop.value = window.innerWidth >= 1024
  if (isDesktop.value) sidebarOpen.value = true
  else sidebarOpen.value = false
}

onMounted(() => {
  checkScreen()
  window.addEventListener('resize', checkScreen)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreen)
})
</script>

<style scoped>
.sidebar-drawer {
  @apply fixed left-0 top-0 z-40 h-full lg:static lg:z-auto;
  transition: transform 0.3s ease;
}

.sidebar-slide-enter-from,
.sidebar-slide-leave-to {
  transform: translateX(-100%);
}
.sidebar-slide-enter-active,
.sidebar-slide-leave-active {
  transition: transform 0.3s ease;
}

.chat-panel-wrapper {
  @apply w-full lg:w-96 shrink-0;
}
</style>