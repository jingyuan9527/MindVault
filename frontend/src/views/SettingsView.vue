<template>
  <div class="p-6 max-w-4xl">
    <h2 class="font-display text-2xl mb-6" style="color: var(--color-text)">系统设置</h2>

    <section class="card p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-lg" style="color: var(--color-text)">模型配置</h3>
        <button @click="showAddForm = true" class="btn-primary text-sm">+ 添加模型</button>
      </div>

      <table class="w-full text-sm" v-if="models.length">
        <thead>
          <tr style="border-bottom: 1px solid var(--color-border); color: var(--color-text-secondary)">
            <th class="text-left py-2 font-medium">供应商</th>
            <th class="text-left py-2 font-medium">模型</th>
            <th class="text-left py-2 font-medium">类型</th>
            <th class="text-center py-2 font-medium">状态</th>
            <th class="text-center py-2 font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in models" :key="m.id" style="border-bottom: 1px solid var(--color-border)">
            <td class="py-3" style="color: var(--color-text)">{{ m.provider }}</td>
            <td class="py-3">
              <span style="color: var(--color-text)">{{ m.modelName }}</span>
              <span v-if="m.isPrimary" class="ml-2 px-1.5 py-0.5 text-xs rounded"
                :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">主模型</span>
            </td>
            <td class="py-3" style="color: var(--color-warm-gray)">{{ m.modelType }}</td>
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

      <p v-else class="text-sm py-4" style="color: var(--color-text-secondary)">暂无模型配置，点击上方按钮添加</p>
    </section>

    <section class="card p-6 mt-6">
      <h3 class="font-display text-lg mb-4" style="color: var(--color-text)">数据导入/导出</h3>
      <div class="space-y-4">
        <div class="flex gap-4">
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 JSON（完整备份）</p>
            <button @click="exportJson" :disabled="exporting" class="btn-primary text-sm">
              {{ exporting ? '导出中...' : '导出 JSON' }}
            </button>
          </div>
          <div>
            <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导出 Markdown（按标签分类）</p>
            <button @click="exportMarkdown" :disabled="exportingMd"
              class="px-4 py-2 rounded-lg text-sm font-medium transition-all duration-200"
              :style="{ backgroundColor: 'var(--color-sage)', color: 'white' }"
              @mouseenter="!exportingMd && ($event.target.style.opacity = '0.9')"
              @mouseleave="!exportingMd && ($event.target.style.opacity = '1')">
              {{ exportingMd ? '导出中...' : '导出 Markdown' }}
            </button>
          </div>
        </div>
        <div style="border-top: 1px solid var(--color-border); padding-top: 1rem">
          <p class="text-sm mb-2" style="color: var(--color-text-secondary)">导入 JSON 备份文件</p>
          <input type="file" accept=".json" @change="handleImport" ref="fileInput"
            class="block w-full text-sm file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm transition-colors duration-150"
            style="color: var(--color-text-secondary); --file-bg: var(--color-sage-light); --file-color: var(--color-sage)" />
          <p v-if="importResult" class="text-sm mt-2 fade-in-enter"
            :style="{ color: importResult.success ? 'var(--color-sage)' : 'var(--color-accent)' }">
            {{ importResult.message }}
          </p>
        </div>
      </div>
    </section>

    <transition name="fade">
      <div v-if="showAddForm" class="modal-overlay" @click.self="showAddForm = false">
        <div class="modal-panel w-96">
          <div class="p-6">
            <h3 class="font-display text-lg mb-4">添加模型</h3>
            <div class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">供应商</label>
                <select v-model="form.provider" class="input-field">
                  <option value="ALIYUN">阿里通义千问</option>
                  <option value="DEEPSEEK">DeepSeek</option>
                  <option value="OPENAI">OpenAI</option>
                  <option value="ANTHROPIC">Anthropic</option>
                  <option value="OLLAMA">Ollama（本地）</option>
                </select>
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">模型名称</label>
                <input v-model="form.modelName" placeholder="如 qwen-turbo" class="input-field" />
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">API Key</label>
                <input v-model="form.apiKey" type="password" placeholder="sk-..." class="input-field" />
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">Base URL（可选）</label>
                <input v-model="form.baseUrl" placeholder="https://api.openai.com/v1" class="input-field" />
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

const models = ref([])
const showAddForm = ref(false)
const form = ref({ provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' })
const exporting = ref(false)
const exportingMd = ref(false)
const importResult = ref(null)

async function loadModels() {
  const res = await modelApi.list()
  models.value = res.data.data || []
}

async function addModel() {
  await modelApi.add(form.value)
  showAddForm.value = false
  form.value = { provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' }
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

async function handleImport(e) {
  const file = e.target.files[0]
  if (!file) return
  importResult.value = null
  try {
    const text = await file.text()
    const res = await knowledgeApi.importJson(text)
    const count = res.data.data?.imported || 0
    importResult.value = { success: true, message: `导入成功！共 ${count} 条知识` }
  } catch (err) {
    importResult.value = { success: false, message: '导入失败: ' + (err.response?.data?.message || err.message) }
  }
  e.target.value = ''
}

onMounted(loadModels)
</script>