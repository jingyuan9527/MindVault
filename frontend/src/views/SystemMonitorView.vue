<template>
  <div class="p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">系统监控</h2>

    <div v-if="loading" class="text-sm py-8 text-center" style="color: var(--color-text-secondary)">加载中...</div>
    <template v-else>
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">运行状态</p>
          <p class="text-lg font-semibold" :style="{ color: health.status === 'UP' ? 'var(--color-sage)' : 'var(--color-accent)' }">{{ health.status }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">运行时间</p>
          <p class="text-lg font-semibold" style="color: var(--color-text)">{{ formatUptime(health.uptime) }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">JVM 版本</p>
          <p class="text-lg font-semibold" style="color: var(--color-text)">{{ info.javaVersion }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">CPU 核心</p>
          <p class="text-lg font-semibold" style="color: var(--color-text)">{{ info.availableProcessors }}</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
        <div class="card p-4">
          <h3 class="font-display text-sm mb-3" style="color: var(--color-text)">JVM 内存</h3>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">堆已用</span>
              <span style="color: var(--color-text)">{{ info.heapUsed }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">空闲</span>
              <span style="color: var(--color-text)">{{ info.freeMemory }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">已分配</span>
              <span style="color: var(--color-text)">{{ info.totalMemory }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">最大</span>
              <span style="color: var(--color-text)">{{ info.maxMemory }}</span>
            </div>
          </div>
        </div>
        <div class="card p-4">
          <h3 class="font-display text-sm mb-3" style="color: var(--color-text)">磁盘</h3>
          <div class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">空闲</span>
              <span style="color: var(--color-text)">{{ health.checks?.disk?.free }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">总量</span>
              <span style="color: var(--color-text)">{{ health.checks?.disk?.total }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--color-text-secondary)">空闲比例</span>
              <span style="color: var(--color-text)">{{ health.checks?.disk?.freePercent }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">线程数</p>
          <p class="text-lg font-semibold" style="color: var(--color-text)">{{ metrics['jvm.threads.live'] }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">活跃连接</p>
          <p class="text-lg font-semibold" style="color: var(--color-text)">{{ metrics.activeConnections }}</p>
        </div>
        <div class="card p-4">
          <p class="text-xs mb-1" style="color: var(--color-text-secondary)">数据库</p>
          <p class="text-lg font-semibold" :style="{ color: health.checks?.database?.status === 'UP' ? 'var(--color-sage)' : 'var(--color-accent)' }">{{ health.checks?.database?.status }}</p>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'

const loading = ref(true)
const health = ref({})
const info = ref({})
const metrics = ref({})

function formatUptime(seconds) {
  if (!seconds) return '-'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const parts = []
  if (d) parts.push(d + '天')
  if (h) parts.push(h + '时')
  parts.push(m + '分')
  return parts.join(' ')
}

async function loadAll() {
  try {
    const [hRes, iRes, mRes] = await Promise.all([
      systemApi.health(),
      systemApi.info(),
      systemApi.metrics(),
    ])
    health.value = hRes.data || {}
    info.value = iRes.data?.data || {}
    metrics.value = mRes.data?.data || {}
  } catch {}
  loading.value = false
}

onMounted(loadAll)
</script>
