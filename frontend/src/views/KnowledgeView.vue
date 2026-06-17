<template>
  <div class="flex flex-col h-full">
    <div class="p-4 border-b border-gray-200 space-y-3 shrink-0">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-semibold text-gray-800">知识库</h2>
        <button @click="openAddForm"
          class="px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">
          + 新建笔记
        </button>
      </div>
      <div class="relative">
        <input v-model="searchText" placeholder="搜索笔记... 用 #tag 过滤标签"
          class="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
        <svg class="absolute left-3 top-2.5 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
      </div>
      <div class="flex flex-wrap gap-2" v-if="activeTags.length">
        <span v-for="tag in activeTags" :key="tag"
          class="flex items-center gap-1 px-2 py-0.5 bg-blue-100 text-blue-600 text-xs rounded-full">
          #{{ tag }}
          <button @click="removeTag(tag)" class="hover:text-blue-800">&times;</button>
        </span>
        <span v-if="filteredItems.length !== store.items.length" class="text-xs text-gray-400 self-center">
          找到 {{ filteredItems.length }} 条结果
        </span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-4">
      <div v-if="store.isLoading" class="flex justify-center py-12">
        <div class="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
      </div>

      <div v-else-if="!filteredItems.length" class="flex flex-col items-center justify-center py-16 text-gray-400">
        <p class="text-5xl mb-4">📝</p>
        <p class="text-lg font-medium text-gray-600">{{ searchText ? '没有匹配的笔记' : '还没有笔记' }}</p>
        <p class="text-sm mt-1">{{ searchText ? '试试其他关键字或标签' : '点击右上角「新建笔记」添加第一条知识' }}</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        <NoteCard
          v-for="note in filteredItems"
          :key="note.id"
          :note="note"
          @click="openDetail(note)"
          @delete="deleteNote"
        />
      </div>
    </div>

    <!-- 详情 / 编辑 Modal -->
    <div v-if="detailNote" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
      @click.self="closeDetail">
      <div class="bg-white rounded-xl w-[600px] max-h-[80vh] overflow-y-auto mx-4 shadow-xl">
        <template v-if="!isEditing">
          <div class="p-6">
            <div class="flex items-start justify-between mb-4">
              <h3 class="text-xl font-bold text-gray-800">{{ detailNote.title }}</h3>
              <div class="flex items-center gap-2">
                <button @click="startEditing" class="text-blue-600 hover:text-blue-800 text-sm">编辑</button>
                <button @click="closeDetail" class="text-gray-400 hover:text-gray-600 text-lg leading-none">&times;</button>
              </div>
            </div>
            <div class="flex flex-wrap gap-1.5 mb-4" v-if="detailTags.length">
              <span v-for="tag in detailTags" :key="tag"
                class="px-2 py-0.5 text-xs rounded bg-gray-100 text-gray-600">#{{ tag }}</span>
            </div>
            <p class="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">{{ detailNote.content }}</p>
            <p v-if="detailNote.sourceUrl" class="mt-4 text-xs text-gray-400">
              来源: <a :href="detailNote.sourceUrl" target="_blank" class="text-blue-600 hover:underline">{{ detailNote.sourceUrl }}</a>
            </p>
            <div class="mt-4 pt-4 border-t border-gray-100 text-xs text-gray-400">
              创建于 {{ formatTime(detailNote.createdAt) }}
              <span v-if="detailNote.updatedAt !== detailNote.createdAt"> | 更新于 {{ formatTime(detailNote.updatedAt) }}</span>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="p-6">
            <div class="flex items-start justify-between mb-4">
              <h3 class="text-xl font-bold text-gray-800">编辑笔记</h3>
              <button @click="cancelEditing" class="text-gray-400 hover:text-gray-600 text-lg leading-none">&times;</button>
            </div>
            <div class="space-y-3">
              <div>
                <label class="block text-sm text-gray-600 mb-1">标题</label>
                <input v-model="editForm.title"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
              </div>
              <div>
                <label class="block text-sm text-gray-600 mb-1">内容</label>
                <textarea v-model="editForm.content" rows="8"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500 resize-none"></textarea>
              </div>
              <div>
                <label class="block text-sm text-gray-600 mb-1">标签</label>
                <input v-model="editForm.tagsText" placeholder="#java #并发 空格分隔"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
                <p class="text-xs text-gray-400 mt-1">以 # 开头，空格分隔多个标签</p>
              </div>
              <div>
                <label class="block text-sm text-gray-600 mb-1">来源 URL</label>
                <input v-model="editForm.sourceUrl" placeholder="https://"
                  class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
              </div>
            </div>
            <div class="flex justify-end gap-2 mt-6">
              <button @click="cancelEditing"
                class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
              <button @click="saveEdit"
                class="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">保存</button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 新建笔记 Modal -->
    <div v-if="showAddForm" class="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
      @click.self="showAddForm = false">
      <div class="bg-white rounded-xl p-6 w-[500px] shadow-xl">
        <h3 class="text-lg font-semibold mb-4">新建笔记</h3>
        <div class="space-y-3">
          <div>
            <label class="block text-sm text-gray-600 mb-1">标题</label>
            <input v-model="addForm.title" placeholder="输入标题"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">内容</label>
            <textarea v-model="addForm.content" placeholder="输入内容" rows="6"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500 resize-none"></textarea>
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">标签</label>
            <input v-model="addForm.tagsText" placeholder="#java #并发 空格分隔"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
            <p class="text-xs text-gray-400 mt-1">以 # 开头，空格分隔多个标签</p>
          </div>
          <div>
            <label class="block text-sm text-gray-600 mb-1">来源 URL（可选）</label>
            <input v-model="addForm.sourceUrl" placeholder="https://"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:border-blue-500" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-6">
          <button @click="closeAddForm"
            class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800">取消</button>
          <button @click="addNote"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import NoteCard from '@/components/knowledge/NoteCard.vue'

const store = useKnowledgeStore()
const searchText = ref('')
const detailNote = ref(null)
const isEditing = ref(false)
const editForm = ref({ title: '', content: '', tagsText: '', sourceUrl: '' })
const showAddForm = ref(false)
const addForm = ref({ title: '', content: '', tagsText: '', sourceUrl: '' })

const activeTags = computed(() => {
  const tags = []
  const parts = searchText.value.split(/\s+/)
  for (const p of parts) {
    if (p.startsWith('#')) tags.push(p.slice(1))
  }
  return tags
})

const keywordText = computed(() => {
  return searchText.value.split(/\s+/).filter(p => !p.startsWith('#')).join(' ')
})

const filteredItems = computed(() => {
  let items = store.items
  if (activeTags.value.length) {
    items = items.filter(n => {
      const noteTags = parseTags(n.tags)
      return activeTags.value.every(t => noteTags.includes(t))
    })
  }
  const kw = keywordText.value.toLowerCase().trim()
  if (kw) {
    items = items.filter(n =>
      (n.title && n.title.toLowerCase().includes(kw)) ||
      (n.content && n.content.toLowerCase().includes(kw))
    )
  }
  return items
})

const detailTags = computed(() => {
  if (!detailNote.value?.tags) return []
  return parseTags(detailNote.value.tags)
})

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function tagsToArray(tagsText) {
  const parts = tagsText.split(/#/).filter(Boolean)
  return parts.map(p => p.trim()).filter(Boolean)
}

function tagsToText(tagsJson) {
  const arr = parseTags(tagsJson)
  return arr.map(t => '#' + t).join(' ')
}

function removeTag(tag) {
  searchText.value = searchText.value.replace(new RegExp(`#${tag}\\s*`), '').trim()
}

function openDetail(note) {
  detailNote.value = note
  isEditing.value = false
}

function closeDetail() {
  detailNote.value = null
  isEditing.value = false
}

function openAddForm() {
  addForm.value = { title: '', content: '', tagsText: '', sourceUrl: '' }
  showAddForm.value = true
}

function closeAddForm() {
  showAddForm.value = false
}

function startEditing() {
  editForm.value = {
    title: detailNote.value.title,
    content: detailNote.value.content,
    tagsText: tagsToText(detailNote.value.tags),
    sourceUrl: detailNote.value.sourceUrl || ''
  }
  isEditing.value = true
}

function cancelEditing() {
  isEditing.value = false
}

async function saveEdit() {
  const tags = tagsToArray(editForm.value.tagsText)
  const updated = await knowledgeApi.update(detailNote.value.id, {
    title: editForm.value.title,
    content: editForm.value.content,
    tags: JSON.stringify(tags),
    sourceUrl: editForm.value.sourceUrl || null
  })
  const saved = updated.data.data
  Object.assign(detailNote.value, saved)
  const idx = store.items.findIndex(i => i.id === saved.id)
  if (idx !== -1) Object.assign(store.items[idx], saved)
  isEditing.value = false
}

async function addNote() {
  if (!addForm.value.title || !addForm.value.content) return
  const tags = tagsToArray(addForm.value.tagsText)
  await store.add({
    title: addForm.value.title,
    content: addForm.value.content,
    tags: JSON.stringify(tags),
    sourceUrl: addForm.value.sourceUrl || null
  })
  showAddForm.value = false
}

async function deleteNote(id) {
  if (confirm('确定删除此笔记？')) {
    await store.remove(id)
    if (detailNote.value?.id === id) closeDetail()
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

onMounted(() => store.loadItems())
</script>