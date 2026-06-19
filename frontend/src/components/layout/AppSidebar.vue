<template>
  <aside class="w-72 flex flex-col h-screen shrink-0"
    :style="{ backgroundColor: 'var(--color-surface)', borderRight: '1px solid var(--color-border)' }">
    <div class="p-5 flex items-center justify-between" style="border-bottom: 1px solid var(--color-border)">
      <div>
        <h1 class="font-display text-2xl font-bold" style="color: var(--color-accent)">MindVault</h1>
        <p class="text-xs mt-1" style="color: var(--color-text-secondary)">知忆 · 你的AI增强第二大脑</p>
      </div>
      <button @click="$emit('close')"
        class="lg:hidden p-1.5 rounded-lg transition-colors duration-150"
        style="color: var(--color-text-secondary)"
        @mouseenter="$event.target.style.color = 'var(--color-text)'"
        @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <nav class="p-3 space-y-1 flex-1">
      <router-link to="/"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/' ? 'font-medium' : ''"
        :style="$route.path === '/' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/')"
        @mouseleave="unhoverNav($event, '/')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
        </svg>
        知识库
      </router-link>
      <router-link to="/chat"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/chat' ? 'font-medium' : ''"
        :style="$route.path === '/chat' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/chat')"
        @mouseleave="unhoverNav($event, '/chat')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"/>
        </svg>
        对话
      </router-link>
      <router-link to="/review"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/review' ? 'font-medium' : ''"
        :style="$route.path === '/review' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/review')"
        @mouseleave="unhoverNav($event, '/review')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        复习
        <span v-if="dueCount > 0"
          class="ml-auto px-1.5 py-0.5 text-xs font-medium rounded-full"
          :style="{ backgroundColor: 'var(--color-accent)', color: 'white' }">
          {{ dueCount }}
        </span>
      </router-link>
      <router-link to="/flashcards"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/flashcards' ? 'font-medium' : ''"
        :style="$route.path === '/flashcards' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/flashcards')"
        @mouseleave="unhoverNav($event, '/flashcards')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
        </svg>
        知识卡片
      </router-link>
      <router-link to="/writing"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/writing' ? 'font-medium' : ''"
        :style="$route.path === '/writing' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/writing')"
        @mouseleave="unhoverNav($event, '/writing')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
        </svg>
        写作辅助
      </router-link>
      <router-link to="/daily-review"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/daily-review' ? 'font-medium' : ''"
        :style="$route.path === '/daily-review' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/daily-review')"
        @mouseleave="unhoverNav($event, '/daily-review')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        每日复盘
      </router-link>
      <router-link to="/operation-logs"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/operation-logs' ? 'font-medium' : ''"
        :style="$route.path === '/operation-logs' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/operation-logs')"
        @mouseleave="unhoverNav($event, '/operation-logs')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        操作日志
      </router-link>
      <router-link to="/token-usage"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/token-usage' ? 'font-medium' : ''"
        :style="$route.path === '/token-usage' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/token-usage')"
        @mouseleave="unhoverNav($event, '/token-usage')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"/>
        </svg>
        用量统计
      </router-link>
      <router-link to="/backups"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/backups' ? 'font-medium' : ''"
        :style="$route.path === '/backups' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/backups')"
        @mouseleave="unhoverNav($event, '/backups')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        数据备份
      </router-link>
      <router-link to="/system"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/system' ? 'font-medium' : ''"
        :style="$route.path === '/system' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/system')"
        @mouseleave="unhoverNav($event, '/system')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"/>
        </svg>
        系统监控
      </router-link>
      <router-link to="/users"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/users' ? 'font-medium' : ''"
        :style="$route.path === '/users' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/users')"
        @mouseleave="unhoverNav($event, '/users')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"/>
        </svg>
        用户管理
      </router-link>
      <router-link to="/settings"
        class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-150"
        :class="$route.path === '/settings' ? 'font-medium' : ''"
        :style="$route.path === '/settings' ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
        @mouseenter="hoverNav($event, '/settings')"
        @mouseleave="unhoverNav($event, '/settings')">
        <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        </svg>
        设置
      </router-link>
    </nav>

    <div class="p-3" style="border-top: 1px solid var(--color-border)">
      <p class="text-xs font-medium mb-2 px-2" style="color: var(--color-text-secondary)">标签云</p>
      <div class="flex flex-wrap gap-1.5 px-2 max-h-32 overflow-y-auto">
        <router-link v-for="tag in tags" :key="tag.name" :to="{ path: '/', query: { tag: tag.name } }"
          class="px-2 py-0.5 rounded text-xs transition-all duration-150 whitespace-nowrap hover:opacity-80"
          :style="{
            backgroundColor: 'var(--color-sage-light)',
            color: 'var(--color-sage)',
            fontSize: Math.min(0.75 + tag.count * 0.04, 1) + 'rem'
          }">
          {{ tag.name }}
          <span class="ml-0.5 opacity-60">({{ tag.count }})</span>
        </router-link>
        <p v-if="!tags.length" class="text-xs px-2" style="color: var(--color-text-secondary)">暂无标签</p>
      </div>
    </div>

    <div class="p-4 pt-2 flex items-center justify-between" style="border-top: 1px solid var(--color-border)">
      <p class="text-xs" style="color: var(--color-text-secondary)">v0.3.0 · 智能增强版</p>
      <button @click="themeStore.toggle()"
        class="p-1.5 rounded-lg transition-colors duration-150"
        style="color: var(--color-text-secondary)"
        @mouseenter="$event.target.style.color = 'var(--color-text)'"
        @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'"
        :title="themeStore.isDark ? '切换亮色模式' : '切换暗色模式'">
        <svg v-if="themeStore.isDark" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z"/>
        </svg>
        <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z"/>
        </svg>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { reviewApi } from '@/api/review'
import { knowledgeApi } from '@/api/knowledge'

defineEmits(['close'])

const themeStore = useThemeStore()
const dueCount = ref(0)
const tags = ref([])
let pollTimer = null

function hoverNav(e, path) {
  const el = e.currentTarget
  if (el.classList.contains('font-medium')) return
  el.style.backgroundColor = 'rgba(45,42,36,0.03)'
}

function unhoverNav(e, path) {
  const el = e.currentTarget
  if (el.classList.contains('font-medium')) return
  el.style.backgroundColor = 'transparent'
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
  loadDueCount()
  loadTags()
  pollTimer = setInterval(loadDueCount, 60000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>