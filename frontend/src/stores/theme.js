import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function init() {
    const saved = localStorage.getItem('mindvault-dark')
    isDark.value = saved === 'true'
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('mindvault-dark', isDark.value)
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  return { isDark, init, toggle }
})
