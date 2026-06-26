import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme } from 'naive-ui'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  const naiveTheme = computed(() => isDark.value ? darkTheme : null)

  function init() {
    const saved = localStorage.getItem('mindvault-dark')
    isDark.value = saved === 'true'
    applyTheme()
  }

  function toggleDark() {
    isDark.value = !isDark.value
    localStorage.setItem('mindvault-dark', String(isDark.value))
    applyTheme()
  }

  function applyTheme() {
    const html = document.documentElement
    html.classList.toggle('dark', isDark.value)
    html.classList.toggle('light', !isDark.value)
  }

  return { isDark, naiveTheme, init, toggleDark }
})