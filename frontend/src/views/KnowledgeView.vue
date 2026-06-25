<template>
  <div class="flex flex-col h-full">
    <!-- Search & Filter Bar -->
    <div class="p-4 md:p-5 shrink-0" style="border-bottom: 1px solid var(--color-border)">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <div class="knowledge-header-icon">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
            </svg>
          </div>
          <h2 class="font-display text-xl">知识库</h2>
          <div class="flex items-center gap-1 p-0.5 rounded-lg" style="background-color: var(--color-bg)">
            <span v-for="v in viewModes" :key="v.key" :title="v.label" class="cursor-pointer px-1" @click="viewMode = v.key" v-html="v.icon" />
          </div>
        </div>
        <div class="flex items-center gap-1.5">
          <n-button type="primary" size="small" @click="openAddForm('text')">
            <template #icon>
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
            </template>
            <span>新建</span>
          </n-button>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <n-input v-model:value="keywordInput" placeholder="搜索笔记..." clearable class="flex-1" @clear="onKeywordClear">
          <template #prefix>
            <svg class="w-4 h-4" style="color: var(--color-text-secondary)" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </template>
        </n-input>
        <n-select v-model:value="sortBy" :options="sortOptions" size="small" style="width: 120px" @update:value="onSortChange" />
      </div>

      <div v-if="selectedTags.length" class="flex flex-wrap gap-2 mt-3">
        <span v-for="tag in selectedTags" :key="tag" style="background-color: var(--color-sage-light); color: var(--color-sage)" class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium">
          #{{ tag }}
          <svg class="w-3 h-3 cursor-pointer hover:opacity-70" fill="none" stroke="currentColor" viewBox="0 0 24 24" @click="removeTag(tag)"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M6 18L18 6M6 6l12 12"/></svg>
        </span>
        <span class="text-xs self-center" style="color: var(--color-text-secondary)">共 {{ store.total }} 条结果</span>
        <span class="text-xs self-center cursor-pointer hover:underline ml-1" style="color: var(--color-text-secondary)" @click="clearAllTags">清除</span>
      </div>
      <div class="mt-2">
        <n-select
          v-model:value="addTagValue"
          :options="tagOptions"
          filterable
          placeholder="添加标签筛选..."
          size="small"
          style="max-width: 260px"
          clearable
          @update:value="onAddTag"
        />
      </div>
    </div>

    <!-- Batch actions -->
    <div v-if="selectedIds.length" class="px-3 md:px-5 py-2 flex items-center gap-2 md:gap-3 shrink-0 flex-wrap" style="background-color: var(--color-sage-light); border-bottom: 1px solid var(--color-border)">
      <span class="text-xs md:text-sm font-medium" style="color: var(--color-sage)">已选 {{ selectedIds.length }} 项</span>
      <n-button quaternary size="tiny" type="error" @click="batchDelete">
        <template #icon>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
        </template>
        批量删除
      </n-button>
      <n-button quaternary size="tiny" @click="showBatchTag = true">
        <template #icon>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"/></svg>
        </template>
        打标签
      </n-button>
      <n-button quaternary size="tiny" @click="batchAiTag" :loading="aiTagging">
        <template #icon>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z"/></svg>
        </template>
        AI 打标签
      </n-button>
      <n-button quaternary size="tiny" @click="batchExport">
        <template #icon>
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
        </template>
        导出
      </n-button>
      <n-button quaternary size="tiny" class="ml-auto" @click="clearSelection">取消选择</n-button>
    </div>

    <!-- Content Area -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="store.isLoading && viewMode === 'card'" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 md:gap-4 p-4 md:p-5">
        <n-card v-for="i in 6" :key="i" size="small">
          <n-skeleton text :repeat="4" />
        </n-card>
      </div>
      <div v-else-if="store.isLoading && viewMode === 'list'" class="px-4 md:px-5 py-2 space-y-2">
        <n-card v-for="i in 8" :key="i" size="small">
          <div class="flex items-center gap-3">
            <n-skeleton width="16px" height="16px" round />
            <div class="flex-1">
              <n-skeleton text :repeat="2" />
            </div>
          </div>
        </n-card>
      </div>
      <div v-else-if="store.isLoading" class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-2 md:gap-3 p-4 md:p-5">
        <n-card v-for="i in 8" :key="i" size="small">
          <n-skeleton text :repeat="3" />
        </n-card>
      </div>

      <div v-else-if="!store.items.length && !store.isLoading" class="flex flex-col items-center justify-center py-16" style="color: var(--color-text-secondary)">
        <svg class="w-12 h-12 mb-4 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
        </svg>
        <p class="text-lg font-display font-medium" style="color: var(--color-text-secondary)">{{ hasActiveFilter ? '没有匹配的笔记' : '还没有笔记' }}</p>
        <p class="text-sm mt-1 opacity-60">{{ hasActiveFilter ? '换个关键词试试？' : '点击右上角「新建」添加第一条知识' }}</p>
      </div>

      <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 md:gap-4 p-4 md:p-5 stagger-enter">
        <div v-for="note in store.items" :key="note.id" class="relative">
          <div class="absolute top-3 left-3 z-10" @click.stop>
            <n-checkbox :checked="selectedIds.includes(note.id)" size="small" @update:checked="toggleSelect(note.id)" />
          </div>
          <NoteCard :note="note" @click="openDetail(note)" @delete="deleteNote" />
        </div>
      </div>

      <div v-else-if="viewMode === 'list'">
        <NoteListItem v-for="note in store.items" :key="note.id" :note="note" :selected="selectedIds.includes(note.id)" @click="openDetail(note)" @toggle-select="toggleSelect" />
      </div>

      <div v-else class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-2 md:gap-3 p-4 md:p-5 stagger-enter">
        <div v-for="note in store.items" :key="note.id" class="relative">
          <div class="absolute top-2 left-3 z-10" @click.stop>
            <n-checkbox :checked="selectedIds.includes(note.id)" size="small" @update:checked="toggleSelect(note.id)" />
          </div>
          <n-card size="small" hoverable class="cursor-pointer" :segmented="{ action: 'soft' }" @click="openDetail(note)">
            <p class="text-sm font-medium truncate">{{ note.aiTitle || note.title }}</p>
            <div class="text-xs mt-1 leading-relaxed" style="color: var(--color-warm-gray)">
              <ContentRenderer :content="note.summary || note.content" preview class="line-clamp-2" />
            </div>
            <div v-if="gridTags(note).length" class="flex flex-wrap gap-1 mt-2">
              <n-tag v-for="tag in gridTags(note).slice(0, 2)" :key="tag" size="tiny" type="primary" :bordered="false">#{{ tag }}</n-tag>
            </div>
            <template #action>
              <div class="flex items-center justify-between w-full px-1">
                <span class="text-xs" style="color: var(--color-text-secondary)">{{ formatDate(note.createdAt) }}</span>
              </div>
            </template>
          </n-card>
        </div>
      </div>
    </div>

    <!-- Bottom Paginator -->
    <div v-if="!store.isLoading && store.total > 0" class="px-4 md:px-5 py-2 flex items-center justify-between shrink-0" style="border-top: 1px solid var(--color-border); background-color: var(--color-bg)">
      <n-pagination
        :page="currentPage"
        :page-count="totalPages"
        :page-size="pageSize"
        :page-slot="5"
        :show-size-picker="true"
        :page-sizes="[20, 50, 100]"
        @update:page="onPageChange"
        @update:page-size="onPageSizeChange"
        size="small"
      />
      <span class="text-xs" style="color: var(--color-text-secondary)">共 {{ store.total }} 条</span>
    </div>

    <!-- Detail / Edit Modal -->
    <n-modal v-model:show="showDetail" preset="card" class="opaque-modal" style="max-width: 600px; max-height: 80vh; background-color: var(--color-bg) !important;" :bordered="false" size="large" @after-leave="closeDetail">
      <template v-if="!isEditing && detailNote">
        <div class="flex items-start justify-between mb-1">
          <h3 class="font-display text-xl font-bold" style="color: var(--color-text)">{{ detailNote.aiTitle || detailNote.title }}</h3>
          <div class="flex items-center gap-1.5 shrink-0 ml-4">
            <n-button quaternary size="tiny" @click="startEditing">
              <template #icon>
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
              </template>
            </n-button>
            <n-button v-if="detailNote.autoProcessStatus === 'COMPLETED' || detailNote.autoProcessStatus === 'RELATION_DONE'" quaternary size="tiny" @click="reprocessNote">
              <template #icon>
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/></svg>
              </template>
            </n-button>
            <n-button quaternary size="tiny" @click="closeDetail">✕</n-button>
          </div>
        </div>
        <p v-if="detailNote.aiTitle && detailNote.title" class="text-xs mb-2" style="color: var(--color-text-secondary)">原标题: {{ detailNote.title }}</p>
        <n-space v-if="mergedDetailTags.length" size="small" class="mb-4">
          <n-tag v-for="tag in mergedDetailTags" :key="tag" size="tiny" type="primary" :bordered="false">#{{ tag }}</n-tag>
        </n-space>
        <div class="text-sm leading-relaxed" style="color: var(--color-warm-gray)">
          <ContentRenderer :content="detailNote.content" />
        </div>
        <p v-if="detailNote.sourceUrl" class="mt-4 text-xs">
          <span style="color: var(--color-text-secondary)">来源: </span>
          <n-a :href="detailNote.sourceUrl" target="_blank">{{ detailNote.sourceUrl }}</n-a>
        </p>
        <div class="mt-4 pt-4 text-xs" style="border-top: 1px solid var(--color-border); color: var(--color-text-secondary)">
          创建于 {{ formatTime(detailNote.createdAt) }}
          <span v-if="detailNote.updatedAt !== detailNote.createdAt"> | 更新于 {{ formatTime(detailNote.updatedAt) }}</span>
        </div>
        <div v-if="relatedItems.length" class="mt-4 pt-4" style="border-top: 1px solid var(--color-border)">
          <p class="text-xs font-medium mb-2" style="color: var(--color-text-secondary)">相关笔记</p>
          <n-space vertical size="small">
            <div v-for="item in relatedItems" :key="item.id" class="px-3 py-2 rounded-lg cursor-pointer transition-colors duration-150 text-sm hover-sage-bg" style="background-color: var(--color-bg)">
              <p class="font-medium" style="color: var(--color-text)">{{ item.title }}</p>
              <p class="text-xs mt-0.5" style="color: var(--color-text-secondary)">相似度 {{ (item.similarity * 100).toFixed(0) }}%</p>
            </div>
          </n-space>
        </div>
      </template>
      <template v-else>
        <div class="flex items-start justify-between mb-4">
          <h3 class="font-display text-xl font-bold">编辑笔记</h3>
          <n-button quaternary size="tiny" @click="cancelEditing">✕</n-button>
        </div>
        <n-space vertical size="medium">
          <n-form-item label="标题">
            <n-input v-model:value="editForm.title" />
          </n-form-item>
          <n-form-item label="内容">
            <n-input v-model:value="editForm.content" type="textarea" rows="8" />
          </n-form-item>
          <n-form-item label="标签">
            <TagInput v-model="editForm.tags" placeholder="输入标签，回车添加" />
          </n-form-item>
          <n-form-item label="来源 URL">
            <n-input v-model:value="editForm.sourceUrl" placeholder="https://" />
          </n-form-item>
        </n-space>
        <div class="flex justify-end gap-2 mt-6">
          <n-button @click="cancelEditing">取消</n-button>
          <n-button type="primary" @click="saveEdit">保存</n-button>
        </div>
      </template>
    </n-modal>

    <!-- New Note Modal -->
    <n-modal v-model:show="showAddForm" preset="card" class="opaque-modal" style="max-width: 520px; background-color: var(--color-bg) !important;" :bordered="false" title="新建笔记" @after-leave="closeAddForm">
      <n-tabs v-model:value="addTab" type="line" animated>
        <n-tab-pane v-for="tab in addTabs" :key="tab.key" :name="tab.key" :tab="tab.label">
          <n-space v-if="tab.key === 'text'" vertical size="medium">
            <n-input v-model:value="addForm.title" placeholder="输入标题" />
            <n-input v-model:value="addForm.content" type="textarea" placeholder="输入内容" rows="6" />
            <TagInput v-model="addForm.tags" placeholder="输入标签，回车添加" />
          </n-space>
          <n-space v-if="tab.key === 'url'" vertical size="medium">
            <n-input v-model:value="addUrl" placeholder="https://example.com/article" />
            <n-alert v-if="urlError" type="warning" :show-icon="true" closable @close="urlError = ''">{{ urlError }}</n-alert>
          </n-space>
          <n-space v-if="tab.key === 'pdf'" vertical size="medium">
            <input ref="pdfInput" type="file" accept=".pdf" class="hidden" @change="onPdfSelected" />
            <n-button @click="pdfInput?.click()">选择 PDF 文件</n-button>
            <p v-if="pdfFileName" class="text-sm" style="color: var(--color-sage)">已选择: {{ pdfFileName }}</p>
            <n-alert v-if="pdfError" type="warning" :show-icon="true" closable @close="pdfError = ''">{{ pdfError }}</n-alert>
          </n-space>
        </n-tab-pane>
      </n-tabs>
      <template #footer>
        <n-space justify="end">
          <n-button @click="closeAddForm">取消</n-button>
          <n-button type="primary" :loading="adding" @click="submitAdd">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- Batch Tag Modal -->
    <n-modal v-model:show="showBatchTag" preset="card" class="opaque-modal" style="max-width: 400px; background-color: var(--color-bg) !important;" :bordered="false" title="批量打标签">
      <p class="text-xs mb-3" style="color: var(--color-text-secondary)">为选中的 {{ selectedIds.length }} 条知识添加标签</p>
      <n-input v-model:value="batchTagInput" placeholder="输入标签名" @keyup.enter="doBatchTag" />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showBatchTag = false">取消</n-button>
          <n-button type="primary" :disabled="!batchTagInput.trim()" @click="doBatchTag">添加</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import NoteCard from '@/components/knowledge/NoteCard.vue'
import NoteListItem from '@/components/knowledge/NoteListItem.vue'
import ContentRenderer from '@/components/common/ContentRenderer.vue'
import TagInput from '@/components/common/TagInput.vue'

const dialog = useDialog()
const message = useMessage()
const store = useKnowledgeStore()
const route = useRoute()
const router = useRouter()

const keywordInput = ref('')
const selectedTags = ref([])
const debounceTimer = ref(null)

const pageSize = ref(20)
const currentPage = ref(1)

const sortBy = ref('createdAt')
const sortOrder = ref('desc')

const tagOptions = ref([])
const addTagValue = ref(null)

const showDetail = ref(false)
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
const pdfInput = ref(null)
const aiTagging = ref(false)

const viewModes = [
  { key: 'card', label: '卡片视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/></svg>' },
  { key: 'list', label: '列表视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16"/></svg>' },
  { key: 'grid', label: '网格视图', icon: '<svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 5a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1V5zm10 0a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1V5zM4 15a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1H5a1 1 0 01-1-1v-4zm10 0a1 1 0 011-1h4a1 1 0 011 1v4a1 1 0 01-1 1h-4a1 1 0 01-1-1v-4z"/></svg>' }
]

const addTabs = [
  { key: 'text', label: '文本' },
  { key: 'url', label: '网页' },
  { key: 'pdf', label: 'PDF' },
]

const sortOptions = [
  { label: '创建时间', value: 'createdAt' },
  { label: '更新时间', value: 'updatedAt' },
  { label: '标题 A→Z', value: 'title' },
]

const hasActiveFilter = computed(() => keywordInput.value || selectedTags.value.length > 0)
const totalPages = computed(() => Math.max(1, Math.ceil(store.total / pageSize.value)))

async function fetchData() {
  await store.fetchItems({
    page: currentPage.value - 1,
    size: pageSize.value,
    keyword: keywordInput.value || undefined,
    tags: selectedTags.value.length ? selectedTags.value : undefined,
    sortBy: sortBy.value,
    sortOrder: sortOrder.value,
  })
}

function syncUrl() {
  const query = {}
  if (keywordInput.value) query.keyword = keywordInput.value
  if (selectedTags.value.length) query.tags = selectedTags.value.join(',')
  if (currentPage.value > 1) query.page = String(currentPage.value)
  if (pageSize.value !== 20) query.size = String(pageSize.value)
  if (sortBy.value !== 'createdAt') query.sortBy = sortBy.value
  router.replace({ query })
}

async function loadAndSync() {
  await fetchData()
  syncUrl()
}

function triggerSearch() {
  currentPage.value = 1
  clearTimeout(debounceTimer.value)
  debounceTimer.value = setTimeout(loadAndSync, 300)
}

function onKeywordClear() {
  keywordInput.value = ''
  triggerSearch()
}

function addTag(tag) {
  if (!tag || selectedTags.value.includes(tag)) return
  selectedTags.value.push(tag)
  addTagValue.value = null
  triggerSearch()
}

function onAddTag(val) {
  addTag(val)
}

function removeTag(tag) {
  selectedTags.value = selectedTags.value.filter(t => t !== tag)
  triggerSearch()
}

function clearAllTags() {
  selectedTags.value = []
  triggerSearch()
}

function onPageChange(page) {
  currentPage.value = page
  loadAndSync()
}

function onPageSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadAndSync()
}

function onSortChange(val) {
  sortBy.value = val
  currentPage.value = 1
  loadAndSync()
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
  showDetail.value = true
  loadRelated(note.id)
}

function closeDetail() {
  showDetail.value = false
  detailNote.value = null
  isEditing.value = false
  relatedItems.value = []
}

function startEditing() {
  editForm.value = {
    title: detailNote.value.title,
    content: detailNote.value.content,
    tags: detailNote.value.userTags || detailNote.value.tags || '[]',
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
    userTags: editForm.value.tags,
    sourceUrl: editForm.value.sourceUrl || null
  })
  const saved = updated.data.data
  Object.assign(detailNote.value, saved)
  isEditing.value = false
  fetchData()
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

async function addNote() {
  if (!addForm.value.content) {
    message.warning('请输入内容')
    return
  }
  await store.add({ title: addForm.value.title, content: addForm.value.content, userTags: addForm.value.tags, sourceUrl: addForm.value.sourceUrl || null })
  showAddForm.value = false
  fetchData()
}

async function addUrlNote() {
  if (!addUrl.value.trim()) {
    urlError.value = '请输入网页 URL'
    return
  }
  urlError.value = ''
  try {
    await knowledgeApi.parseUrl(addUrl.value.trim())
    showAddForm.value = false
    fetchData()
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
    await knowledgeApi.parsePdf(pdfFile.value)
    showAddForm.value = false
    fetchData()
  } catch (err) {
    pdfError.value = err.response?.data?.message || 'PDF 解析失败'
  }
}

async function reprocessNote() {
  dialog.warning({
    title: '重新 AI 处理',
    content: '确定重新 AI 处理此笔记？AI 标题和标签将被重新生成。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await knowledgeApi.reprocess(detailNote.value.id)
        const res = await knowledgeApi.getById(detailNote.value.id)
        Object.assign(detailNote.value, res.data.data)
        fetchData()
      } catch (err) {
        console.error('重新处理失败:', err)
      }
    }
  })
}

async function deleteNote(id) {
  dialog.warning({
    title: '删除笔记',
    content: '确定删除此笔记？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await store.remove(id)
      if (detailNote.value?.id === id) closeDetail()
      fetchData()
    }
  })
}

async function batchDelete() {
  dialog.warning({
    title: '批量删除',
    content: `确定删除选中的 ${selectedIds.value.length} 条知识？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await knowledgeApi.batchDelete(selectedIds.value)
      clearSelection()
      fetchData()
    }
  })
}

async function doBatchTag() {
  const tag = batchTagInput.value.trim()
  if (!tag) return
  await knowledgeApi.batchTag(selectedIds.value, tag)
  showBatchTag.value = false
  batchTagInput.value = ''
  clearSelection()
  fetchData()
}

async function batchAiTag() {
  aiTagging.value = true
  try {
    const res = await knowledgeApi.batchAiTag(selectedIds.value)
    const data = res.data?.data || {}
    message.success(`AI 打标完成: ${data.success}/${data.total}`)
    clearSelection()
    fetchData()
  } catch (err) {
    message.error('AI 打标失败: ' + (err.response?.data?.message || err.message))
  } finally {
    aiTagging.value = false
  }
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

function parseTags(tags) {
  if (!tags) return []
  try { return JSON.parse(tags) } catch { return [] }
}

const mergedDetailTags = computed(() => {
  if (!detailNote.value) return []
  const ai = parseTags(detailNote.value.tags)
  const user = parseTags(detailNote.value.userTags)
  return [...new Set([...ai, ...user])]
})

function gridTags(note) {
  const ai = parseTags(note.tags)
  const user = parseTags(note.userTags)
  return [...new Set([...ai, ...user])]
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

async function loadTags() {
  try {
    const res = await knowledgeApi.getTags()
    tagOptions.value = (res.data.data || []).map(t => ({
      label: `${t.name} (${t.count})`,
      value: t.name
    }))
  } catch {}
}

function initFromUrl() {
  const q = route.query
  if (q.keyword) keywordInput.value = q.keyword
  if (q.tags) selectedTags.value = q.tags.split(',')
  if (q.page) currentPage.value = parseInt(q.page) || 1
  if (q.size) pageSize.value = parseInt(q.size) || 20
  if (q.sortBy) sortBy.value = q.sortBy
}

watch(keywordInput, () => {
  triggerSearch()
})

onMounted(() => {
  initFromUrl()
  loadTags()
  fetchData()
})
</script>

<style scoped>
.knowledge-header-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; flex-shrink: 0;
  background: var(--gradient-brand);
}

.opaque-modal :deep(.n-card) {
  background-color: var(--color-bg) !important;
}
</style>
