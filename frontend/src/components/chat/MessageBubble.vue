<template>
  <div class="flex" :class="isUser ? 'justify-end' : 'justify-start'">
    <div
      class="max-w-[75%] rounded-xl px-4 py-2.5"
      :class="isUser
        ? 'bg-blue-600 text-white rounded-br-sm'
        : 'bg-gray-100 text-gray-800 rounded-bl-sm'"
    >
      <p class="text-sm whitespace-pre-wrap">{{ message }}</p>
      <p class="text-xs mt-1" :class="isUser ? 'text-blue-200' : 'text-gray-400'">
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