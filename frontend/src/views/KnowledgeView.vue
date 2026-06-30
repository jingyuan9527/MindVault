<template>
  <div class="knowledge-view">
    <!-- ===== HEADER: search + tag filter + sort ===== -->
    <header class="view-header">
      <div class="header-inner">
        <div class="search-group">
          <div class="search-wrapper">
            <svg class="search-icon w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              v-model="keyword"
              class="search-input"
              placeholder="搜索笔记..."
              @keydown.enter="onSearch"
            />
            <button v-if="keyword" class="search-clear" @click="clearSearch">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          <button class="search-btn" @click="onSearch">搜索</button>
        </div>

        <n-select
          v-model:value="selectedTagFilter"
          :options="tagOptions"
          multiple
          filterable
          placeholder="标签筛选"
          class="tag-select"
          @update:value="onTagFilterChange"
        />

        <n-select
          v-if="!isSearchMode"
          v-model:value="sortBy"
          :options="sortOptions"
          class="sort-select"
          @update:value="onSortChange"
        />

        <button
          v-if="isSearchMode"
          class="deep-toggle"
          :class="{ active: deepSearch }"
          @click="toggleDeepSearch"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span>深度检索</span>
        </button>

        <div class="density-toggle">
          <button class="density-list" :class="{ active: density === 'list' }" title="紧凑列表" @click="setDensity('list')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <button class="density-card" :class="{ active: density === 'card' }" title="卡片" @click="setDensity('card')">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <!-- ===== FEED: card grid ===== -->
    <div class="feed" ref="feedRef">
      <div v-if="isLoading" class="feed-grid">
        <div v-for="i in 6" :key="i" class="skeleton-card">
          <div class="skeleton h-4 w-24 mb-3"></div>
          <div class="skeleton h-3 w-full mb-2"></div>
          <div class="skeleton h-3 w-3/4 mb-2"></div>
          <div class="skeleton h-3 w-1/2"></div>
        </div>
      </div>

      <div v-else-if="isSearchMode" class="search-results">
        <div v-if="searchResults.length" class="search-results-list">
          <SearchResultItem
            v-for="result in searchResults"
            :key="result.id"
            :result="result"
            :keyword="keyword"
            :selected="result.id === selectedNoteId"
            @click="openDrawer(result)"
          />
        </div>
        <div v-else class="empty-state">
          <svg class="w-14 h-14 mb-3" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p class="text-base font-medium" style="color: var(--color-text-secondary)">没有匹配的笔记</p>
          <p class="text-sm mt-1" style="color: var(--color-text-secondary)">换个关键词试试？</p>
        </div>
      </div>

      <div v-else-if="!items.length" class="empty-state">
        <svg class="w-14 h-14 mb-3" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <p class="text-base font-medium" style="color: var(--color-text-secondary)">
          {{ hasFilter ? '没有匹配的笔记' : '还没有笔记' }}
        </p>
        <p class="text-sm mt-1" style="color: var(--color-text-secondary)">
          {{ hasFilter ? '换个关键词试试？' : '点击右下角 + 写下第一条笔记' }}
        </p>
      </div>

      <div v-else :class="density === 'list' ? 'feed-list' : 'feed-grid'">
        <template v-if="density === 'list'">
          <NoteListItem
            v-for="note in items"
            :key="note.id"
            :note="note"
            :highlighted="note.id === selectedNoteId"
            @click="openDrawer(note)"
          />
        </template>
        <template v-else>
          <NoteCard
            v-for="note in items"
            :key="note.id"
            :note="note"
            :selected="note.id === selectedNoteId"
            @click="openDrawer(note)"
            @edit="openEditModal(note)"
            @delete="confirmDelete(note)"
          />
        </template>

        <div v-if="totalPages > 1" class="pagination-bar col-span-full">
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

    <!-- ===== FAB ===== -->
    <button class="fab" title="新建笔记" @click="openCreateModal">
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M12 4v16m8-8H4" />
      </svg>
    </button>

    <!-- ===== EDITOR MODAL ===== -->
    <NoteEditorModal
      v-model:visible="showEditor"
      :note="editingNote"
      @saved="onNoteSaved"
    />

    <!-- ===== NOTE DRAWER ===== -->
    <NoteDrawer
      :visible="drawerVisible"
      :note="drawerNote"
      :related-notes="relatedNotes"
      :can-go-back="navStack.length > 0"
      @update:visible="onDrawerVisibleChange"
      @navigate="navigateToNote"
      @back="goBack"
      @edit="editFromDrawer"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import NoteCard from '@/components/knowledge/NoteCard.vue'
import NoteListItem from '@/components/knowledge/NoteListItem.vue'
import NoteEditorModal from '@/components/knowledge/NoteEditorModal.vue'
import NoteDrawer from '@/components/knowledge/NoteDrawer.vue'
import SearchResultItem from '@/components/knowledge/SearchResultItem.vue'

const dialog = useDialog()
const message = useMessage()
const store = useKnowledgeStore()
const route = useRoute()
const feedRef = ref(null)

/* Filter state */
const keyword = ref('')
const selectedTagFilter = ref([])
const tagOptions = ref([])
const sortBy = ref('createdAt')
const sortOptions = [
  { label: '最新创建', value: 'createdAt' },
  { label: '最近更新', value: 'updatedAt' },
  { label: '标题', value: 'title' },
]

/* Pagination */
const page = ref(0)
const pageSize = ref(20)

/* Density */
const density = ref(localStorage.getItem('knowledge-density') || 'list')
function setDensity(d) {
  density.value = d
  localStorage.setItem('knowledge-density', d)
}

/* Deep search */
const deepSearch = ref(localStorage.getItem('knowledge-deep-search') === 'true')
function toggleDeepSearch() {
  deepSearch.value = !deepSearch.value
  localStorage.setItem('knowledge-deep-search', String(deepSearch.value))
  if (isSearchMode.value) fetchData()
}

/* Modal state */
const showEditor = ref(false)
const editingNote = ref(null)

/* Drawer state */
const drawerVisible = ref(false)
const drawerNote = ref(null)
const relatedNotes = ref([])
const navStack = ref([])

/* Computed */
const items = computed(() => store.items)
const total = computed(() => store.total)
const searchResults = computed(() => store.searchResults)
const isLoading = computed(() => store.isLoading || store.isSearching)
const isSearchMode = computed(() => keyword.value.trim().length > 0)
const hasFilter = computed(() => keyword.value || selectedTagFilter.value.length > 0)
const selectedNoteId = computed(() => drawerNote.value?.id ?? null)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/* Data fetching */
async function fetchData() {
  if (isSearchMode.value) {
    await store.search({
      keyword: keyword.value.trim(),
      topN: pageSize.value,
      offset: 0,
      deep: deepSearch.value,
    })
  } else {
    await store.fetchItems({
      page: page.value,
      size: pageSize.value,
      tags: selectedTagFilter.value.length ? selectedTagFilter.value : undefined,
      sortBy: sortBy.value,
      sortOrder: 'desc',
    })
  }
}

async function loadTags() {
  try {
    const res = await knowledgeApi.getTags()
    const raw = res.data?.data || res.data || []
    tagOptions.value = raw.map(t => {
      if (typeof t === 'string') return { label: t, value: t }
      return { label: t.name || t.tagName || t.label, value: t.name || t.tagName || t.value }
    })
  } catch {
    tagOptions.value = []
  }
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  fetchData()
  feedRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

/* Search */
function onSearch() {
  page.value = 0
  fetchData()
}

function clearSearch() {
  keyword.value = ''
  page.value = 0
  fetchData()
}

function onTagFilterChange() {
  page.value = 0
  fetchData()
}

function onSortChange() {
  page.value = 0
  fetchData()
}

/* Modal actions */
function openCreateModal() {
  editingNote.value = null
  showEditor.value = true
}

function openEditModal(note) {
  editingNote.value = note
  showEditor.value = true
}

async function onNoteSaved() {
  fetchData()
  loadTags()
  if (drawerVisible.value && drawerNote.value) {
    try {
      const res = await knowledgeApi.getById(drawerNote.value.id)
      drawerNote.value = res.data.data
    } catch {
      // keep old note if fetch fails
    }
  }
}

/* Drawer actions */
async function openDrawer(note) {
  navStack.value = []
  drawerNote.value = note
  drawerVisible.value = true
  await loadRelated(note.id)
}

async function navigateToNote(related) {
  if (drawerNote.value) navStack.value.push(drawerNote.value)
  try {
    const res = await knowledgeApi.getById(related.id)
    drawerNote.value = res.data.data
  } catch {
    drawerNote.value = { ...related, content: related.summary || '' }
  }
  await loadRelated(related.id)
}

function goBack() {
  const prev = navStack.value.pop()
  if (prev) {
    drawerNote.value = prev
    loadRelated(prev.id)
  }
}

function onDrawerVisibleChange(val) {
  drawerVisible.value = val
  if (!val) {
    drawerNote.value = null
    relatedNotes.value = []
    navStack.value = []
  }
}

function editFromDrawer() {
  editingNote.value = drawerNote.value
  showEditor.value = true
}

async function loadRelated(id) {
  try {
    const res = await knowledgeApi.getRelated(id, 5)
    relatedNotes.value = res.data?.data || []
  } catch {
    relatedNotes.value = []
  }
}

/* Delete */
function confirmDelete(note) {
  dialog.warning({
    title: '删除笔记',
    content: '确定删除"{{noteTitle}}"吗？'.replace('{{noteTitle}}', note.aiTitle || note.title || '无标题'),
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await knowledgeApi.delete(note.id)
      fetchData()
    }
  })
}

/* URL init */
function initFromUrl() {
  const q = route.query
  if (q.keyword) keyword.value = q.keyword
  if (q.page) page.value = parseInt(q.page) - 1 || 0
}

onMounted(async () => {
  initFromUrl()
  await loadTags()
  if (route.query.tags) {
    const tags = route.query.tags.split(',')
    const valid = tagOptions.value.filter(o => tags.includes(o.value)).map(o => o.value)
    selectedTagFilter.value = valid
  }
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

/* ===== Header ===== */
.view-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-bg);
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}
.header-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  max-width: 1200px;
  margin: 0 auto;
}
.search-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 200px;
}
.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 120px;
}
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
.search-input:focus {
  border-color: var(--color-primary);
}
.search-input::placeholder {
  color: var(--color-placeholder);
}
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
.search-clear:hover {
  color: var(--color-text);
}
.search-btn {
  padding: 7px 14px;
  border-radius: 8px;
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.15s ease;
  font-family: inherit;
}
.search-btn:hover {
  opacity: 0.9;
}
.tag-select {
  min-width: 150px;
  max-width: 220px;
}
.sort-select {
  min-width: 100px;
}

/* ===== Deep search toggle ===== */
.deep-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.15s ease;
  font-family: inherit;
  white-space: nowrap;
}
.deep-toggle:hover {
  color: var(--color-text);
  border-color: var(--color-text-secondary);
}
.deep-toggle.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

/* ===== Density toggle ===== */
.density-toggle {
  display: flex;
  align-items: center;
  gap: 2px;
  background: var(--color-surface);
  border-radius: 8px;
  padding: 2px;
}
.density-toggle button {
  width: 30px;
  height: 30px;
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
.density-toggle button:hover {
  color: var(--color-text);
}
.density-toggle button.active {
  background: var(--color-bg);
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* ===== Feed ===== */
.feed {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.feed-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  max-width: 1200px;
  margin: 0 auto;
}
.search-results {
  max-width: 900px;
  margin: 0 auto;
}
.search-results-list {
  background: var(--color-surface);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
.feed-list {
  max-width: 900px;
  margin: 0 auto;
  background: var(--color-surface);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}
@media (max-width: 640px) {
  .feed-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== Skeleton ===== */
.skeleton-card {
  background: var(--color-surface);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.skeleton {
  background: var(--color-border);
  border-radius: 4px;
  animation: pulse 1.5s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

/* ===== Empty state ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 16px;
  text-align: center;
}

/* ===== Pagination ===== */
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
.page-btn:hover:not(:disabled) {
  background: var(--color-surface);
  color: var(--color-text);
}
.page-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

/* ===== FAB ===== */
.fab {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  border: none;
  z-index: 50;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}
.fab:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.25);
}
@media (max-width: 640px) {
  .fab {
    bottom: 20px;
    right: 20px;
    width: 48px;
    height: 48px;
  }
}
</style>