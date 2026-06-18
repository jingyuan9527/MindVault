<template>
  <div class="p-4 md:p-6 max-w-4xl">
    <h2 class="font-display text-xl md:text-2xl mb-4 md:mb-6" style="color: var(--color-text)">系统设置</h2>

    <section class="card p-4 md:p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-base md:text-lg" style="color: var(--color-text)">模型配置</h3>
        <button @click="showAddForm = true" class="btn-primary text-sm">+ 添加模型</button>
      </div>

      <!-- Desktop table -->
      <div class="hidden md:block overflow-x-auto" v-if="models.length">
        <table class="w-full text-sm">
          <thead>
            <tr style="border-bottom: 1px solid var(--color-border); color: var(--color-text-secondary)">
              <th class="text-left py-2 font-medium">供应商</th>
              <th class="text-left py-2 font-medium">模型</th>
              <th class="text-left py-2 font-medium">类型</th>
              <th class="text-center py-2 font-medium w-20">优先级</th>
              <th class="text-center py-2 font-medium">状态</th>
              <th class="text-center py-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(m, idx) in models" :key="m.id" style="border-bottom: 1px solid var(--color-border)">
              <td class="py-3" style="color: var(--color-text)">{{ m.provider }}</td>
              <td class="py-3">
                <span style="color: var(--color-text)">{{ m.modelName }}</span>
                <span v-if="m.isPrimary" class="ml-2 px-1.5 py-0.5 text-xs rounded"
                  :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">主模型</span>
              </td>
              <td class="py-3" style="color: var(--color-warm-gray)">{{ m.modelType }}</td>
              <td class="py-3 text-center">
                <div class="flex items-center justify-center gap-1">
                  <button @click="movePriority(m.id, m.priority - 1)" :disabled="idx === 0"
                    class="text-xs transition-colors duration-150 disabled:opacity-20"
                    style="color: var(--color-text-secondary)"
                    @mouseenter="$event.target.style.color = 'var(--color-text)'"
                    @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">&#9650;</button>
                  <span class="text-xs w-5 text-center" style="color: var(--color-warm-gray)">{{ m.priority }}</span>
                  <button @click="movePriority(m.id, m.priority + 1)" :disabled="idx === models.length - 1"
                    class="text-xs transition-colors duration-150 disabled:opacity-20"
                    style="color: var(--color-text-secondary)"
                    @mouseenter="$event.target.style.color = 'var(--color-text)'"
                    @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">&#9660;</button>
                </div>
              </td>
              <td class="py-3 text-center">
                <span class="px-2 py-0.5 text-xs rounded"
                  :style="m.isEnabled ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-text-secondary)' }">
                  {{ m.isEnabled ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button v-if="!m.isPrimary" @click="setPrimary(m.id)"
                    class="text-xs transition-colors duration-150"
                    style="color: var(--color-accent)"
                    @mouseenter="$event.target.style.color = 'var(--color-accent-hover)'"
                    @mouseleave="$event.target.style.color = 'var(--color-accent)'">设为主模型</button>
                  <button @click="testConnection(m.id)"
                    class="text-xs transition-colors duration-150"
                    style="color: var(--color-text-secondary)"
                    @mouseenter="$event.target.style.color = 'var(--color-text)'"
                    @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">测试</button>
                  <button @click="deleteModel(m.id)"
                    class="text-xs transition-colors duration-150"
                    style="color: var(--color-text-secondary)"
                    @mouseenter="$event.target.style.color = 'var(--color-accent)'"
                    @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile cards -->
      <div class="md:hidden space-y-3" v-if="models.length">
        <div v-for="(m, idx) in models" :key="m.id"
          class="p-3 rounded-lg"
          :style="{ backgroundColor: 'var(--color-bg)' }">
          <div class="flex items-start justify-between mb-2">
            <div>
              <span class="text-sm font-medium" style="color: var(--color-text)">{{ m.provider }}</span>
              <span v-if="m.isPrimary" class="ml-2 px-1.5 py-0.5 text-xs rounded"
                :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">主模型</span>
            </div>
            <span class="px-2 py-0.5 text-xs rounded shrink-0"
              :style="m.isEnabled ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { backgroundColor: '#f0eeeb', color: 'var(--color-text-secondary)' }">
              {{ m.isEnabled ? '启用' : '禁用' }}
            </span>
          </div>
          <p class="text-xs mb-1" style="color: var(--color-text)">{{ m.modelName }}</p>
          <p class="text-xs mb-2" style="color: var(--color-warm-gray)">{{ m.modelType }} · 优先级 {{ m.priority }}</p>
          <div class="flex items-center gap-2 flex-wrap">
            <button @click="movePriority(m.id, m.priority - 1)" :disabled="idx === 0"
              class="text-xs px-2 py-1 rounded transition-colors duration-150 disabled:opacity-20"
              style="color: var(--color-text-secondary); border: 1px solid var(--color-border)">上移</button>
            <button @click="movePriority(m.id, m.priority + 1)" :disabled="idx === models.length - 1"
              class="text-xs px-2 py-1 rounded transition-colors duration-150 disabled:opacity-20"
              style="color: var(--color-text-secondary); border: 1px solid var(--color-border)">下移</button>
            <button v-if="!m.isPrimary" @click="setPrimary(m.id)"
              class="text-xs transition-colors duration-150"
              style="color: var(--color-accent)">设为主模型</button>
            <button @click="testConnection(m.id)"
              class="text-xs transition-colors duration-150"
              style="color: var(--color-text-secondary)">测试</button>
            <button @click="deleteModel(m.id)"
              class="text-xs transition-colors duration-150"
              style="color: var(--color-text-secondary)">删除</button>
          </div>
        </div>
      </div>

      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无模型配置，点击上方按钮添加</p>
    </section>

    <section class="card p-4 md:p-6 mt-4 md:mt-6">
      <h3 class="font-display text-base md:text-lg mb-4" style="color: var(--color-text)">数据导入/导出</h3>
      <div class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 JSON（完整备份）</p>
            <button @click="exportJson" :disabled="exporting" class="btn-primary text-sm w-full sm:w-auto">
              {{ exporting ? '导出中...' : '导出 JSON' }}
            </button>
          </div>
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 Markdown（按标签分类）</p>
            <button @click="exportMarkdown" :disabled="exportingMd"
              class="w-full sm:w-auto px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
              :style="{ backgroundColor: 'var(--color-sage)', color: 'white' }"
              @mouseenter="!exportingMd && ($event.target.style.opacity = '0.9')"
              @mouseleave="!exportingMd && ($event.target.style.opacity = '1')">
              {{ exportingMd ? '导出中...' : '导出 Markdown' }}
            </button>
          </div>
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 CSV（表格分析）</p>
            <button @click="exportCsv" :disabled="exportingCsv"
              class="w-full sm:w-auto px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
              :style="{ backgroundColor: 'var(--color-warm-gray)', color: 'white' }"
              @mouseenter="!exportingCsv && ($event.target.style.opacity = '0.9')"
              @mouseleave="!exportingCsv && ($event.target.style.opacity = '1')">
              {{ exportingCsv ? '导出中...' : '导出 CSV' }}
            </button>
          </div>
        </div>
        <div style="border-top: 1px solid var(--color-border); padding-top: 1rem">
          <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导入 JSON 备份文件</p>
          <div class="flex items-center gap-3 mb-3">
            <span class="text-xs" style="color: var(--color-text-secondary)">冲突处理:</span>
            <label class="text-xs flex items-center gap-1">
              <input type="radio" v-model="conflictMode" value="skip" /> 跳过
            </label>
            <label class="text-xs flex items-center gap-1">
              <input type="radio" v-model="conflictMode" value="overwrite" /> 覆盖
            </label>
          </div>
          <div class="border-2 border-dashed rounded-lg p-6 text-center transition-colors duration-150 cursor-pointer"
            :style="{ borderColor: dragOver ? 'var(--color-sage)' : 'var(--color-border)', backgroundColor: dragOver ? 'var(--color-sage-light)' : 'transparent' }"
            @dragover.prevent="dragOver = true"
            @dragleave="dragOver = false"
            @drop.prevent="onDrop"
            @click="fileInputRef.click()">
            <svg class="w-8 h-8 mx-auto mb-2" :style="{ color: dragOver ? 'var(--color-sage)' : 'var(--color-text-secondary)' }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
            </svg>
            <p class="text-sm" :style="{ color: dragOver ? 'var(--color-sage)' : 'var(--color-text-secondary)' }">
              {{ dragOver ? '释放文件开始导入' : '拖拽 JSON 文件到此处，或点击选择' }}
            </p>
          </div>
          <input type="file" accept=".json" @change="handleFileSelect" ref="fileInputRef" class="hidden" />

          <div v-if="importProgress" class="mt-3">
            <div class="flex items-center justify-between mb-1">
              <span class="text-xs" style="color: var(--color-text-secondary)">导入进度</span>
              <span class="text-xs font-medium" style="color: var(--color-sage)">{{ importProgress }}%</span>
            </div>
            <div class="w-full h-2 rounded-full" style="background-color: var(--color-border)">
              <div class="h-full rounded-full transition-all duration-300"
                :style="{ width: importProgress + '%', backgroundColor: 'var(--color-sage)' }"></div>
            </div>
          </div>

          <div v-if="importPreview" class="mt-3 p-3 rounded-lg" style="background-color: var(--color-sage-light)">
            <p class="text-sm font-medium" style="color: var(--color-sage)">导入预览</p>
            <p class="text-xs mt-1" style="color: var(--color-text)">共 {{ importPreview.totalCount }} 条知识</p>
            <p class="text-xs" style="color: var(--color-sage)">新增: {{ importPreview.newCount }} 条</p>
            <p v-if="importPreview.conflictCount > 0" class="text-xs" style="color: var(--color-accent)">
              冲突: {{ importPreview.conflictCount }} 条 (将{{ conflictMode === 'skip' ? '跳过' : '覆盖' }})
            </p>
            <div class="flex gap-2 mt-2">
              <button @click="confirmImport" :disabled="importing"
                class="btn-primary text-xs px-3 py-1.5">
                {{ importing ? '导入中...' : '确认导入' }}
              </button>
              <button @click="cancelImport" class="btn-secondary text-xs px-3 py-1.5">取消</button>
            </div>
          </div>
          <p v-if="importResult" class="text-sm mt-2 fade-in-enter"
            :style="{ color: importResult.success ? 'var(--color-sage)' : 'var(--color-accent)' }">
            {{ importResult.message }}
          </p>
        </div>
      </div>
    </section>

    <section class="card p-4 md:p-6 mt-4 md:mt-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-base md:text-lg" style="color: var(--color-text)">自动备份</h3>
        <button @click="createBackup" :disabled="backingUp" class="btn-primary text-sm">
          {{ backingUp ? '备份中...' : '立即备份' }}
        </button>
      </div>
      <div v-if="backups.length" class="space-y-2">
        <div v-for="bak in backups" :key="bak"
          class="flex items-center justify-between px-4 py-2.5 rounded-lg transition-colors duration-150"
          style="background-color: var(--color-bg)"
          @mouseenter="$event.currentTarget.style.backgroundColor = 'var(--color-sage-light)'"
          @mouseleave="$event.currentTarget.style.backgroundColor = 'var(--color-bg)'">
          <span class="text-sm" style="color: var(--color-text)">{{ bak }}</span>
          <button @click="downloadBackup(bak)"
            class="text-xs px-3 py-1 rounded transition-colors duration-150"
            style="color: var(--color-accent)"
            @mouseenter="$event.target.style.backgroundColor = 'rgba(207,112,88,0.1)'"
            @mouseleave="$event.target.style.backgroundColor = 'transparent'">
            下载
          </button>
        </div>
      </div>
      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无备份，将在每天 3:00 自动备份</p>
    </section>

    <transition name="fade">
      <div v-if="showAddForm" class="modal-overlay" @click.self="showAddForm = false">
        <div class="modal-panel w-[calc(100%-2rem)] sm:w-96">
          <div class="p-4 sm:p-6">
            <h3 class="font-display text-lg mb-4">添加模型</h3>
            <div class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">供应商</label>
                <select v-model="form.provider" @change="onProviderChange" class="input-field">
                  <option value="ALIYUN">阿里通义千问</option>
                  <option value="DEEPSEEK">DeepSeek</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="ANTHROPIC">Anthropic</option>
                  <option value="OLLAMA">Ollama（本地）</option>
                </select>
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">API Key</label>
                <input v-model="form.apiKey" type="password" placeholder="sk-..." class="input-field" />
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">Base URL（可选）</label>
                <input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" class="input-field" />
              </div>
              <div>
                <button @click="fetchModels" :disabled="!form.apiKey || fetching"
                  class="text-sm px-3 py-1.5 rounded-lg transition-all duration-150 mb-2"
                  :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }"
                  @mouseenter="!fetching && ($event.target.style.opacity = '0.8')"
                  @mouseleave="!fetching && ($event.target.style.opacity = '1')">
                  {{ fetching ? '拉取中...' : '从远端拉取模型列表' }}
                </button>
                <div v-if="fetchedModelList.length" class="mt-1">
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">选择模型</label>
                  <select v-model="form.modelName" class="input-field">
                    <option value="" disabled>请选择一个模型</option>
                    <option v-for="m in fetchedModelList" :key="m" :value="m">{{ m }}</option>
                  </select>
                </div>
                <div v-else>
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">模型名称</label>
                  <input v-model="form.modelName" placeholder="如 qwen-turbo（或先拉取列表）" class="input-field" />
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-6">
              <button @click="showAddForm = false" class="btn-secondary">取消</button>
              <button @click="addModel" class="btn-primary">保存</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { modelApi } from '@/api/models'
import { knowledgeApi } from '@/api/knowledge'
import { backupApi } from '@/api/backup'

const models = ref([])
const showAddForm = ref(false)
const form = ref({ provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' })
const fetching = ref(false)
const fetchedModelList = ref([])
const exporting = ref(false)
const exportingMd = ref(false)
const exportingCsv = ref(false)
const importResult = ref(null)
const importPreview = ref(null)
const conflictMode = ref('skip')
const importing = ref(false)
const importProgress = ref(0)
const dragOver = ref(false)
const fileInputRef = ref(null)
let selectedFile = null

const backups = ref([])
const backingUp = ref(false)

async function loadModels() {
  const res = await modelApi.list()
  models.value = res.data.data || []
}

function onProviderChange() {
  fetchedModelList.value = []
  form.value.modelName = ''
}

async function fetchModels() {
  if (!form.value.apiKey) return
  fetching.value = true
  fetchedModelList.value = []
  try {
    const res = await modelApi.fetchModels({
      provider: form.value.provider,
      apiKey: form.value.apiKey,
      baseUrl: form.value.baseUrl || undefined
    })
    fetchedModelList.value = res.data.data || []
  } catch {
    fetchedModelList.value = []
  } finally {
    fetching.value = false
  }
}

async function addModel() {
  await modelApi.add(form.value)
  showAddForm.value = false
  form.value = { provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' }
  await loadModels()
}

async function movePriority(id, newPriority) {
  await modelApi.updatePriority(id, newPriority)
  await loadModels()
}

async function setPrimary(id) {
  await modelApi.setPrimary(id)
  await loadModels()
}

async function testConnection(id) {
  const res = await modelApi.testConnection(id)
  alert(res.data.data ? '连接成功' : '连接失败')
}

async function deleteModel(id) {
  if (confirm('确定删除此模型配置？')) {
    await modelApi.delete(id)
    await loadModels()
  }
}

async function exportJson() {
  exporting.value = true
  try {
    const res = await knowledgeApi.exportJson()
    const blob = new Blob([res.data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindvault-export-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

async function exportMarkdown() {
  exportingMd.value = true
  try {
    const res = await knowledgeApi.exportMarkdown()
    const blob = new Blob([res.data], { type: 'application/zip' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindvault-export-${new Date().toISOString().slice(0, 10)}.zip`
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exportingMd.value = false
  }
}

async function exportCsv() {
  exportingCsv.value = true
  try {
    const res = await knowledgeApi.exportCsv()
    const blob = new Blob([res.data], { type: 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindvault-export-${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } finally {
    exportingCsv.value = false
  }
}

function onDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer.files[0]
  if (!file) return
  startImport(file)
}

function handleFileSelect(e) {
  const file = e.target.files[0]
  if (!file) return
  startImport(file)
}

async function startImport(file) {
  selectedFile = file
  importResult.value = null
  importPreview.value = null
  importProgress.value = 0
  try {
    const text = await file.text()
    importProgress.value = 30
    const res = await knowledgeApi.previewImport(text)
    importPreview.value = res.data.data
    importProgress.value = 0
  } catch (err) {
    importResult.value = { success: false, message: '预览失败: ' + (err.response?.data?.message || err.message) }
    importPreview.value = null
    importProgress.value = 0
  }
}

async function confirmImport() {
  if (!selectedFile) return
  importing.value = true
  importResult.value = null
  importProgress.value = 10
  try {
    const text = await selectedFile.text()
    importProgress.value = 40
    const res = await knowledgeApi.importJson(text, conflictMode.value)
    importProgress.value = 90
    const data = res.data.data
    importResult.value = { success: true, message: `导入成功！共 ${data.imported} 条知识 (冲突模式: ${data.conflictMode})` }
    importPreview.value = null
    selectedFile = null
    importProgress.value = 100
    setTimeout(() => { importProgress.value = 0 }, 1500)
  } catch (err) {
    importResult.value = { success: false, message: '导入失败: ' + (err.response?.data?.message || err.message) }
    importProgress.value = 0
  } finally {
    importing.value = false
  }
}

function cancelImport() {
  importPreview.value = null
  selectedFile = null
  importProgress.value = 0
}

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

onMounted(() => {
  loadModels()
  loadBackups()
})
</script>