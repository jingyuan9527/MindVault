<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6">
    <div class="flex items-center gap-3 mb-4 md:mb-6">
      <div class="monitor-header-icon">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
        </svg>
      </div>
      <div>
        <h2 class="font-display text-xl md:text-2xl" style="color: var(--color-text)">系统监控</h2>
        <p class="text-xs" style="color: var(--color-text-secondary)">JVM、内存、磁盘与数据库状态</p>
      </div>
    </div>

    <n-spin v-if="loading" class="flex justify-center py-12" />

    <div v-else class="space-y-4">
      <div class="monitor-grid">
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(93, 122, 90, 0.12)">
            <div class="w-2 h-2 rounded-full" :class="health.status === 'UP' ? 'bg-green-500' : 'bg-red-500'"></div>
          </div>
          <div>
            <p class="monitor-stat-label">运行状态</p>
            <p class="monitor-stat-value" :class="health.status === 'UP' ? 'text-green-600' : 'text-red-600'">{{ health.status }}</p>
          </div>
        </div>
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(198, 95, 57, 0.12)">
            <svg class="w-5 h-5" style="color: var(--color-accent)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
          </div>
          <div>
            <p class="monitor-stat-label">运行时间</p>
            <p class="monitor-stat-value" style="color: var(--color-text)">{{ formatUptime(health.uptime) }}</p>
          </div>
        </div>
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(93, 122, 90, 0.12)">
            <svg class="w-5 h-5" style="color: var(--color-sage)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4"/></svg>
          </div>
          <div>
            <p class="monitor-stat-label">JVM 版本</p>
            <p class="monitor-stat-value" style="color: var(--color-text)">{{ info.javaVersion }}</p>
          </div>
        </div>
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(198, 95, 57, 0.12)">
            <svg class="w-5 h-5" style="color: var(--color-accent)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"/></svg>
          </div>
          <div>
            <p class="monitor-stat-label">CPU 核心</p>
            <p class="monitor-stat-value" style="color: var(--color-text)">{{ info.availableProcessors }}</p>
          </div>
        </div>
      </div>

      <div class="monitor-card-grid">
        <div class="monitor-card">
          <h3 class="monitor-card-title">JVM 内存</h3>
          <div class="monitor-memory-bar">
            <div class="memory-bar-fill" :style="{ width: memoryPct + '%', backgroundColor: memoryColor }"></div>
          </div>
          <div class="monitor-memory-items">
            <div class="memory-item"><span>堆已用</span><span class="font-medium">{{ info.heapUsed }}</span></div>
            <div class="memory-item"><span>空闲</span><span>{{ info.freeMemory }}</span></div>
            <div class="memory-item"><span>已分配</span><span>{{ info.totalMemory }}</span></div>
            <div class="memory-item"><span>最大</span><span>{{ info.maxMemory }}</span></div>
          </div>
        </div>
        <div class="monitor-card">
          <h3 class="monitor-card-title">磁盘</h3>
          <div class="monitor-memory-bar">
            <div class="memory-bar-fill" :style="{ width: diskUsedPct + '%', backgroundColor: diskColor }"></div>
          </div>
          <div class="monitor-memory-items">
            <div class="memory-item"><span>空闲</span><span class="font-medium">{{ health.checks?.disk?.free }}</span></div>
            <div class="memory-item"><span>总量</span><span>{{ health.checks?.disk?.total }}</span></div>
            <div class="memory-item"><span>已用比例</span><span>{{ diskUsedPct }}%</span></div>
          </div>
        </div>
      </div>

      <div class="monitor-grid">
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(93, 122, 90, 0.12)">
            <svg class="w-5 h-5" style="color: var(--color-sage)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z"/></svg>
          </div>
          <div>
            <p class="monitor-stat-label">线程数</p>
            <p class="monitor-stat-value" style="color: var(--color-text)">{{ metrics['jvm.threads.live'] || 0 }}</p>
          </div>
        </div>
        <div class="monitor-stat">
          <div class="monitor-stat-icon" style="background-color: rgba(198, 95, 57, 0.12)">
            <svg class="w-5 h-5" style="color: var(--color-accent)" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"/></svg>
          </div>
          <div>
            <p class="monitor-stat-label">活跃连接</p>
            <p class="monitor-stat-value" style="color: var(--color-text)">{{ metrics.activeConnections || 0 }}</p>
          </div>
        </div>
        <div class="monitor-stat">
          <div class="monitor-stat-icon" :style="{ backgroundColor: dbStatusColor }">
            <div class="w-2 h-2 rounded-full" :class="health.checks?.database?.status === 'UP' ? 'bg-green-500' : 'bg-red-500'"></div>
          </div>
          <div>
            <p class="monitor-stat-label">数据库</p>
            <p class="monitor-stat-value" :class="health.checks?.database?.status === 'UP' ? 'text-green-600' : 'text-red-600'">{{ health.checks?.database?.status || 'N/A' }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { systemApi } from '@/api/system'

const loading = ref(true)
const health = ref({})
const info = ref({})
const metrics = ref({})

const memoryPct = computed(() => {
  if (!info.value.maxMemory || !info.value.heapUsed) return 0
  const used = parseInt(info.value.heapUsed) || 0
  const max = parseInt(info.value.maxMemory) || 1
  return Math.round((used / max) * 100)
})
const memoryColor = computed(() => memoryPct.value > 80 ? 'var(--color-danger)' : memoryPct.value > 60 ? 'var(--color-accent)' : 'var(--color-sage)')
const diskUsedPct = computed(() => {
  const freePct = parseFloat(health.value?.checks?.disk?.freePercent) || 0
  return Math.round(100 - freePct)
})
const diskColor = computed(() => diskUsedPct.value > 80 ? 'var(--color-danger)' : diskUsedPct.value > 60 ? 'var(--color-accent)' : 'var(--color-sage)')
const dbStatusColor = computed(() => health.value.checks?.database?.status === 'UP' ? 'rgba(93, 122, 90, 0.12)' : 'rgba(239, 68, 68, 0.12)')

function formatUptime(seconds) {
  if (!seconds) return '-'
  const d = Math.floor(seconds / 86400); const h = Math.floor((seconds % 86400) / 3600); const m = Math.floor((seconds % 3600) / 60)
  const parts = []; if (d) parts.push(d + '天'); if (h) parts.push(h + '时'); parts.push(m + '分'); return parts.join(' ')
}

async function loadAll() { try { const [hRes, iRes, mRes] = await Promise.all([systemApi.health(), systemApi.info(), systemApi.metrics()]); health.value = hRes.data || {}; info.value = iRes.data?.data || {}; metrics.value = mRes.data?.data || {} } catch {}; loading.value = false }

onMounted(loadAll)
</script>

<style scoped>
.monitor-header-icon { width: 40px; height: 40px; border-radius: 12px; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); }
.monitor-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.monitor-card-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.monitor-stat { display: flex; align-items: center; gap: 12px; padding: 16px; border-radius: 12px; background-color: var(--color-surface); border: 1px solid var(--color-border); }
.monitor-stat-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.monitor-stat-label { font-size: 0.7rem; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.04em; }
.monitor-stat-value { font-size: 0.95rem; font-weight: 600; margin-top: 1px; }
.monitor-card { padding: 20px; border-radius: 14px; background-color: var(--color-surface); border: 1px solid var(--color-border); }
.monitor-card-title { font-size: 0.8rem; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 12px; }
.monitor-memory-bar { height: 8px; border-radius: 999px; background-color: var(--color-border); overflow: hidden; margin-bottom: 14px; }
.memory-bar-fill { height: 100%; border-radius: 999px; transition: all 0.5s ease; }
.monitor-memory-items { display: flex; flex-direction: column; gap: 6px; }
.memory-item { display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--color-warm-gray); }
@media (max-width: 768px) { .monitor-grid { grid-template-columns: repeat(2, 1fr); } .monitor-card-grid { grid-template-columns: 1fr; } }
</style>