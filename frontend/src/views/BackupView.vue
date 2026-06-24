<template>
  <div class="flex-1 overflow-y-auto p-4 md:p-6">
    <div class="flex items-center gap-3 mb-4 md:mb-6">
      <div class="backup-header-icon">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
      </div>
      <div>
        <h2 class="font-display text-xl md:text-2xl" style="color: var(--color-text)">数据备份</h2>
        <p class="text-xs" style="color: var(--color-text-secondary)">数据库备份与恢复</p>
      </div>
    </div>

    <div class="backup-card mb-4">
      <div class="backup-card-header">
        <div>
          <h3 class="font-display text-base md:text-lg">自动备份</h3>
          <p class="text-xs mt-1" style="color: var(--color-text-secondary)">数据库备份将在每天 3:00 自动执行，保留最近 7 天的备份</p>
        </div>
        <n-button type="primary" :loading="backingUp" @click="createBackup" class="backup-btn">
          <template #icon>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
          </template>
          立即备份
        </n-button>
      </div>

      <div v-if="backups.length" class="backup-timeline">
        <div v-for="(bak, idx) in backups" :key="bak" class="backup-timeline-item">
          <div class="timeline-dot" :class="{ first: idx === 0 }"></div>
          <div class="timeline-content">
            <div class="timeline-name">{{ bak }}</div>
            <n-button text type="primary" size="tiny" @click="downloadBackup(bak)">下载</n-button>
          </div>
        </div>
      </div>
      <n-empty v-else description="暂无备份" class="py-8" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { backupApi } from '@/api/backup'

const backups = ref([])
const backingUp = ref(false)

async function loadBackups() { try { const res = await backupApi.list(); backups.value = res.data.data || [] } catch {} }
async function createBackup() { backingUp.value = true; try { await backupApi.create(); await loadBackups() } finally { backingUp.value = false } }
async function downloadBackup(filename) { try { const res = await backupApi.download(filename); const blob = new Blob([res.data], { type: 'application/json' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = filename; a.click(); URL.revokeObjectURL(url) } catch (err) { console.error('下载备份失败:', err) } }

onMounted(loadBackups)
</script>

<style scoped>
.backup-header-icon {
  width: 40px; height: 40px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}
.backup-btn { --n-color: #4f46e5; --n-color-hover: #3730a3; --n-color-pressed: #312e81; }
.backup-card { padding: 20px; border-radius: 14px; background-color: var(--color-surface); border: 1px solid var(--color-border); }
.backup-card-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.backup-timeline { position: relative; padding-left: 20px; }
.backup-timeline::before { content: ''; position: absolute; left: 7px; top: 8px; bottom: 8px; width: 2px; background-color: var(--color-border); }
.backup-timeline-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; position: relative; }
.timeline-dot { width: 16px; height: 16px; border-radius: 50%; border: 2px solid var(--color-border); background-color: var(--color-surface); flex-shrink: 0; margin-left: -20px; }
.timeline-dot.first { border-color: var(--color-sage); background-color: var(--color-sage-light); }
.timeline-content { flex: 1; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.timeline-name { font-size: 0.85rem; color: var(--color-text); font-family: monospace; }
</style>