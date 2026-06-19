<template>
  <div class="flex flex-col h-full">
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <h2 class="font-display text-xl">知识库</h2>
          <div class="flex items-center gap-1 p-0.5 rounded-lg" style="background-color: var(--color-bg)">
            <button v-for="v in viewModes" :key="v.key" @click="viewMode = v.key"
              class="p-1.5 rounded transition-all duration-150"
              :style="viewMode === v.key ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' } : { color: 'var(--color-text-secondary)' }"
              :title="v.label" v-html="v.icon"></button>
          </div>
        </div>
        <div class="flex items-center gap-1.5">
          <button @click="openAddForm('url')" class="btn-secondary items-center gap-1 text-sm hidden sm:flex" title="解析网页">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
            </svg>
            <span class="hidden md:inline">解析URL</span>
          </button>
          <button @click="openAddForm('pdf')" class="btn-secondary items-center gap-1 text-sm hidden sm:flex" title="解析PDF">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
            </svg>
            <span class="hidden md:inline">解析PDF</span>
          </button>
          <div class="relative" ref="moreMenuRef">
            <button @click="showMoreMenu = !showMoreMenu"
              class="btn-secondary flex items-center justify-center text-sm sm:hidden"
              style="width: 36px; height: 36px">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"/>
              </svg>
            </button>
            <transition name="fade">
              <div v-if="showMoreMenu"
                class="absolute right-0 top-full mt-1 w-36 rounded-lg shadow-lg z-20 py-1"
                :style="{ backgroundColor: 'var(--color-surface)', border: '1px solid var(--color-border)' }">
                <button @click="openAddForm('url'); showMoreMenu = false"
                  class="flex items-center gap-2 w-full px-3 py-2 text-sm text-left transition-colors duration-150"
                  style="color: var(--color-text)"
                  @mouseenter="$event.target.style.backgroundColor = 'var(--color-sage-light)'"
                  @mouseleave="$event.target.style.backgroundColor = 'transparent'">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"/>
                  </svg>
                  解析URL
                </button>
                <button @click="openAddForm('pdf'); showMoreMenu = false"
                  class="flex items-center gap-2 w-full px-3 py-2 text-sm text-left transition-colors duration-150"
                  style="color: var(--color-text)"
                  @mouseenter="$event.target.style.backgroundColor = 'var(--color-sage-light)'"
                  @mouseleave="$event.target.style.backgroundColor = 'transparent'">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                  </svg>
                  解析PDF
                </button>
              </div>
            </transition>
          </div>
          <button @click="openAddForm('text')" class="btn-primary flex items-center gap-1.5 text-sm">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            <span class="hidden sm:inline">新建笔记</span>
          </button>
        </div>
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

    <div v-if="selectedIds.length" class="px-3 md:px-5 py-2 flex items-center gap-2 md:gap-3 shrink-0 flex-wrap"
      style="background-color: var(--color-sage-light); border-bottom: 1px solid var(--color-border)">
      <span class="text-xs md:text-sm font-medium" style="color: var(--color-sage)">已选 {{ selectedIds.length }} 项</span>
      <button @click="batchDelete" class="text-xs px-2 md:px-2.5 py-1 rounded transition-colors duration-150"
        style="color: var(--color-accent)" @mouseenter="$event.target.style.backgroundColor = 'rgba(207,112,88,0.1)'" @mouseleave="$event.target.style.backgroundColor = 'transparent'">
        批量删除</button>
      <button @click="showBatchTag = true" class="text-xs px-2 md:px-2.5 py-1 rounded transition-colors duration-150"
        style="color: var(--color-sage)" @mouseenter="$event.target.style.backgroundColor = 'rgba(163,177,138,0.2)'" @mouseleave="$event.target.style.backgroundColor = 'transparent'">
        打标签</button>
      <button @click="batchExport" class="text-xs px-2 md:px-2.5 py-1 rounded transition-colors duration-150"
        style="color: var(--color-text-secondary)" @mouseenter="$event.target.style.backgroundColor = 'rgba(0,0,0,0.05)'" @mouseleave="$event.target.style.backgroundColor = 'transparent'">
        导出</button>
      <button @click="clearSelection" class="text-xs ml-auto px-2 md:px-2.5 py-1 rounded"
        style="color: var(--color-text-secondary)">取消选择</button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div v-if="store.isLoading" class="flex justify-center py-12">
        <div class="w-6 h-6 rounded-full animate-spin"
          style="border: 2px solid var(--color-border); border-top-color: var(--color-accent)"></div>
      </div>

      <div v-else-if="!filteredItems.length" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <p class="text-5xl mb-4 opacity-50">&#x1F4DD;</p>
        <p class="text-lg font-display font-medium" style="color: var(--color-warm-gray)">{{ searchText ? '没有匹配的笔记' : '还没有笔记' }}</p>
        <p class="text-sm mt-1">{{ searchText ? '试试其他关键字或标签' : '点击右上角「新建笔记」添加第一条知识' }}</p>
      </div>

      <!-- 卡片视图 -->
      <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 md:gap-4 p-4 md:p-5 stagger-enter">
        <div v-for="note in filteredItems" :key="note.id" class="relative">
          <div class="absolute top-3 left-3 z-10" @click.stop>
            <input type="checkbox" :checked="selectedIds.includes(note.id)" @change="toggleSelect(note.id)"
              class="w-4 h-4 rounded cursor-pointer opacity-60 hover:opacity-100 transition-opacity"
              :style="{ accentColor: 'var(--color-sage)' }" />
          </div>
          <NoteCard :note="note" @click="openDetail(note)" @delete="deleteNote" />
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else-if="viewMode === 'list'">
        <NoteListItem v-for="note in filteredItems" :key="note.id"
          :note="note" :selected="selectedIds.includes(note.id)"
          @click="openDetail(note)" @toggle-select="toggleSelect" />
      </div>

      <!-- 网格视图 -->
      <div v-else class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-2 md:gap-3 p-4 md:p-5 stagger-enter">
        <div v-for="note in filteredItems" :key="note.id" class="relative">
          <div class="absolute top-2 left-2 z-10" @click.stop>
            <input type="checkbox" :checked="selectedIds.includes(note.id)" @change="toggleSelect(note.id)"
              class="w-3.5 h-3.5 rounded cursor-pointer opacity-60 hover:opacity-100 transition-opacity"
              :style="{ accentColor: 'var(--color-sage)' }" />
          </div>
          <div class="card p-3 cursor-pointer" @click="openDetail(note)">
            <p class="text-sm font-medium truncate" style="color: var(--color-text)">{{ note.title }}</p>
            <div class="text-xs mt-1" style="color: var(--color-warm-gray)">
              <ContentRenderer :content="note.summary || note.content" preview class="line-clamp-2" />
            </div>
            <div class="flex flex-wrap gap-1 mt-2" v-if="gridTags(note).length">
              <span v-for="tag in gridTags(note).slice(0, 2)" :key="tag" class="tag-pill text-xs">{{ tag }}</span>
            </div>
            <p class="text-xs mt-2" style="color: var(--color-text-secondary)">{{ formatDate(note.createdAt) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情 / 编辑 Modal -->
    <transition name="fade">
      <div v-if="detailNote" class="modal-overlay" @click.self="closeDetail">
        <div class="modal-panel w-[calc(100%-2rem)] sm:w-[600px] max-h-[80vh] overflow-y-auto">
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
              <div class="text-sm leading-relaxed" style="color: var(--color-warm-gray)">
                <ContentRenderer :content="detailNote.content" />
              </div>
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
                  <TagInput v-model="editForm.tags" placeholder="输入标签，回车添加" />
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
        <div class="modal-panel w-[calc(100%-2rem)] sm:w-[520px]">
          <div class="p-6">
            <div class="flex items-center gap-2 mb-5" style="border-bottom: 1px solid var(--color-border); padding-bottom: 1rem">
              <button v-for="tab in addTabs" :key="tab.key"
                @click="addTab = tab.key"
                class="px-3 py-1.5 text-sm rounded-lg transition-all duration-150"
                :style="addTab === tab.key ? { backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)', fontWeight: 500 } : { color: 'var(--color-text-secondary)' }">
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
                <TagInput v-model="addForm.tags" placeholder="输入标签，回车添加" />
              </div>
            </div>
            <div v-if="addTab === 'url'" class="space-y-3">
              <div>
                <label class="block text-sm mb-1" style="color: var(--color-text-secondary)">网页 URL</label>
                <input v-model="addUrl" placeholder="https://example.com/article" class="input-field" />
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

    <!-- 批量打标签 Modal -->
    <transition name="fade">
      <div v-if="showBatchTag" class="modal-overlay" @click.self="showBatchTag = false">
        <div class="modal-panel w-[calc(100%-2rem)] sm:w-96">
          <div class="p-4 sm:p-6">
            <h3 class="font-display text-lg mb-4">批量打标签</h3>
            <p class="text-xs mb-3" style="color: var(--color-text-secondary)">为选中的 {{ selectedIds.length }} 条知识添加标签</p>
            <input v-model="batchTagInput" placeholder="输入标签名" class="input-field" @keyup.enter="doBatchTag" />
            <div class="flex justify-end gap-2 mt-4">
              <button @click="showBatchTag = false" class="btn-secondary">取消</button>
              <button @click="doBatchTag" :disabled="!batchTagInput.trim()" class="btn-primary">添加</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import NoteCard from '@/components/knowledge/NoteCard.vue'
import NoteListItem from '@/components/knowledge/NoteListItem.vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'
import TagInput from '@/components/common/TagInput.vue'

const store = useKnowledgeStore()
const route = useRoute()
const searchText = ref('')
const detailNote = ref(null)
const isEditing = ref(false)
const editForm = ref({ title: '', content: '', tags: '[]', sourceUrl: '' })
const showAddForm = ref(false)
const addForm = ref({ title: '', content: '', tags: '[]', sourceUrl: '' })
const addTab = ref('text')
const addUrl = ref('')
const urlError = ref('')
const pdfFile = ref(null)
const pdfFileName = ref('')
const pdfError = ref('')
const adding = ref(false)
const relatedItems = ref([])
const viewMode = ref('card')
const selectedIds = ref([])
const showBatchTag = ref(false)
const batchTagInput = ref('')
const showMoreMenu = ref(false)
const moreMenuRef = ref(null)

function onClickOutside(e) {
  if (moreMenuRef.value && !moreMenuRef.value.contains(e.target)) {
    showMoreMenu.value = false
  }
}

const viewModes = [
  { key: 'card', label: '卡片视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zm10 0a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>' },
  { key: 'list', label: '列表视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16"/></svg>' },
  { key: 'grid', label: '网格视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zm10 0a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zm10 0a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z"/></svg>' }
]

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

function gridTags(note) {
  return parseTags(note.tags)
}

function removeTag(tag) {
  searchText.value = searchText.value.replace(new RegExp(`#${tag}\\s*`), '').trim()
}

function toggleSelect(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx >= 0) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

function clearSelection() {
  selectedIds.value = []
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

function openAddForm(tab) {
  addForm.value = { title: '', content: '', tags: '[]', sourceUrl: '' }
  addTab.value = tab || 'text'
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
    tags: detailNote.value.tags || '[]',
    sourceUrl: detailNote.value.sourceUrl || ''
  }
  isEditing.value = true
}

function cancelEditing() {
  isEditing.value = false
}

async function saveEdit() {
  const updated = await knowledgeApi.update(detailNote.value.id, {
    title: editForm.value.title,
    content: editForm.value.content,
    tags: editForm.value.tags,
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
  await store.add({
    title: addForm.value.title,
    content: addForm.value.content,
    tags: addForm.value.tags,
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

async function batchDelete() {
  if (!confirm(`确定删除选中的 ${selectedIds.value.length} 条知识？`)) return
  await knowledgeApi.batchDelete(selectedIds.value)
  store.items = store.items.filter(i => !selectedIds.value.includes(i.id))
  if (detailNote.value && selectedIds.value.includes(detailNote.value.id)) closeDetail()
  clearSelection()
}

async function doBatchTag() {
  const tag = batchTagInput.value.trim()
  if (!tag) return
  await knowledgeApi.batchTag(selectedIds.value, tag)
  for (const item of store.items) {
    if (selectedIds.value.includes(item.id)) {
      const tags = parseTags(item.tags)
      if (!tags.includes(tag)) {
        tags.push(tag)
        item.tags = JSON.stringify(tags)
      }
    }
  }
  showBatchTag.value = false
  batchTagInput.value = ''
  clearSelection()
}

async function batchExport() {
  try {
    const res = await knowledgeApi.batchExport(selectedIds.value)
    const blob = new Blob([res.data], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `mindvault-export-selected-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    console.error('批量导出失败:', err)
  }
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth()+1}/${d.getDate()}`
}

onMounted(() => {
  if (route.query.tag) {
    searchText.value = '#' + route.query.tag
  }
  store.loadItems()
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', onClickOutside)
})

watch(() => route.query, (query) => {
  if (query.tag) {
    searchText.value = '#' + query.tag
  } else {
    searchText.value = ''
  }
})
</script>