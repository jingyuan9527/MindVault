<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6 max-w-4xl">
    <n-space vertical size="large">
      <h2 class="font-display text-xl md:text-2xl" style="color: var(--color-text)">数据备份</h2>

      <n-card title="自动备份" size="small">
        <template #header-extra>
          <n-button type="primary" size="small" :loading="backingUp" @click="createBackup">
            <template #icon>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </template>
            立即备份
          </n-button>
        </template>

        <p class="text-xs mb-4" style="color: var(--color-text-secondary)">
          数据库备份将在每天 3:00 自动执行，保留最近 7 天的备份
        </p>

        <n-list v-if="backups.length">
          <n-list-item v-for="bak in backups" :key="bak">
            <div class="flex items-center justify-between w-full">
              <span class="text-sm" style="color: var(--color-text)">{{ bak }}</span>
              <n-button text type="primary" size="small" @click="downloadBackup(bak)">下载</n-button>
            </div>
          </n-list-item>
        </n-list>
        <n-empty v-else description="暂无备份" />
      </n-card>
    </n-space>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { backupApi } from '@/api/backup'

const backups = ref([])
const backingUp = ref(false)

async function loadBackups() {
  try {
    const res = await backupApi.list()
    backups.value = res.data.data || []
  } catch {}
}

async function createBackup() {
  backingUp.value = true
  try {
    await backupApi.create()
    await loadBackups()
  } finally {
    backingUp.value = false
  }
}

async function downloadBackup(filename) {
  try {
    const res = await backupApi.download(filename)
    const blob = new Blob([res.data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('下载备份失败:', err)
  }
}

onMounted(loadBackups)
</script>
