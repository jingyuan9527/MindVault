<template>
  <div class="flex flex-col h-full">
    <div class="p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between mb-3">
        <h2 class="font-display text-xl">知识库</h2>
        <button @click="openAddForm" class="btn-primary flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          新建笔记
        </button>
      </div>
      <div class="relative">
        <input v-model="searchText" placeholder="搜索笔记... 用 #tag 过滤标签"
          class="input-field pl-9" />
        <svg class="absolute left-3 top-2.5 w-4 h-4" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
        </svg>
      </div>
      <div class="flex flex-wrap gap-2 mt-3" v-if="activeTags.length">
        <span v-for="tag in activeTags" :key="tag"
          class="tag-pill flex items-center gap-1">
          #{{ tag }}
          <button @click="removeTag(tag)" class="hover:opacity-60">&times;</button>
        </span>
        <span v-if="filteredItems.length !== store.items.length" class="text-xs self-center" style="color: var(--color-text-secondary)">
          找到 {{ filteredItems.length }} 条结果
        </span>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-5">
      <div v-if="store.isLoading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin"
          style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="!filteredItems.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <p class="text-5xl mb-4 opacity-50">&#x1F4DD;</p>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">{{ searchText ? '没有匹配的笔记' : '还没有笔记' }}</p>
        <p class="text-sm mt-1">{{ searchText ? '试试其他关键字或标签' : '点击右上角「新建笔记」添加第一条知识' }}</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 stagger-enter">
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
    <transition name="fade">
      <div v-if="detailNote" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-panel w-[600px] max-h-[80vh] overflow-y-auto">
          <template v-if="!isEditing">
            <div class="p-6">
              <div class="flex items-start justify-between mb-4">
                <h3 class="font-display text-xl font-bold" style="color: var(--color-text)">{{ detailNote.title }}</h3>
                <div class="flex items-center gap-2 shrink-0 ml-4">
                  <button @click="startEditing" class="text-sm transition-colors duration-150"
                    style="color: var(--color-accent)" @mouseenter="$event.target.style.color = 'var(--color-accent-hover)'" @mouseleave="$event.target.style.color = 'var(--color-accent)'">编辑</button>
                  <button @click="closeDetail" class="text-lg leading-none transition-colors duration-150"
                    style="color: var(--color-text-secondary)" @mouseenter="$event.target.style.color = 'var(--color-text)'" @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">&times;</button>
                </div>
              </div>
              <div class="flex flex-wrap gap-1.5 mb-4" v-if="detailTags.length">
                <span v-for="tag in detailTags" :key="tag" class="tag-pill">#{{ tag }}</span>
              </div>
              <p class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--color-warm-gray); line-height: 1.7">{{ detailNote.content }}</p>
              <p v-if="detailNote.sourceUrl" class="mt-4 text-xs">
                <span style="color: var(--color-text-secondary)">来源: </span>
                <a :href="detailNote.sourceUrl" target="_blank" style="color: var(--color-accent)" class="hover:underline">{{ detailNote.sourceUrl }}</a>
              </p>
              <div class="mt-4 pt-4 text-xs" style="border-top: 1px solid var(--color-border); color: var(--color-text-secondary)">
                创建于 {{ formatTime(detailNote.createdAt) }}
                <span v-if="detailNote.updatedAt !== detailNote.createdAt"> | 更新于 {{ formatTime(detailNote.updatedAt) }}</span>
              </div>
              <div v-if="relatedItems.length" class="mt-4 pt-4" style="border-top: 1px solid var(--color-border)">
                <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">相关笔记</p>
                <div class="space-y-2">
                  <div v-for="item in relatedItems" :key="item.id"
                    class="px-3 py-2 rounded-lg cursor-pointer transition-colors duration-150 text-sm"
                    style="background-color: var(--color-bg)"
                    @mouseenter="$event.target.style.backgroundColor = 'var(--color-sage-light)'"
                    @mouseleave="$event.target.style.backgroundColor = 'var(--color-bg)'">
                    <p class="font-medium" style="color: var(--color-text)">{{ item.title }}</p>
                    <p class="text-xs mt-0.5" style="color: var(--color-text-secondary)">
                      相似度 {{ (item.similarity * 100).toFixed(0) }}%
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="p-6">
              <div class="flex items-start justify-between mb-4">
                <h3 class="font-display text-xl font-bold">编辑笔记</h3>
                <button @click="cancelEditing" class="text-lg leading-none transition-colors duration-150"
                  style="color: var(--color-text-secondary)" @mouseenter="$event.target.style.color = 'var(--color-text)'" @mouseleave="$event.target.style.color = 'var(--color-text-secondary)'">&times;</button>
              </div>
              <div class="space-y-3">
                <div>
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">标题</label>
                  <input v-model="editForm.title" class="input-field" />
                </div>
                <div>
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">内容</label>
                  <textarea v-model="editForm.content" rows="8" class="input-field resize-none"></textarea>
                </div>
                <div>
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">标签</label>
                  <input v-model="editForm.tagsText" placeholder="#java #并发 空格分隔" class="input-field" />
                  <p class="text-xs mt-1" style="color: var(--color-text-secondary)">以 # 开头，空格分隔多个标签</p>
                </div>
                <div>
                  <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">来源 URL</label>
                  <input v-model="editForm.sourceUrl" placeholder="https://" class="input-field" />
                </div>
              </div>
              <div class="flex justify-end gap-2 mt-6">
                <button @click="cancelEditing" class="btn-secondary">取消</button>
                <button @click="saveEdit" class="btn-primary">保存</button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </transition>

    <!-- 新建笔记 Modal -->
    <transition name="fade">
      <div v-if="showAddForm" class="modal-overlay" @click.self="closeAddForm">
        <div class="modal-panel w-[520px]">
          <div class="p-6">
            <div class="flex items-center gap-2 mb-5" style="border-bottom: 1px solid var(--color-border); padding-bottom: 1rem">
              <button v-for="tab in addTabs" :key="tab.key"
                @click="addTab = tab.key"
                class="px-3 py-1.5 text-sm rounded-lg transition-all duration-150"
                :style="addTab === tab.key ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)', fontWeight: 500 } : { color: 'var(--color-text-secondary)' }"
                @mouseenter="addTab !== tab.key && ($event.target.style.color = 'var(--color-text)')"
                @mouseleave="addTab !== tab.key && ($event.target.style.color = 'var(--color-text-secondary)')">
                {{ tab.label }}
              </button>
            </div>

            <div v-if="addTab === 'text'" class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">标题</label>
                <input v-model="addForm.title" placeholder="输入标题" class="input-field" />
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">内容</label>
                <textarea v-model="addForm.content" placeholder="输入内容" rows="6" class="input-field resize-none"></textarea>
              </div>
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">标签</label>
                <input v-model="addForm.tagsText" placeholder="#java #并发 空格分隔" class="input-field" />
                <p class="text-xs mt-1" style="color: var(--color-text-secondary)">以 # 开头，空格分隔多个标签</p>
              </div>
            </div>

            <div v-if="addTab === 'url'" class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">网页 URL</label>
                <input v-model="addUrl" placeholder="https://example.com/article" class="input-field" />
                <p class="text-xs mt-1" style="color: var(--color-text-secondary)">输入网页地址，系统将自动抓取内容</p>
              </div>
              <p v-if="urlError" class="text-sm" style="color: var(--color-accent)">{{ urlError }}</p>
            </div>

            <div v-if="addTab === 'pdf'" class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">上传 PDF 文件</label>
                <input type="file" accept=".pdf" @change="onPdfSelected" ref="pdfInput"
                  class="block w-full text-sm file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm transition-colors duration-150"
                  style="color: var(--color-text-secondary)"
                  :style="{ '--file-bg': 'var(--color-sage-light)', '--file-color': 'var(--color-sage)' }" />
                <p class="text-xs mt-1" style="color: var(--color-text-secondary)">支持 .pdf 格式，系统将自动解析内容</p>
              </div>
              <p v-if="pdfFileName" class="text-sm" style="color: var(--color-sage)">已选择: {{ pdfFileName }}</p>
              <p v-if="pdfError" class="text-sm" style="color: var(--color-accent)">{{ pdfError }}</p>
            </div>

            <div class="flex justify-end gap-2 mt-6">
              <button @click="closeAddForm" class="btn-secondary">取消</button>
              <button @click="submitAdd" :disabled="adding" class="btn-primary">
                {{ adding ? '处理中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
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
const addTab = ref('text')
const addUrl = ref('')
const urlError = ref('')
const pdfFile = ref(null)
const pdfFileName = ref('')
const pdfError = ref('')
const adding = ref(false)
const relatedItems = ref([])

const addTabs = [
  { key: 'text', label: '文本' },
  { key: 'url', label: '网页' },
  { key: 'pdf', label: 'PDF' },
]

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
  loadRelated(note.id)
}

function closeDetail() {
  detailNote.value = null
  isEditing.value = false
  relatedItems.value = []
}

function openAddForm() {
  addForm.value = { title: '', content: '', tagsText: '', sourceUrl: '' }
  addTab.value = 'text'
  addUrl.value = ''
  urlError.value = ''
  pdfFile.value = null
  pdfFileName.value = ''
  pdfError.value = ''
  showAddForm.value = true
}

function closeAddForm() {
  showAddForm.value = false
  adding.value = false
}

function onPdfSelected(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 50 * 1024 * 1024) {
    pdfError.value = '文件超过 50MB 限制'
    pdfFile.value = null
    pdfFileName.value = ''
    return
  }
  pdfFile.value = file
  pdfFileName.value = file.name
  pdfError.value = ''
}

async function loadRelated(id) {
  try {
    const res = await knowledgeApi.getRelated(id)
    relatedItems.value = res.data.data || []
  } catch {}
}

async function submitAdd() {
  if (adding.value) return
  adding.value = true
  try {
    if (addTab.value === 'text') {
      await addNote()
    } else if (addTab.value === 'url') {
      await addUrlNote()
    } else if (addTab.value === 'pdf') {
      await addPdfNote()
    }
  } finally {
    adding.value = false
  }
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

async function addUrlNote() {
  if (!addUrl.value.trim()) {
    urlError.value = '请输入网页 URL'
    return
  }
  urlError.value = ''
  try {
    const res = await knowledgeApi.parseUrl(addUrl.value.trim())
    store.items.unshift(res.data.data)
    showAddForm.value = false
  } catch (err) {
    urlError.value = err.response?.data?.message || 'URL 解析失败'
  }
}

async function addPdfNote() {
  if (!pdfFile.value) {
    pdfError.value = '请选择 PDF 文件'
    return
  }
  pdfError.value = ''
  try {
    const res = await knowledgeApi.parsePdf(pdfFile.value)
    store.items.unshift(res.data.data)
    showAddForm.value = false
  } catch (err) {
    pdfError.value = err.response?.data?.message || 'PDF 解析失败'
  }
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