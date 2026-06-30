<template>
  <n-modal
    :show="visible"
    :title="isEdit ? '编辑笔记' : '新建笔记'"
    preset="card"
    :style="{ maxWidth: '720px', width: '100%' }"
    :mask-closable="!submitting"
    @update:show="$emit('update:visible', $event)"
  >
    <div class="editor">
      <n-input
        v-model:value="title"
        placeholder="标题（可选）"
        size="large"
        class="mb-3"
      />

      <div class="editor-body" :class="{ split: showPreview }">
        <textarea
          ref="contentRef"
          v-model="content"
          class="editor-textarea"
          placeholder="写下你的想法...（支持 Markdown）"
        />
        <div v-if="showPreview" class="preview-pane">
          <div class="preview-content" v-html="renderedPreview"></div>
        </div>
      </div>

      <div class="editor-tags mt-3">
        <n-dynamic-tags v-model:value="tags" placeholder="添加标签" />
      </div>

      <div class="editor-footer">
        <div class="flex items-center gap-2">
          <button
            class="preview-toggle"
            :class="{ active: showPreview }"
            @click="showPreview = !showPreview"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <span>{{ showPreview ? '编辑' : '预览' }}</span>
          </button>
        </div>
        <div class="footer-actions">
          <span class="char-count">{{ content.length }} 字</span>
          <n-button size="small" @click="$emit('update:visible', false)" :disabled="submitting">取消</n-button>
          <n-button size="small" type="primary" :loading="submitting" @click="submit">
            {{ isEdit ? '保存' : '发布' }}
          </n-button>
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { knowledgeApi } from '@/api/knowledge'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

const message = useMessage()
const contentRef = ref(null)

const props = defineProps({
  visible: Boolean,
  note: Object,
})
const emit = defineEmits(['update:visible', 'saved'])

const isEdit = computed(() => !!props.note)
const isMobile = ref(window.innerWidth < 768)

const title = ref('')
const content = ref('')
const tags = ref([])
const submitting = ref(false)
const showPreview = ref(false)

watch(() => props.visible, (val) => {
  if (!val) return
  if (props.note) {
    title.value = props.note.title || ''
    content.value = props.note.content || ''
    tags.value = parseTags(props.note.userTags)
  } else {
    title.value = ''
    content.value = ''
    tags.value = []
    showPreview.value = false
  }
  nextTick(() => {
    contentRef.value?.focus()
  })
})

const renderedPreview = computed(() => {
  if (!content.value) return '<p style="color:var(--color-text-secondary)">预览区域</p>'
  const html = marked.parse(content.value, { breaks: true, gfm: true })
  return DOMPurify.sanitize(html)
})

function parseTags(tagsStr) {
  if (!tagsStr) return []
  try { return JSON.parse(tagsStr) } catch { return [] }
}

async function submit() {
  if (submitting.value) return
  if (!content.value.trim()) {
    message.warning('请输入内容')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await knowledgeApi.update(props.note.id, {
        title: title.value,
        content: content.value,
        userTags: JSON.stringify(tags.value),
      })
    } else {
      await knowledgeApi.add({
        title: title.value,
        content: content.value,
        userTags: JSON.stringify(tags.value),
      })
    }
    emit('saved')
    emit('update:visible', false)
    message.success(isEdit.value ? '已保存' : '发布成功')
  } catch (err) {
    message.error('操作失败: ' + (err.response?.data?.message || err.message))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.editor {
  display: flex;
  flex-direction: column;
}
.editor-body {
  position: relative;
}
.editor-body.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  min-height: 250px;
}
.editor-textarea {
  width: 100%;
  min-height: 180px;
  max-height: 420px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  font-size: 14px;
  line-height: 1.7;
  font-family: inherit;
  padding: 10px 12px;
  resize: vertical;
  outline: none;
  transition: border-color 0.15s ease;
}
.editor-textarea:focus {
  border-color: var(--color-primary);
}
.editor-textarea::placeholder {
  color: var(--color-placeholder);
}
.preview-pane {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 10px 12px;
  max-height: 420px;
  overflow-y: auto;
  background: var(--color-bg);
}
.preview-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text);
}
.editor-tags {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
}
.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
}
.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.char-count {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.preview-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  cursor: pointer;
  transition: all 0.15s ease;
}
.preview-toggle:hover {
  color: var(--color-text);
  border-color: var(--color-text-secondary);
}
.preview-toggle.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}
</style>