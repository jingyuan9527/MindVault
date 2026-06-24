<template>
  <div class="flex flex-col h-full sidebar-inner">
    <!-- Header -->
    <div class="px-5 py-5 flex items-center justify-between shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div>
        <h1 class="font-display text-xl font-bold sidebar-brand">MindVault</h1>
        <p class="text-xs mt-0.5" style="color: var(--color-text-secondary)">知忆 · AI增强第二大脑</p>
      </div>
      <n-button text class="lg:hidden !text-secondary" @click="$emit('close')" aria-label="关闭菜单">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </n-button>
    </div>

    <!-- Navigation -->
    <n-menu
      :options="menuOptions"
      :value="activeKey"
      :collapsed="false"
      :collapsed-width="0"
      root-indent="12"
      class="sidebar-menu shrink-0"
      @update:value="handleMenuSelect"
    />

    <!-- Tag cloud -->
    <div class="px-4 py-3 shrink-0" style="border-top: 1px solid var(--color-border)">
      <p class="text-xs font-medium mb-2 px-1" style="color: var(--color-text-secondary)">标签云</p>
      <div class="flex flex-wrap gap-1.5 max-h-28 overflow-y-auto px-1">
        <router-link v-for="tag in tags" :key="tag.name"
          :to="{ path: '/', query: { tag: tag.name } }"
          class="px-2 py-0.5 rounded-full text-xs transition-all duration-150 whitespace-nowrap"
          :style="{
            background: 'var(--color-accent-light)',
            color: 'var(--color-accent)',
            fontSize: Math.min(0.75 + tag.count * 0.04, 1) + 'rem',
            backdropFilter: 'blur(8px)'
          }">
          {{ tag.name }}
          <span class="ml-0.5 opacity-60">({{ tag.count }})</span>
        </router-link>
        <p v-if="!tags.length" class="text-xs px-1" style="color: var(--color-text-secondary)">暂无标签</p>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-auto px-4 py-3 shrink-0" style="border-top: 1px solid var(--color-border)">
      <div class="flex items-center justify-center gap-2 mb-2">
        <button v-for="t in THEMES" :key="t.id"
          class="theme-dot"
          :class="{ active: themeStore.currentTheme === t.id }"
          :style="{ '--dot-clr': dotColors[t.id] }"
          @click="themeStore.setTheme(t.id)"
          :title="t.label"
        />
      </div>
      <div class="flex items-center justify-between">
        <span class="text-xs" style="color: var(--color-text-secondary)">v0.5.1</span>
        <div class="flex items-center gap-1">
          <n-button text class="!text-secondary" title="切换亮度" @click="themeStore.toggleDark()">
            <svg v-if="themeStore.isDark" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
            <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
            </svg>
          </n-button>
          <n-button text class="!text-secondary" title="退出登录" @click="handleLogout">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NIcon, NBadge } from 'naive-ui'
import {
  LibraryOutline, ChatboxOutline, CheckmarkCircleOutline,
  CardOutline, CreateOutline, AnalyticsOutline,
  DocumentTextOutline, BarChartOutline, CloudDownloadOutline,
  DesktopOutline, PeopleOutline, SettingsOutline,
  CogOutline
} from '@vicons/ionicons5'
import { useThemeStore, THEMES } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import { reviewApi } from '@/api/review'
import { knowledgeApi } from '@/api/knowledge'

defineEmits(['close'])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const dueCount = ref(0)
const tags = ref([])
let pollTimer = null

const dotColors = {
  'amber-earth': '#D4856A',
  'lavender-calm': '#A855F7',
  'violet-glass': '#8B5CF6',
}

function renderIcon(icon) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

function renderLabel(label, badge) {
  if (!badge) return label
  return () => h('div', { class: 'flex items-center justify-between w-full' }, [
    h('span', label),
    h(NBadge, { value: badge, size: 'small' })
  ])
}

const menuOptions = [
  { label: '知识库', key: '/', icon: renderIcon(LibraryOutline) },
  { label: '对话', key: '/chat', icon: renderIcon(ChatboxOutline) },
  {
    label: () => h('div', { class: 'flex items-center justify-between w-full gap-2' }, [
      h('span', '复习'),
      dueCount.value > 0
        ? h(NBadge, { value: dueCount.value, size: 'small', 'max': 99 })
        : null
    ]),
    key: '/review',
    icon: renderIcon(CheckmarkCircleOutline)
  },
  { label: '知识卡片', key: '/flashcards', icon: renderIcon(CardOutline) },
  { label: '写作辅助', key: '/writing', icon: renderIcon(CreateOutline) },
  { label: '每日复盘', key: '/daily-review', icon: renderIcon(AnalyticsOutline) },
  { label: '操作日志', key: '/operation-logs', icon: renderIcon(DocumentTextOutline) },
  { label: '用量统计', key: '/token-usage', icon: renderIcon(BarChartOutline) },
  { label: '数据备份', key: '/backups', icon: renderIcon(CloudDownloadOutline) },
  { label: '系统监控', key: '/system', icon: renderIcon(DesktopOutline) },
  { label: '用户管理', key: '/users', icon: renderIcon(PeopleOutline) },
  { label: '系统配置', key: '/system-config', icon: renderIcon(CogOutline) },
  { label: '设置', key: '/settings', icon: renderIcon(SettingsOutline) },
]

const activeKey = computed(() => route.path)

function handleMenuSelect(key) {
  router.push(key)
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

async function loadTags() {
  try {
    const res = await knowledgeApi.getTags()
    tags.value = res.data.data || []
  } catch {}
}

onMounted(() => {
  if (!authStore.isLoggedIn()) return
  loadDueCount()
  loadTags()
  pollTimer = setInterval(loadDueCount, 60000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.sidebar-inner {
  position: relative;
  z-index: 1;
}

.sidebar-brand {
  background: var(--gradient-brand);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-menu {
  --n-item-color: var(--color-text-secondary);
  --n-item-color-hover: var(--color-accent);
  --n-item-color-active: var(--color-accent);
  --n-item-text-color: var(--color-text-secondary);
  --n-item-text-color-hover: var(--color-accent);
  --n-item-text-color-active: var(--color-accent);
  --n-arrow-color: var(--color-text-secondary);
  --n-item-font-size: 14px;
}

.theme-dot {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: var(--dot-clr);
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0.5;
}
.theme-dot:hover {
  opacity: 0.8;
  transform: scale(1.15);
}
.theme-dot.active {
  opacity: 1;
  border-color: var(--dot-clr);
  box-shadow: 0 0 12px var(--dot-clr);
  transform: scale(1.1);
}
</style>