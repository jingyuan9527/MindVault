<template>
  <div class="flex" :class="isUser ? 'justify-end' : 'justify-start'">
    <div class="max-w-[75%]">
      <div
        class="rounded-xl px-4 py-2.5"
        :class="isUser ? 'rounded-br-sm' : 'rounded-bl-sm'"
        :style="isUser
          ? { backgroundColor: 'var(--color-accent)', color: 'white' }
          : { backgroundColor: '#eeebe7', color: 'var(--color-text)' }"
      >
        <p class="text-sm whitespace-pre-wrap leading-relaxed">{{ message }}</p>
        <p class="text-xs mt-1" :style="{ color: isUser ? 'rgba(255,255,255,0.6)' : 'var(--color-text-secondary)' }">
          {{ time }}
        </p>
      </div>

      <div v-if="sourcesList.length" class="mt-2 flex flex-wrap gap-1.5 px-1">
        <a v-for="s in sourcesList" :key="s.id" :href="s.url || '#'" target="_blank"
          class="inline-flex items-center gap-1 px-2 py-1 rounded text-xs transition-all duration-150 hover:opacity-80"
          :style="{ backgroundColor: 'var(--color-sage-light)', color: 'var(--color-sage)' }">
          <svg class="w-3 h-3 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          {{ s.title || `知识 #${s.id}` }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  message: String,
  isUser: Boolean,
  time: String,
  sources: { type: String, default: '[]' }
})

const sourcesList = computed(() => {
  try {
    return JSON.parse(props.sources)
  } catch { return [] }
})
</script>