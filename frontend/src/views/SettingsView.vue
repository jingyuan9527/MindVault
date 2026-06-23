<template>
  <div class="flex flex-col h-full">
    <div class="shrink-0 px-4 md:px-5 py-3" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center gap-3">
        <div class="settings-header-icon">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
        </div>
        <div>
          <h2 class="font-display text-lg md:text-xl">系统设置</h2>
          <p class="text-xs" style="color: var(--color-text-secondary)">管理模型、Token 和数据</p>
        </div>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto p-4 md:p-6">
    <n-card size="small" class="mb-4 md:mb-6">
      <template #header>
        <div class="flex items-center justify-between w-full">
          <span class="font-display text-base md:text-lg">模型配置</span>
          <n-button type="primary" size="small" @click="showAddForm = true">+ 添加模型</n-button>
        </div>
      </template>

      <!-- Desktop NDataTable -->
      <div class="hidden md:block" v-if="models.length">
        <n-data-table :columns="modelColumns" :data="models" :bordered="false" :single-line="false" size="small" />
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
          <n-space size="small">
            <n-button text size="tiny" :disabled="idx === 0" @click="movePriority(m.id, m.priority - 1)">上移</n-button>
            <n-button text size="tiny" :disabled="idx === models.length - 1" @click="movePriority(m.id, m.priority + 1)">下移</n-button>
            <n-button v-if="!m.isPrimary" text size="tiny" type="primary" @click="setPrimary(m.id)">设为主模型</n-button>
            <n-button text size="tiny" @click="testConnection(m.id)">测试</n-button>
            <n-button text size="tiny" type="error" @click="deleteModel(m.id)">删除</n-button>
          </n-space>
        </div>
      </div>

      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无模型配置，点击上方按钮添加</p>
    </n-card>

    <section class="card p-4 md:p-6 mt-4 md:mt-6">
      <h3 class="font-display text-base md:text-lg mb-4" style="color: var(--color-text)">数据导入/导出</h3>
      <div class="space-y-4">
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 JSON（完整备份）</p>
            <n-button type="primary" size="small" :loading="exporting" @click="exportJson">导出 JSON</n-button>
          </div>
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 Markdown（按标签分类）</p>
            <n-button size="small" :loading="exportingMd" @click="exportMarkdown"
              :style="{ backgroundColor: 'var(--color-sage)', borderColor: 'var(--color-sage)' }"
              class="!text-white hover:opacity-90">导出 Markdown</n-button>
          </div>
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 CSV（表格分析）</p>
            <n-button size="small" :loading="exportingCsv" @click="exportCsv"
              :style="{ backgroundColor: 'var(--color-warm-gray)', borderColor: 'var(--color-warm-gray)' }"
              class="!text-white hover:opacity-90">导出 CSV</n-button>
          </div>
        </div>
        <div style="border-top: 1px solid var(--color-border); padding-top: 1rem">
          <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导入 JSON 备份文件</p>
          <div class="flex items-center gap-3 mb-3">
            <span class="text-xs" style="color: var(--color-text-secondary)">冲突处理:</span>
            <n-radio-group v-model:value="conflictMode" size="small">
              <n-radio value="skip">跳过</n-radio>
              <n-radio value="overwrite">覆盖</n-radio>
            </n-radio-group>
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
              <n-button type="primary" size="tiny" :loading="importing" @click="confirmImport">确认导入</n-button>
              <n-button quaternary size="tiny" @click="cancelImport">取消</n-button>
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
      <router-link to="/backups"
        class="flex items-center justify-between group">
        <h3 class="font-display text-base md:text-lg" style="color: var(--color-text)">数据备份</h3>
        <span class="text-sm transition-colors duration-150 hover-accent"
          style="color: var(--color-text-secondary)">
          管理备份 →
        </span>
      </router-link>
    </section>

    <section class="card p-4 md:p-6 mt-4 md:mt-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-base md:text-lg" style="color: var(--color-text)">API Token</h3>
        <n-button type="primary" size="small" @click="showTokenForm = true">+ 创建 Token</n-button>
      </div>
      <div v-if="tokens.length" class="space-y-2">
        <div v-for="t in tokens" :key="t.id"
          class="flex items-center justify-between px-4 py-2.5 rounded-lg transition-colors duration-150 hover-sage-bg"
          style="background-color: var(--color-bg)">
          <div class="flex-1 min-w-0">
            <span class="text-sm font-medium" style="color: var(--color-text)">{{ t.name }}</span>
            <span class="text-xs ml-2" style="color: var(--color-warm-gray)">创建于 {{ t.createdAt?.slice(0, 10) }}</span>
            <span v-if="t.lastUsedAt" class="text-xs ml-2" style="color: var(--color-warm-gray)">最后使用 {{ t.lastUsedAt?.slice(0, 10) }}</span>
            <span v-if="t.expiresAt" class="text-xs ml-2"
              :style="{ color: new Date(t.expiresAt) < new Date() ? 'var(--color-accent)' : 'var(--color-sage)' }">
              {{ new Date(t.expiresAt) < new Date() ? '已过期' : '有效期至 ' + t.expiresAt?.slice(0, 10) }}
            </span>
          </div>
          <n-button text size="tiny" type="error" @click="deleteToken(t.id)">删除</n-button>
        </div>
      </div>
      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无 API Token</p>
    </section>

    <n-modal v-model:show="showTokenForm" preset="card" style="max-width: 420px" :bordered="false" title="创建 API Token">
      <n-space vertical size="medium">
        <n-input v-model:value="tokenForm.name" placeholder="如 web-clipper" />
        <n-input-number v-model:value="tokenForm.expireDays" :min="1" :max="365" placeholder="有效期（天）" />
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTokenForm = false">取消</n-button>
          <n-button type="primary" :loading="creatingToken" @click="createToken">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="newTokenValueShow" preset="card" style="max-width: 480px" :bordered="false" title="Token 已创建">
      <p class="text-sm mb-4" style="color: var(--color-accent)">请立即复制保存，关闭后将无法再次查看</p>
      <n-input :value="newTokenValue" readonly>
        <template #suffix>
          <n-button text size="tiny" @click="copyTokenValue">复制</n-button>
        </template>
      </n-input>
      <template #footer>
        <n-space justify="end">
          <n-button type="primary" @click="newTokenValue = ''; newTokenValueShow = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="showAddForm" preset="card" style="max-width: 400px" :bordered="false" title="添加模型">
      <n-space vertical size="medium">
        <n-select v-model:value="form.provider" :options="providerOptions" @update:value="onProviderChange" />
        <n-input v-model:value="form.apiKey" type="password" placeholder="sk-..." />
        <n-input v-model:value="form.baseUrl" placeholder="https://api.openai.com/v1" />
        <div>
          <n-button size="tiny" :disabled="!form.apiKey" :loading="fetching" @click="fetchModels"
            :style="{ backgroundColor: 'var(--color-sage-light)', borderColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }"
            class="mb-2">从远端拉取模型列表</n-button>
          <n-select v-if="fetchedModelList.length" v-model:value="form.modelName" :options="fetchedModelOptions" placeholder="请选择一个模型" />
          <n-input v-else v-model:value="form.modelName" placeholder="如 qwen-turbo（或先拉取列表）" />
        </div>
      </n-space>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showAddForm = false">取消</n-button>
          <n-button type="primary" @click="addModel">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
  </div>
</template>

<script setup>
import { ref, computed, h, onMounted } from 'vue'
import { NTag, NButton, NSpace } from 'naive-ui'
import { modelApi } from '@/api/models'
import { knowledgeApi } from '@/api/knowledge'
import { authApi } from '@/api/auth'

const dialog = useDialog()
const message = useMessage()

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

const tokens = ref([])
const showTokenForm = ref(false)
const tokenForm = ref({ name: '', expireDays: 30 })
const creatingToken = ref(false)
const newTokenValue = ref('')
const newTokenValueShow = ref(false)

const providerOptions = [
  { label: '阿里通义千问', value: 'ALIYUN' },
  { label: 'DeepSeek', value: 'DEEPSEEK' },
  { label: 'OpenAI', value: 'OPENAI' },
  { label: 'Anthropic', value: 'ANTHROPIC' },
  { label: 'Ollama（本地）', value: 'OLLAMA' },
]

const fetchedModelOptions = computed(() =>
  fetchedModelList.value.map(m => ({ label: m, value: m }))
)

async function loadTokens() {
  try {
    const res = await authApi.listTokens()
    tokens.value = res.data.data || []
  } catch {}
}

async function createToken() {
  if (!tokenForm.value.name.trim()) return
  creatingToken.value = true
  try {
    const res = await authApi.createToken(tokenForm.value)
    newTokenValue.value = res.data.data.token
    showTokenForm.value = false
    newTokenValueShow.value = true
    await loadTokens()
  } finally {
    creatingToken.value = false
  }
}

async function deleteToken(id) {
  try {
    await authApi.deleteToken(id)
    await loadTokens()
  } catch {}
}

function copyTokenValue() {
  navigator.clipboard?.writeText(newTokenValue.value)
  newTokenValue.value = ''
  newTokenValueShow.value = false
}

async function loadModels() {
  const res = await modelApi.list()
  models.value = res.data.data || []
}

const modelColumns = [
  { title: '供应商', key: 'provider', width: 100 },
  {
    title: '模型',
    key: 'modelName',
    render(row) {
      const children = [row.modelName]
      if (row.isPrimary) {
        children.push(h(NTag, { size: 'tiny', type: 'success', bordered: false, style: 'margin-left: 8px' }, { default: () => '主模型' }))
      }
      return children
    }
  },
  { title: '类型', key: 'modelType', width: 100 },
  {
    title: '优先级',
    key: 'priority',
    width: 80,
    render(row, idx) {
      return h('div', { style: 'display: flex; align-items: center; justify-content: center; gap: 4px' }, [
        h(NButton, { text: true, size: 'tiny', disabled: idx === 0, onClick: () => movePriority(row.id, row.priority - 1) }, { default: () => '▲' }),
        h('span', { style: 'font-size: 12px; width: 20px; text-align: center; color: var(--color-warm-gray)' }, String(row.priority)),
        h(NButton, { text: true, size: 'tiny', disabled: idx === models.length - 1, onClick: () => movePriority(row.id, row.priority + 1) }, { default: () => '▼' }),
      ])
    }
  },
  {
    title: '状态',
    key: 'isEnabled',
    width: 70,
    render(row) {
      return h(NTag, { size: 'small', type: row.isEnabled ? 'success' : 'default', bordered: false }, { default: () => row.isEnabled ? '启用' : '禁用' })
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      const btns = []
      if (!row.isPrimary) {
        btns.push(h(NButton, { text: true, size: 'tiny', type: 'primary', onClick: () => setPrimary(row.id) }, { default: () => '设为主模型' }))
      }
      btns.push(h(NButton, { text: true, size: 'tiny', onClick: () => testConnection(row.id) }, { default: () => '测试' }))
      btns.push(h(NButton, { text: true, size: 'tiny', type: 'error', onClick: () => deleteModel(row.id) }, { default: () => '删除' }))
      return h(NSpace, { size: 'small', align: 'center', justify: 'center' }, { default: () => btns })
    }
  }
]

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
  if (res.data.data) {
    message.success('连接成功')
  } else {
    message.error('连接失败')
  }
}

async function deleteModel(id) {
  dialog.warning({
    title: '删除模型',
    content: '确定删除此模型配置？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await modelApi.delete(id)
      await loadModels()
    }
  })
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

onMounted(() => {
  loadModels()
  loadTokens()
})
</script>

<style scoped>
.settings-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-sage) 0%, #4a6a47 100%);
}
</style>