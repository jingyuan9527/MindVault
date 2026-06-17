<template>
  <div class="flex justify-start">
    <div class="rounded-xl rounded-bl-sm px-4 py-3"
      :style="{ backgroundColor: '#eeebe7' }">
      <div class="flex items-center gap-2">
        <div class="flex gap-1">
          <span class="w-2 h-2 rounded-full animate-bounce" style="animation-delay: 0ms; background-color: var(--color-sage)"></span>
          <span class="w-2 h-2 rounded-full animate-bounce" style="animation-delay: 150ms; background-color: var(--color-sage)"></span>
          <span class="w-2 h-2 rounded-full animate-bounce" style="animation-delay: 300ms; background-color: var(--color-sage)"></span>
        </div>
        <span class="text-sm" style="color: var(--color-text-secondary)">正在检索知识库...</span>
      </div>
    </div>
  </div>
</template>