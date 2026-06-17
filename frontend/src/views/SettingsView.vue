<template>
  <div class="p-6 max-w-4xl">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">系统设置</h2>

    <section class="bg-white rounded-xl border border-gray-200 p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-lg font-semibold text-gray-700">模型配置</h3>
        <button @click="showAddForm = true"
          class="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">
          + 添加模型
        </button>
      </div>

      <table class="w-full text-sm" v-if="models.length">
        <thead>
          <tr class="border-b border-gray-100 text-gray-500">
            <th class="text-left py-2">供应商</th>
            <th class="text-left py-2">模型</th>
            <th class="text-left py-2">类型</th>
            <th class="text-center py-2">状态</th>
            <th class="text-center py-2">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in models" :key="m.id" class="border-b border-gray-50">
            <td class="py-3">{{ m.provider }}</td>
            <td class="py-3">
              {{ m.modelName }}
              <span v-if="m.isPrimary" class="ml-2 px-1.5 py-0.5 bg-blue-100 text-blue-600 text-xs rounded">主模型</span>
            </td>
            <td class="py-3">{{ m.modelType }}</td>
            <td class="py-3 text-center">
              <span class="px-2 py-0.5 text-xs rounded"
                :class="m.isEnabled ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'">
                {{ m.isEnabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td class="py-3 text-center">
              <div class="flex items-center justify-center gap-2">
                <button v-if="!m.isPrimary" @click="setPrimary(m.id)"
                  class="text-blue-600 hover:text-blue-800 text-xs">设为主模型</button>
                <button @click="testConnection(m.id)"
                  class="text-gray-500 hover:text-gray-700 text-xs">测试</button>
                <button @click="deleteModel(m.id)"
                  class="text-red-500 hover:text-red-700 text-xs">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <p v-else class="text-gray-400 text-sm py-4">暂无模型配置，点击上方按钮添加</p>
    </section>

    <div v-if="showAddForm" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
         @click.self="showAddForm = false">
      <div class="bg-white rounded-xl p-6 w-96 shadow-xl">
        <h3 class="text-lg font-semibold mb-4">添加模型</h3>
        <div class="space-y-3">
          <div>
            <label class="block text-sm text-gray-600 mb-1">供应商</label>
            <select v-model="form.provider"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500">
              <option value="ALIYUN">阿里通义千问</option>
              <option value="DEEPSEEK">DeepSeek</option>
              <option value="OPENAI">OpenAI</option>
              <option value="ANTHROPIC">Anthropic</option>
              <option value="OLLAMA">Ollama（本地）</option>
            </select>
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">模型名称</label>
            <input v-model="form.modelName" placeholder="如 qwen-turbo"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">API Key</label>
            <input v-model="form.apiKey" type="password" placeholder="sk-..."
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">Base URL（可选）</label>
            <input v-model="form.baseUrl" placeholder="https://api.openai.com/v1"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button @click="showAddForm = false"
            class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
          <button @click="addModel"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { modelApi } from '@/api/models'

const models = ref([])
const showAddForm = ref(false)
const form = ref({ provider: 'ALIYUN', modelName: '', apiKey: '', baseUrl: '' })

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

onMounted(loadModels)
</script>