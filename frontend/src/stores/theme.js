import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme } from 'naive-ui'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  const naiveTheme = computed(() => isDark.value ? darkTheme : null)

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

  return { isDark, naiveTheme, init, toggle }
})
