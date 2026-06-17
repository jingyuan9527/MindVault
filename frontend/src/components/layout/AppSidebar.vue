<template>
  <aside class="w-72 flex flex-col h-screen shrink-0"
    :style="{ backgroundColor: 'var(--color-surface)', borderRight: '1px solid var(--color-border)' }">
    <div class="p-5" style="border-bottom: 1px solid var(--color-border)">
      <h1 class="font-display text-2xl font-bold" style="color: var(--color-accent)">MindVault</h1>
      <p class="text-xs mt-1" style="color: var(--color-text-secondary)">知忆 · 你的AI增强第二大脑</p>
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

    <div class="p-4" style="border-top: 1px solid var(--color-border)">
      <p class="text-xs" style="color: var(--color-text-secondary)">v0.3.0 · 智能增强版</p>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { reviewApi } from '@/api/review'

const dueCount = ref(0)
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

onMounted(() => {
  loadDueCount()
  pollTimer = setInterval(loadDueCount, 60000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>