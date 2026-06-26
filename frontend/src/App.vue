<template>
  <n-config-provider
    :theme="themeStore.naiveTheme"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme-overrides="themeOverrides"
    :class="{ mobile: isMobile }"
  >
    <n-message-provider>
      <n-dialog-provider>
        <router-view v-slot="{ Component, route }">
          <template v-if="route.path === '/login'">
            <component :is="Component" />
          </template>
          <template v-else>
            <div class="layout-root">
              <!-- Mobile nav bar (lg- only) -->
              <header v-if="!isDesktop" class="mobile-header">
                <div class="flex items-center gap-3 px-4 h-full">
                  <button class="nav-icon-btn" aria-label="菜单" @click="drawerOpen = true">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                  </button>
                  <span class="font-semibold text-base">MindVault</span>
                  <div class="ml-auto flex items-center gap-2">
                    <button class="nav-icon-btn" aria-label="主题切换" @click="themeStore.toggleDark()">
                      <svg v-if="themeStore.isDark" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                      </svg>
                      <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                      </svg>
                    </button>
                    <button class="nav-icon-btn" aria-label="退出" @click="handleLogout">
                      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                      </svg>
                    </button>
                  </div>
                </div>
              </header>

              <!-- Desktop sidebar (xl+) -->
              <aside v-if="isDesktop" class="desktop-sidebar">
                <div class="sidebar-content">
                  <AppSidebar />
                </div>
              </aside>

              <!-- Drawer for mobile/tablet -->
              <n-drawer v-model:show="drawerOpen" :width="drawerWidth" placement="left" :auto-focus="false" @mask-click="drawerOpen = false">
                <n-drawer-content :native-scrollbar="false" :closable="false" body-style="padding:0">
                  <AppSidebar @close="drawerOpen = false" />
                </n-drawer-content>
              </n-drawer>

              <!-- Main content area -->
              <main class="main-content" :class="{ 'with-sidebar': isDesktop, 'with-header': !isDesktop }">
                <transition name="fade" mode="out-in">
                  <component :is="Component" />
                </transition>
              </main>
            </div>
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
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const drawerOpen = ref(false)
const isDesktop = ref(true)
const isMobile = ref(false)

const drawerWidth = computed(() => {
  return isMobile.value ? '80vw' : 280
})

const themeOverrides = computed(() => {
  const dark = themeStore.isDark
  const primary = '#2563eb'
  const primaryLight = dark ? '#1e3a5f' : '#eff6ff'
  const primaryHover = '#3b82f6'
  return {
    common: {
      primaryColor: primary,
      primaryColorHover: primaryHover,
      primaryColorPressed: '#1d4ed8',
      primaryColorSuppl: primaryHover,
      bodyColor: 'transparent',
      cardColor: dark ? '#1e293b' : '#f8fafc',
      modalColor: dark ? '#1e293b' : '#ffffff',
      popoverColor: dark ? '#1e293b' : '#ffffff',
      tableColor: dark ? '#1e293b' : '#f8fafc',
      actionColor: dark ? '#1e293b' : '#f8fafc',
      hoverColor: dark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.03)',
      dividerColor: dark ? '#334155' : '#e2e8f0',
      borderColor: dark ? '#334155' : '#e2e8f0',
      textColor1: dark ? '#f1f5f9' : '#1e293b',
      textColor2: dark ? '#94a3b8' : '#64748b',
      textColor3: dark ? '#94a3b8' : '#94a3b8',
      placeholderColor: dark ? '#94a3b8' : '#94a3b8',
      fontSize: '15px',
      borderRadius: '8px',
    },
    Card: {
      color: dark ? '#1e293b' : '#f8fafc',
      borderColor: 'transparent',
      borderRadius: '12px',
      titleFontSize: '15px',
    },
    Button: {
      borderRadius: '8px',
    },
    Input: {
      borderRadius: '8px',
      border: dark ? '#334155' : '#e2e8f0',
      borderFocus: primary,
      color: dark ? '#1e293b' : '#ffffff',
      colorFocus: dark ? '#1e293b' : '#ffffff',
      textColor: dark ? '#f1f5f9' : '#1e293b',
      placeholderColor: dark ? '#94a3b8' : '#94a3b8',
    },
    Layout: {
      color: 'transparent',
      headerColor: 'transparent',
      siderColor: 'transparent',
      footerColor: 'transparent',
    },
    Menu: {
      itemColorActive: primaryLight,
      itemTextColorActive: primary,
      itemIconColorActive: primary,
      itemColorHover: dark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.03)',
      itemTextColorHover: dark ? '#f1f5f9' : '#1e293b',
      itemIconColorHover: dark ? '#f1f5f9' : '#1e293b',
    },
    Tag: {
      colorPrimary: primary,
      textColorPrimary: '#ffffff',
      borderRadius: '4px',
    },
    Dialog: {
      color: dark ? '#1e293b' : '#ffffff',
    },
    Drawer: {
      color: dark ? '#0f172a' : '#ffffff',
    },
  }
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

function checkScreen() {
  isDesktop.value = window.innerWidth >= 1024
  isMobile.value = window.innerWidth < 768
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
.layout-root {
  position: relative;
  min-height: 100vh;
  display: flex;
  background: var(--color-bg);
}

.mobile-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  z-index: 100;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
}

.nav-icon-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.nav-icon-btn:hover {
  background: var(--color-surface);
  color: var(--color-text);
}

.desktop-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 240px;
  z-index: 50;
  border-right: 1px solid var(--color-border);
  background: var(--color-bg);
}

.sidebar-content {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
}
.main-content.with-sidebar {
  margin-left: 240px;
}
.main-content.with-header {
  padding-top: 60px;
}
</style>