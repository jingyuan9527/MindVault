<template>
  <div class="app-container">
    <div class="bg-glow-1"></div>
    <div class="bg-glow-2"></div>
    <div class="bg-glow-3"></div>
    <div class="bg-orb"></div>
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
                <!-- Glass sidebar -->
                <div class="sidebar-glass-wrapper" :class="{ collapsed: !sidebarOpen }">
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
                </div>

                <n-layout style="height: 100%; --n-color: transparent">
                  <!-- Mobile top bar -->
                  <n-layout-header class="lg:hidden mobile-header">
                    <div class="flex items-center gap-3 px-4 py-3">
                      <n-button text @click="sidebarOpen = true" class="!text-secondary" aria-label="打开侧边栏">
                        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                        </svg>
                      </n-button>
                      <h1 class="font-display text-lg font-bold sidebar-brand">MindVault</h1>
                      <div class="ml-auto flex items-center gap-2">
                        <n-button text @click="chatOpen = !chatOpen" class="!text-secondary" aria-label="打开聊天">
                          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                          </svg>
                        </n-button>
                        <n-button text @click="handleLogout" class="!text-secondary" title="退出登录">
                          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                          </svg>
                        </n-button>
                      </div>
                    </div>
                  </n-layout-header>

                  <!-- Main content -->
                  <n-layout-content content-style="display: flex; flex-direction: column; height: 100%" style="--n-color: transparent">
                    <div class="flex flex-1 overflow-hidden">
                      <div class="flex-1 flex flex-col overflow-hidden">
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
                    class="chat-fab"
                    @click="chatOpen = true"
                  >
                    <template #icon>
<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                          </svg>
                    </template>
                  </n-button>
                </n-layout>
              </n-layout>
            </n-layout>
          </template>
        </router-view>
      </n-dialog-provider>
      </n-message-provider>
    </n-config-provider>
  </div>
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

function themeColors(theme, dark) {
  const c = (t) => {
    if (t === 'violet-glass') return dark
      ? { p: '#8B5CF6', ph: '#A78BFA', pp: '#7C3AED', al: 'rgba(139,92,246,0.15)', s: '#06B6D4' }
      : { p: '#7C3AED', ph: '#8B5CF6', pp: '#6D28D9', al: 'rgba(124,58,237,0.1)', s: '#0891B2' }
    if (t === 'lavender-calm') return dark
      ? { p: '#A855F7', ph: '#C084FC', pp: '#9333EA', al: 'rgba(168,85,247,0.15)', s: '#34D399' }
      : { p: '#9333EA', ph: '#A855F7', pp: '#7E22CE', al: 'rgba(147,51,234,0.1)', s: '#10B981' }
    return dark
      ? { p: '#D4856A', ph: '#E8A87C', pp: '#C67B5C', al: 'rgba(212,133,106,0.15)', s: '#8B9C68' }
      : { p: '#C67B5C', ph: '#D4856A', pp: '#B5651D', al: 'rgba(198,123,92,0.1)', s: '#6B7B3C' }
  }
  const tc = c(theme)
  if (dark) {
    return {
      common: {
        primaryColor: tc.p, primaryColorHover: tc.ph, primaryColorPressed: tc.pp, primaryColorSuppl: tc.ph,
        bodyColor: 'transparent', cardColor: 'rgba(255,255,255,0.04)', modalColor: 'rgba(255,255,255,0.04)',
        popoverColor: 'rgba(15,23,42,0.9)', tableColor: 'rgba(255,255,255,0.03)',
        actionColor: 'rgba(255,255,255,0.02)', hoverColor: 'rgba(255,255,255,0.06)',
      },
      Menu: {
        itemColorActive: tc.al, itemTextColorActive: tc.ph, itemIconColorActive: tc.ph,
        itemColorHover: 'rgba(255,255,255,0.04)', itemTextColorHover: tc.ph, itemIconColorHover: tc.ph,
      },
      Tag: { colorPrimary: tc.p, textColorPrimary: '#ffffff', borderPrimary: `1px solid ${tc.p}4d` },
      Card: { color: 'rgba(255,255,255,0.04)', borderColor: 'rgba(255,255,255,0.08)', actionColor: 'rgba(255,255,255,0.02)' },
      Layout: { color: 'transparent', headerColor: 'transparent', siderColor: 'transparent', footerColor: 'transparent' },
      Button: { color: tc.al, colorHover: tc.p + '40', textColor: tc.ph, textColorHover: tc.ph },
      Input: {
        color: 'rgba(255,255,255,0.04)', colorFocus: 'rgba(255,255,255,0.06)',
        border: 'rgba(255,255,255,0.08)', borderFocus: tc.p,
        textColor: 'rgba(248,250,252,0.92)', placeholderColor: 'rgba(148,163,184,0.5)',
      },
      Dialog: { color: 'rgba(15,23,42,0.95)' },
    }
  }
  return {
    common: {
      primaryColor: tc.p, primaryColorHover: tc.ph, primaryColorPressed: tc.pp, primaryColorSuppl: tc.ph,
      bodyColor: 'transparent', cardColor: 'rgba(255,255,255,0.7)', modalColor: 'rgba(255,255,255,0.85)',
      popoverColor: 'rgba(255,255,255,0.95)', tableColor: 'rgba(255,255,255,0.5)',
      actionColor: 'rgba(0,0,0,0.02)', hoverColor: 'rgba(0,0,0,0.03)',
    },
    Menu: {
      itemColorActive: tc.al, itemTextColorActive: tc.p, itemIconColorActive: tc.p,
      itemColorHover: 'rgba(0,0,0,0.03)', itemTextColorHover: tc.p, itemIconColorHover: tc.p,
    },
    Tag: { colorPrimary: tc.p, textColorPrimary: '#ffffff', borderPrimary: `1px solid ${tc.p}4d` },
    Card: { color: 'rgba(255,255,255,0.7)', borderColor: 'rgba(0,0,0,0.06)', actionColor: 'rgba(0,0,0,0.02)' },
    Layout: { color: 'transparent', headerColor: 'transparent', siderColor: 'transparent', footerColor: 'transparent' },
    Button: { color: tc.al, colorHover: tc.p + '33', textColor: tc.p, textColorHover: tc.ph },
    Input: {
      color: 'rgba(255,255,255,0.5)', colorFocus: 'rgba(255,255,255,0.7)',
      border: 'rgba(0,0,0,0.08)', borderFocus: tc.p,
      textColor: '#0F172A', placeholderColor: 'rgba(71,85,105,0.5)',
    },
    Dialog: { color: 'rgba(255,255,255,0.95)' },
  }
}

const themeOverrides = computed(() => themeColors(themeStore.currentTheme, themeStore.isDark))

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
.app-container {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

/* Animated background glows */
.bg-glow-1 {
  position: fixed;
  top: -20%;
  left: -10%;
  width: 60%;
  height: 60%;
  background: radial-gradient(ellipse at center, var(--color-glow-1, rgba(139, 92, 246, 0.08)) 0%, transparent 70%);
  animation: bg-shift 20s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}
.bg-glow-2 {
  position: fixed;
  bottom: -15%;
  right: -5%;
  width: 50%;
  height: 50%;
  background: radial-gradient(ellipse at center, var(--color-glow-2, rgba(6, 182, 212, 0.06)) 0%, transparent 70%);
  animation: bg-shift 25s ease-in-out infinite reverse;
  pointer-events: none;
  z-index: 0;
}
.bg-glow-3 {
  position: fixed;
  top: 40%;
  left: 50%;
  width: 40%;
  height: 40%;
  background: radial-gradient(ellipse at center, var(--color-glow-3, rgba(245, 158, 11, 0.04)) 0%, transparent 70%);
  animation: bg-shift 30s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}
.bg-orb {
  position: fixed;
  top: 15%;
  right: 20%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle at center, var(--color-orb, rgba(139, 92, 246, 0.06)) 0%, transparent 60%);
  border-radius: 50%;
  animation: bg-orbit 40s linear infinite;
  pointer-events: none;
  z-index: 0;
}

.sidebar-glass-wrapper {
  position: relative;
  z-index: 1;
}
.sidebar-glass-wrapper::before {
  content: '';
  position: absolute;
  inset: 4px;
  border-radius: 16px;
  background: var(--color-glass);
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  border: 1px solid var(--color-glass-border);
  pointer-events: none;
}
.sidebar-glass-wrapper.collapsed::before {
  display: none;
}

.sidebar-sider {
  --n-color: transparent;
  position: relative;
  z-index: 1;
}

.mobile-header {
  --n-color: transparent;
  backdrop-filter: blur(20px) saturate(1.4);
  -webkit-backdrop-filter: blur(20px) saturate(1.4);
  background: var(--color-surface) !important;
  border-bottom: 1px solid var(--color-border) !important;
}

.chat-panel-wrapper {
  @apply w-full lg:w-96 shrink-0;
  position: relative;
  z-index: 1;
}

.chat-fab {
  position: fixed !important;
  bottom: 24px !important;
  right: 24px !important;
  z-index: 50;
  background: var(--gradient-brand, linear-gradient(135deg, #8B5CF6, #7C3AED)) !important;
  border: none !important;
  box-shadow: 0 4px 20px var(--color-accent-light) !important;
  color: white !important;
  transition: all 0.25s ease !important;
}
.chat-fab:hover {
  box-shadow: 0 8px 30px var(--color-accent-light) !important;
  transform: translateY(-2px) !important;
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

.sidebar-brand {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
</style>
