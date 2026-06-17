<template>
  <div class="flex" :class="isUser ? 'justify-end' : 'justify-start'">
    <div
      class="max-w-[75%] rounded-xl px-4 py-2.5"
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
  </div>
</template>

<script setup>
defineProps({
  message: String,
  isUser: Boolean,
  time: String
})
</script>