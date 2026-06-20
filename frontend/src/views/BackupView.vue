<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">数据备份</h2>

    <section class="card p-4 md:p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-base md:text-lg" style="color: var(--color-text)">自动备份</h3>
        <button @click="createBackup" :disabled="backingUp" class="btn-primary text-sm">
          {{ backingUp ? '备份中...' : '立即备份' }}
        </button>
      </div>
      <p class="text-xs mb-4" style="color: var(--color-text-secondary)">数据库备份将在每天 3:00 自动执行，保留最近 7 天的备份</p>
      <div v-if="backups.length" class="space-y-2">
        <div v-for="bak in backups" :key="bak"
          class="flex items-center justify-between px-4 py-2.5 rounded-lg transition-colors duration-150 hover-sage-bg"
          style="background-color: var(--color-bg)">
          <span class="text-sm" style="color: var(--color-text)">{{ bak }}</span>
          <button @click="downloadBackup(bak)"
            class="text-xs px-3 py-1 rounded transition-colors duration-150 hover-accent-bg"
            style="color: var(--color-accent)">
            下载
          </button>
        </div>
      </div>
      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无备份</p>
    </section>
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
