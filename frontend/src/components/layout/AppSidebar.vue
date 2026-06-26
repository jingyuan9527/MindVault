<template>
  <div class="flex flex-col h-full">
    <!-- Logo -->
    <div class="px-4 h-16 flex items-center shrink-0 border-bottom">
      <div>
        <h1 class="font-semibold text-base">MindVault</h1>
        <p class="text-xs" style="color: var(--color-text-secondary)">知忆</p>
      </div>
      <button v-if="!isDesktop" class="close-btn ml-auto" aria-label="关闭菜单" @click="$emit('close')">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <!-- Navigation groups -->
    <div class="flex-1 overflow-y-auto py-2">
      <p class="menu-group-label">📝 笔记</p>
      <router-link v-for="item in groupNotes" :key="item.key" :to="item.key"
        class="menu-item" :class="{ active: activeKey === item.key }"
        @click="onNav">
        <n-icon size="18"><component :is="item.icon" /></n-icon>
        <span>{{ item.label }}</span>
      </router-link>

      <div class="menu-divider"></div>

      <p class="menu-group-label">🧠 学习</p>
      <router-link v-for="item in groupLearning" :key="item.key" :to="item.key"
        class="menu-item" :class="{ active: activeKey === item.key }"
        @click="onNav">
        <n-icon size="18"><component :is="item.icon" /></n-icon>
        <span class="flex-1">{{ item.label }}</span>
        <n-badge v-if="item.key === '/review' && dueCount > 0" :value="dueCount" :max="99" size="small" />
      </router-link>

      <div class="menu-divider"></div>

      <p class="menu-group-label">⚙️ 管理</p>
      <router-link v-for="item in groupAdmin" :key="item.key" :to="item.key"
        class="menu-item" :class="{ active: activeKey === item.key }"
        @click="onNav">
        <n-icon size="18"><component :is="item.icon" /></n-icon>
        <span>{{ item.label }}</span>
      </router-link>
    </div>

    <!-- Footer -->
    <div class="shrink-0 px-4 py-3 border-top">
      <div class="flex items-center justify-between">
        <span class="text-xs" style="color: var(--color-text-secondary)">v0.5.1</span>
        <div class="flex items-center gap-1">
          <button class="footer-btn" aria-label="主题切换" @click="themeStore.toggleDark()">
            <svg v-if="themeStore.isDark" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          </button>
          <button class="footer-btn" aria-label="退出登录" @click="handleLogout">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NBadge } from 'naive-ui'
import {
  LibraryOutline, ChatboxOutline,
  CheckmarkCircleOutline, CardOutline, CreateOutline, AnalyticsOutline,
  PeopleOutline, DocumentTextOutline, BarChartOutline, CloudDownloadOutline,
  DesktopOutline, SettingsOutline
} from '@vicons/ionicons5'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { reviewApi } from '@/api/review'

defineEmits(['close'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const dueCount = ref(0)
const isDesktop = ref(true)
let pollTimer = null

const groupNotes = [
  { label: '知识库', key: '/', icon: LibraryOutline },
  { label: '对话', key: '/chat', icon: ChatboxOutline },
]

const groupLearning = [
  { label: '复习', key: '/review', icon: CheckmarkCircleOutline },
  { label: '闪卡', key: '/flashcards', icon: CardOutline },
  { label: '写作', key: '/writing', icon: CreateOutline },
  { label: '每日回顾', key: '/daily-review', icon: AnalyticsOutline },
]

const groupAdmin = [
  { label: '用户管理', key: '/users', icon: PeopleOutline },
  { label: '操作日志', key: '/operation-logs', icon: DocumentTextOutline },
  { label: 'Token 用量', key: '/token-usage', icon: BarChartOutline },
  { label: '数据备份', key: '/backups', icon: CloudDownloadOutline },
  { label: '系统监控', key: '/system', icon: DesktopOutline },
  { label: '设置', key: '/settings', icon: SettingsOutline },
]

const activeKey = computed(() => route.path)

function onNav() {
  if (!isDesktop.value) {
    // Will be handled by router-link navigation
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

async function loadDueCount() {
  try {
    const res = await reviewApi.getDueCount()
    dueCount.value = res.data.data?.count || 0
  } catch {}
}

function checkScreen() {
  isDesktop.value = window.innerWidth >= 1024
}

onMounted(() => {
  checkScreen()
  window.addEventListener('resize', checkScreen)
  if (authStore.isLoggedIn()) {
    loadDueCount()
    pollTimer = setInterval(loadDueCount, 60000)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreen)
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.border-bottom {
  border-bottom: 1px solid var(--color-border);
}
.border-top {
  border-top: 1px solid var(--color-border);
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.close-btn:hover {
  background: var(--color-surface);
}

.footer-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.footer-btn:hover {
  background: var(--color-surface);
  color: var(--color-text);
}
</style>