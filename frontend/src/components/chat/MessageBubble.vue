<template>
  <div class="flex" :class="alignClass">
    <div class="max-w-[75%]">
      <!-- System/blocked message -->
      <div v-if="blocked"
        class="rounded-xl px-4 py-2.5"
        :style="{ backgroundColor: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' }"
      >
        <div class="flex items-start gap-2">
          <svg class="w-4 h-4 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z"/>
          </svg>
          <span class="text-sm whitespace-pre-wrap leading-relaxed">{{ message }}</span>
        </div>
      </div>

      <!-- User or assistant message -->
      <div v-else
        class="rounded-xl px-4 py-2.5"
        :class="isUser ? 'rounded-br-sm' : 'rounded-bl-sm'"
        :style="bubbleStyle"
      >
        <p class="text-sm whitespace-pre-wrap leading-relaxed">{{ message }}</p>
        <p class="text-xs mt-1" :style="{ color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--color-text-secondary)' }">
          {{ time }}
        </p>
      </div>

      <!-- Knowledge source links -->
      <div v-if="sourcesList.length" class="mt-2 flex flex-wrap gap-1.5 px-1">
        <n-a v-for="s in sourcesList" :key="s.id" :href="s.url || '#'" target="_blank"
          :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)', padding: '2px 8px', borderRadius: '4px', fontSize: '12px', textDecoration: 'none' }">
          <template #prefix>
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </template>
          {{ s.title || `知识 #${s.id}` }}
        </n-a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  message: String,
  isUser: Boolean,
  blocked: Boolean,
  time: String,
  sources: { type: String, default: '[]' }
})

const alignClass = computed(() => props.blocked ? 'justify-center' : (props.isUser ? 'justify-end' : 'justify-start'))

const bubbleStyle = computed(() => {
  if (props.isUser) {
    return { backgroundColor: 'var(--color-accent)', color: 'white' }
  }
  return { backgroundColor: '#eeebe7', color: 'var(--color-text)' }
})

const sourcesList = computed(() => {
  try { return JSON.parse(props.sources) }
  catch { return [] }
})

</script>