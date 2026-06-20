<template>
  <n-config-provider :theme="themeStore.naiveTheme" :locale="zhCN" :date-locale="dateZhCN"
    :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
      <router-view v-slot="{ Component, route }">
        <template v-if="route.path === '/login'">
          <component :is="Component" />
        </template>
        <template v-else>
          <n-layout position="absolute" style="height: 100vh; --n-color: transparent">
            <n-layout has-sider style="height: 100%; --n-color: transparent">
              <n-layout-sider
                :width="260"
                :collapsed-width="0"
                :collapsed="!sidebarOpen"
                collapse-mode="transform"
                :native-scrollbar="false"
                bordered
                class="sidebar-sider"
                @update:collapsed="sidebarOpen = !sidebarOpen"
              >
                <AppSidebar @close="sidebarOpen = false" />
              </n-layout-sider>

              <n-layout style="height: 100%; --n-color: transparent">
                <!-- Mobile top bar -->
                <n-layout-header bordered class="lg:hidden mobile-header">
                  <div class="flex items-center gap-3 px-4 py-3">
                    <n-button text @click="sidebarOpen = true" class="!text-secondary">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                      </svg>
                    </n-button>
                    <h1 class="font-display text-lg font-bold" style="color: #4D6A4A">MindVault</h1>
                    <div class="ml-auto flex items-center gap-2">
                      <n-button text @click="chatOpen = !chatOpen" class="!text-secondary">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                        </svg>
                      </n-button>
                      <n-button text @click="handleLogout" class="!text-secondary" title="退出登录">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                        </svg>
                      </n-button>
                    </div>
                  </div>
                </n-layout-header>

                <!-- Main content -->
                <n-layout-content content-style="display: flex; flex-direction: column; height: 100%" style="--n-color: transparent">
                  <div class="flex flex-1 overflow-hidden">
                    <div class="flex-1 flex flex-col overflow-hidden" style="background-color: var(--color-bg)">
                      <transition name="fade" mode="out-in">
                        <component :is="Component" />
                      </transition>
                    </div>

                    <!-- Chat panel -->
                    <transition name="panel-transition">
                      <div v-if="chatOpen" class="chat-panel-wrapper">
                        <ChatPanel @close="chatOpen = false" />
                      </div>
                    </transition>
                  </div>
                </n-layout-content>

                <!-- Desktop chat FAB -->
                <n-button v-if="!chatOpen && isDesktop"
                  circle
                  :style="{ backgroundColor: 'var(--color-accent)' }"
                  class="!fixed bottom-6 right-6 z-50 shadow-lg"
                  @click="chatOpen = true"
                >
                  <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                  </svg>
                </n-button>
              </n-layout>
            </n-layout>
          </n-layout>
        </template>
      </router-view>
    </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { zhCN, dateZhCN } from 'naive-ui'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ChatPanel from '@/components/chat/ChatPanel.vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const chatOpen = ref(false)
const sidebarOpen = ref(false)
const isDesktop = ref(true)

const themeOverrides = computed(() => {
  if (themeStore.isDark) {
    return {
      common: {
        primaryColor: '#7a9a77',
        primaryColorHover: '#8aaa87',
        primaryColorPressed: '#6a8a67',
        primaryColorSuppl: '#8aaa87',
      },
      Menu: {
        itemColorActive: '#2a3a28',
        itemTextColorActive: '#7a9a77',
        itemIconColorActive: '#7a9a77',
      },
    }
  }
  return {
    common: {
      primaryColor: '#5d7a5a',
      primaryColorHover: '#4d6a4a',
      primaryColorPressed: '#3d5a3a',
      primaryColorSuppl: '#6d8a6a',
    },
    Menu: {
      itemColorActive: '#e8f0e6',
      itemTextColorActive: '#5d7a5a',
      itemIconColorActive: '#5d7a5a',
    },
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function checkScreen() {
  isDesktop.value = window.innerWidth >= 1024
  sidebarOpen.value = isDesktop.value
}

onMounted(() => {
  themeStore.init()
  checkScreen()
  window.addEventListener('resize', checkScreen)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreen)
})
</script>

<style scoped>
.sidebar-sider {
  --n-color: var(--color-surface);
}
.mobile-header {
  --n-color: var(--color-surface);
}
.chat-panel-wrapper {
  @apply w-full lg:w-96 shrink-0;
}
.panel-transition-enter-from,
.panel-transition-leave-to {
  transform: translateX(100%);
  opacity: 0;
}
.panel-transition-enter-active,
.panel-transition-leave-active {
  transition: all 0.3s ease;
}
</style>
