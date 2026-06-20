<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">系统监控</h2>

    <n-spin v-if="loading" class="flex justify-center py-12" />

    <n-space v-else vertical size="large">
      <n-grid :cols="4" :x-gap="12" :y-gap="12">
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="运行状态">
              <template #default>
                <n-tag :type="health.status === 'UP' ? 'success' : 'error'" :bordered="false">{{ health.status }}</n-tag>
              </template>
            </n-statistic>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="运行时间" :value="formatUptime(health.uptime)" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="JVM 版本" :value="info.javaVersion" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="CPU 核心" :value="info.availableProcessors" />
          </n-card>
        </n-gi>
      </n-grid>

      <n-grid :cols="2" :x-gap="12" :y-gap="12">
        <n-gi>
          <n-card size="small" title="JVM 内存">
            <div class="space-y-2 text-sm">
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">堆已用</span><span>{{ info.heapUsed }}</span></div>
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">空闲</span><span>{{ info.freeMemory }}</span></div>
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">已分配</span><span>{{ info.totalMemory }}</span></div>
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">最大</span><span>{{ info.maxMemory }}</span></div>
            </div>
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" title="磁盘">
            <div class="space-y-2 text-sm">
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">空闲</span><span>{{ health.checks?.disk?.free }}</span></div>
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">总量</span><span>{{ health.checks?.disk?.total }}</span></div>
              <div class="flex justify-between"><span style="color: var(--color-text-secondary)">空闲比例</span><span>{{ health.checks?.disk?.freePercent }}</span></div>
            </div>
          </n-card>
        </n-gi>
      </n-grid>

      <n-grid :cols="3" :x-gap="12" :y-gap="12">
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="线程数" :value="metrics['jvm.threads.live']" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="活跃连接" :value="metrics.activeConnections" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card size="small" class="stat-card">
            <n-statistic label="数据库">
              <template #default>
                <n-tag :type="health.checks?.database?.status === 'UP' ? 'success' : 'error'" :bordered="false">
                  {{ health.checks?.database?.status }}
                </n-tag>
              </template>
            </n-statistic>
          </n-card>
        </n-gi>
      </n-grid>
    </n-space>
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

<style scoped>
.stat-card {
  --n-padding-bottom: 16px;
}
</style>
