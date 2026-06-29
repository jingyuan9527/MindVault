<template>
  <div class="knowledge-view">
    <!-- ===== TOP INPUT MODULE (always visible) ===== -->
    <div class="input-area" :class="{ 'is-desktop': isDesktop && isLarge }">
      <div class="input-card">
        <!-- Toolbar row -->
        <div class="input-toolbar">
          <div class="flex items-center gap-2">
            <button class="tool-btn" :class="{ active: showExtra }" title="更多选项" @click="showExtra = !showExtra">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01" />
              </svg>
            </button>
          </div>
          <div class="flex items-center gap-2">
            <button v-if="isDesktop && isLarge" class="tool-btn" :class="{ active: splitView }" title="分栏预览" @click="splitView = !splitView">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7" />
              </svg>
            </button>
            <button class="tool-btn publish-btn" :loading="submitting" @click="publish">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19V5m0 0l-7 7m7-7l7 7" />
              </svg>
              <span class="hidden sm:inline ml-1">发布</span>
            </button>
          </div>
        </div>

        <!-- Textarea + optional preview split -->
        <div class="input-body" :class="{ split: splitView }">
          <textarea
            ref="inputRef"
            v-model="newContent"
            class="input-textarea"
            :placeholder="isMobile ? '写点什么...' : '写下你的想法...'"
            @input="autoResize"
            @keydown="handleInputKeydown"
          />
          <div v-if="splitView" class="preview-pane">
            <div class="preview-content" v-html="renderedPreview"></div>
          </div>
        </div>

        <!-- Extra options (expandable) -->
        <div v-if="showExtra" class="input-extra">
          <input v-model="newTitle" class="extra-input" placeholder="标题（可选）" />
          <div class="extra-tags">
            <input v-model="tagInput" class="extra-input flex-1" placeholder="标签，回车添加" @keydown.enter.prevent="addTag" />
            <div v-if="newTags.length" class="flex flex-wrap gap-1 mt-2">
              <span v-for="(tag, i) in newTags" :key="i" class="tag-pill">
                #{{ tag }}
                <svg class="w-3 h-3 ml-1 cursor-pointer" fill="none" stroke="currentColor" viewBox="0 0 24 24" @click="newTags.splice(i, 1)">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </span>
            </div>
          </div>
        </div>

        <!-- Bottom toolbar -->
        <div class="input-bottom-toolbar">
          <button class="tool-btn-sm" title="插入标签" @click="focusInsert('#')">#</button>
          <button class="tool-btn-sm" title="插入图片" @click="handleImageUpload">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </button>
          <button class="tool-btn-sm" title="插入代码块" @click="focusInsert('```\n\n```')">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
          </button>
          <button class="tool-btn-sm" title="分割线" @click="focusInsert('\n---\n')">&mdash;</button>
          <span class="text-xs ml-auto" style="color: var(--color-text-secondary)">{{ newContent.length }} 字</span>
        </div>
      </div>
    </div>

    <!-- ===== FILTER BAR ===== -->
    <div class="filter-bar">
      <div class="filter-inner">
        <div class="flex items-center gap-2 flex-1">
          <div class="search-wrapper flex-1">
            <svg class="w-4 h-4 search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input v-model="keyword" class="search-input" placeholder="搜索笔记..." @input="onSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword = ''; onSearch()">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <select v-model="sortBy" class="sort-select" @change="onSortChange">
            <option value="createdAt">最新</option>
            <option value="updatedAt">最近更新</option>
            <option value="title">标题</option>
          </select>
        </div>
        <div v-if="selectedFilterTags.length" class="flex flex-wrap gap-1.5 mt-2">
          <span v-for="tag in selectedFilterTags" :key="tag" class="tag-pill tag-pill-active">
            #{{ tag }}
            <svg class="w-3 h-3 ml-1 cursor-pointer" fill="none" stroke="currentColor" viewBox="0 0 24 24" @click="removeFilterTag(tag)">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </span>
          <span class="text-xs self-center cursor-pointer" style="color: var(--color-text-secondary)" @click="selectedFilterTags = []; fetchData()">清除</span>
        </div>
      </div>
    </div>

    <!-- ===== CONTENT FEED ===== -->
    <div class="feed" ref="feedRef">
      <!-- Loading skeleton -->
      <div v-if="isLoading" class="feed-list">
        <div v-for="i in 5" :key="i" class="skeleton-card p-4 mb-4">
          <div class="skeleton h-4 w-24 mb-3"></div>
          <div class="skeleton h-3 w-full mb-2"></div>
          <div class="skeleton h-3 w-3/4 mb-2"></div>
          <div class="skeleton h-3 w-1/2"></div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!items.length" class="empty-state">
        <svg class="w-12 h-12 mb-3" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <p class="text-base font-medium" style="color: var(--color-text-secondary)">{{ hasFilter ? '没有匹配的笔记' : '还没有笔记' }}</p>
        <p class="text-sm mt-1" style="color: var(--color-text-secondary)">{{ hasFilter ? '换个关键词试试？' : '在上方输入框写下第一条笔记' }}</p>
      </div>

      <!-- Note cards -->
      <div v-else class="feed-list">
        <div
          v-for="note in items"
          :key="note.id"
          class="note-card"
          :class="{ editing: editingId === note.id }"
        >
          <!-- View mode -->
          <template v-if="editingId !== note.id">
            <div class="card-header">
              <span class="card-time">{{ formatTime(note.createdAt) }}</span>
              <div class="flex flex-wrap gap-1">
                <span v-for="tag in mergedTags(note)" :key="tag" class="tag-pill">#{{ tag }}</span>
              </div>
            </div>
            <div class="card-title-row">
              <h3 class="card-title">{{ note.aiTitle || note.title || '无标题' }}</h3>
              <span v-if="note.aiTitle && note.title" class="card-original-title">原标题: {{ note.title }}</span>
            </div>
            <div class="card-content text-sm leading-relaxed" style="color: var(--color-text)">
              <ContentRenderer :content="note.content" preview class="line-clamp-4" />
            </div>
            <div class="card-actions">
              <button class="action-btn" title="编辑" @click="startEdit(note)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button class="action-btn" title="收藏">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                </svg>
              </button>
              <button class="action-btn" title="复制" @click="copyContent(note)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
              </button>
              <button class="action-btn action-btn-danger" title="删除" @click="confirmDelete(note)">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          </template>

          <!-- Edit mode (inline) -->
          <template v-else>
            <div class="edit-header">
              <h3 class="font-medium text-sm">编辑笔记</h3>
              <div class="flex items-center gap-1">
                <button class="tool-btn-sm" @click="cancelEdit">取消</button>
                <button class="tool-btn-sm save-btn" @click="saveEdit(note)">保存</button>
              </div>
            </div>
            <input v-model="editTitle" class="edit-input" placeholder="标题（可选）" />
            <textarea v-model="editContent" class="edit-textarea" rows="6" placeholder="内容..."></textarea>
            <div class="edit-tags">
              <TagInput v-model="editTags" placeholder="标签" />
            </div>
          </template>
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination-bar">
          <div class="flex items-center gap-1">
            <button class="page-btn" :disabled="page <= 0" @click="goPage(page - 1)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <span class="text-sm" style="color: var(--color-text-secondary)">{{ page + 1 }} / {{ totalPages }}</span>
            <button class="page-btn" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
          <span class="text-xs" style="color: var(--color-text-secondary)">共 {{ total }} 条</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import ContentRenderer from '@/components/common/ContentRenderer.vue'
import TagInput from '@/components/common/TagInput.vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const dialog = useDialog()
const message = useMessage()
const store = useKnowledgeStore()
const route = useRoute()
const inputRef = ref(null)
const feedRef = ref(null)

/* Responsive */
const isDesktop = ref(window.innerWidth >= 1024)
const isLarge = ref(window.innerWidth >= 1440)
const isMobile = ref(window.innerWidth < 768)

function checkScreen() {
  isDesktop.value = window.innerWidth >= 1024
  isLarge.value = window.innerWidth >= 1440
  isMobile.value = window.innerWidth < 768
}

/* Input state */
const newContent = ref('')
const newTitle = ref('')
const newTags = ref([])
const tagInput = ref('')
const showExtra = ref(false)
const splitView = ref(false)
const submitting = ref(false)

/* Filter state */
const keyword = ref('')
const sortBy = ref('createdAt')
const selectedFilterTags = ref([])
let debounceTimer = null

/* Edit state */
const editingId = ref(null)
const editTitle = ref('')
const editContent = ref('')
const editTags = ref('[]')

/* Pagination */
const page = ref(0)
const pageSize = ref(20)

const items = computed(() => store.items)
const total = computed(() => store.total)
const isLoading = computed(() => store.isLoading)
const hasFilter = computed(() => keyword.value || selectedFilterTags.value.length > 0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* Markdown preview */
const renderedPreview = computed(() => {
  if (!newContent.value) return '<p style="color:var(--color-text-secondary)">预览区域</p>'
  const html = marked.parse(newContent.value, { breaks: true, gfm: true })
  return DOMPurify.sanitize(html)
})

/* Input helpers */
function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function focusInsert(text) {
  const el = inputRef.value
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const before = newContent.value.substring(0, start)
  const after = newContent.value.substring(end)
  newContent.value = before + text + after
  nextTick(() => {
    el.focus()
    const pos = start + text.length
    el.setSelectionRange(pos, pos)
  })
}

function addTag() {
  const t = tagInput.value.trim()
  if (!t || newTags.value.includes(t)) return
  newTags.value.push(t)
  tagInput.value = ''
}

function handleInputKeydown(e) {
  if (e.ctrlKey && e.key === 'Enter') {
    publish()
  }
}

async function publish() {
  if (submitting.value || !newContent.value.trim()) {
    if (!newContent.value.trim()) message.warning('请输入内容')
    return
  }
  submitting.value = true
  try {
    await knowledgeApi.add({
      title: newTitle.value || '',
      content: newContent.value,
      userTags: JSON.stringify(newTags.value),
    })
    newContent.value = ''
    newTitle.value = ''
    newTags.value = []
    tagInput.value = ''
    showExtra.value = false
    if (inputRef.value) inputRef.value.style.height = 'auto'
    await fetchData()
    message.success('发布成功')
  } catch (err) {
    message.error('发布失败: ' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}

function handleImageUpload() {
  message.info('图片上传功能待实现')
}

/* Search */
function onSearch() {
  page.value = 0
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchData, 300)
}

function onSortChange() {
  page.value = 0
  fetchData()
}

function removeFilterTag(tag) {
  selectedFilterTags.value = selectedFilterTags.value.filter(t => t !== tag)
  fetchData()
}

/* Data */
async function fetchData() {
  await store.fetchItems({
    page: page.value,
    size: pageSize.value,
    keyword: keyword.value || undefined,
    tags: selectedFilterTags.value.length ? selectedFilterTags.value : undefined,
    sortBy: sortBy.value,
    sortOrder: 'desc',
  })
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  fetchData()
  feedRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

/* Inline edit */
function startEdit(note) {
  editingId.value = note.id
  editTitle.value = note.title || ''
  editContent.value = note.content || ''
  editTags.value = note.userTags || '[]'
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit(note) {
  try {
    await knowledgeApi.update(note.id, {
      title: editTitle.value,
      content: editContent.value,
      userTags: editTags.value,
    })
    editingId.value = null
    await fetchData()
    message.success('已保存')
  } catch (err) {
    message.error('保存失败: ' + (err.response?.data?.message || err.message))
  }
}

function confirmDelete(note) {
  dialog.warning({
    title: '删除笔记',
    content: '确定删除这条笔记吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await knowledgeApi.delete(note.id)
      if (editingId.value === note.id) editingId.value = null
      await fetchData()
    }
  })
}

function copyContent(note) {
  const text = note.content || ''
  navigator.clipboard.writeText(text).then(() => {
    message.success('已复制')
  })
}

function mergedTags(note) {
  const ai = parseTags(note.tags)
  const user = parseTags(note.userTags)
  return [...new Set([...ai, ...user])]
}

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
}

/* Init */
function initFromUrl() {
  const q = route.query
  if (q.keyword) keyword.value = q.keyword
  if (q.tags) selectedFilterTags.value = q.tags.split(',')
  if (q.page) page.value = parseInt(q.page) - 1 || 0
}

onMounted(() => {
  checkScreen()
  window.addEventListener('resize', checkScreen)
  initFromUrl()
  fetchData()
})
</script>

<style scoped>
.knowledge-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.input-area {
  padding: 16px 16px 0;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-bg);
}
.input-card { max-width: 720px; margin: 0 auto; }
@media (min-width: 1024px) {
  .input-area.is-desktop { padding: 24px 24px 0; }
}
.input-card {
  background: var(--color-surface);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px 0;
}
.tool-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;
}
.tool-btn:hover {
  background: var(--color-bg);
  color: var(--color-text);
}
.tool-btn.active {
  color: var(--color-primary);
  background: var(--color-primary-light);
}
.publish-btn {
  color: var(--color-primary);
  font-weight: 500;
}
.publish-btn:hover { background: var(--color-primary-light); }
.input-body { padding: 4px 12px 8px; }
.input-body.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.input-textarea {
  width: 100%;
  min-height: 52px;
  max-height: 300px;
  border: none;
  background: transparent;
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.6;
  font-family: inherit;
  resize: none;
  outline: none;
  padding: 8px 0;
}
.input-textarea::placeholder { color: var(--color-placeholder); }
.preview-pane {
  border-left: 1px solid var(--color-border);
  padding-left: 12px;
  max-height: 300px;
  overflow-y: auto;
}
.preview-content { font-size: 14px; line-height: 1.6; color: var(--color-text); }
.input-extra {
  padding: 8px 12px;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.extra-input {
  width: 100%;
  border: 1px solid var(--color-border);
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 6px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s ease;
}
.extra-input:focus { border-color: var(--color-primary); }
.extra-input::placeholder { color: var(--color-placeholder); }
.input-bottom-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px 8px;
  border-top: 1px solid var(--color-border);
}
.tool-btn-sm {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.15s ease;
}
.tool-btn-sm:hover { background: var(--color-bg); color: var(--color-text); }
.tool-btn-sm.save-btn {
  color: var(--color-primary);
  font-weight: 500;
  width: auto;
  padding: 0 10px;
}
.filter-bar { padding: 0; border-bottom: 1px solid var(--color-border); }
.filter-inner {
  max-width: 720px;
  margin: 0 auto;
  padding: 12px 16px;
}
.search-wrapper { position: relative; display: flex; align-items: center; }
.search-icon {
  position: absolute;
  left: 10px;
  color: var(--color-text-secondary);
  pointer-events: none;
}
.search-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 14px;
  padding: 7px 32px 7px 34px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s ease;
}
.search-input:focus { border-color: var(--color-primary); }
.search-input::placeholder { color: var(--color-placeholder); }
.search-clear {
  position: absolute;
  right: 6px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
}
.search-clear:hover { color: var(--color-text); }
.sort-select {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-text);
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  font-family: inherit;
  cursor: pointer;
  min-width: 80px;
}
.sort-select:focus { border-color: var(--color-primary); }
.feed { flex: 1; overflow-y: auto; padding: 16px; }
.feed-list { max-width: 720px; margin: 0 auto; }
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 16px;
  text-align: center;
}
.note-card {
  background: var(--color-surface);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 16px;
  margin-bottom: 16px;
  transition: all 0.15s ease;
}
.note-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08); }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.card-time { font-size: 12px; color: var(--color-text-secondary); }
.card-title-row { margin-bottom: 8px; }
.card-title { font-size: 16px; font-weight: 600; line-height: 1.4; color: var(--color-text); }
.card-original-title { display: block; font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.card-content { color: var(--color-text); line-height: 1.7; }
.card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--color-border);
}
.action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.action-btn:hover { background: var(--color-bg); color: var(--color-text); }
.action-btn-danger:hover { color: var(--color-danger); background: rgba(239, 68, 68, 0.08); }
.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}
.edit-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 15px;
  padding: 8px 12px;
  margin-bottom: 8px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.15s ease;
}
.edit-input:focus { border-color: var(--color-primary); }
.edit-textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  padding: 8px 12px;
  margin-bottom: 8px;
  outline: none;
  font-family: inherit;
  resize: vertical;
  line-height: 1.6;
  transition: border-color 0.15s ease;
}
.edit-textarea:focus { border-color: var(--color-primary); }
.edit-tags { margin-top: 4px; }
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
}
.page-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s ease;
}
.page-btn:hover:not(:disabled) { background: var(--color-surface); color: var(--color-text); }
.page-btn:disabled { opacity: 0.4; cursor: default; }
</style>
