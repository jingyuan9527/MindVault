import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme } from 'naive-ui'

export const THEMES = [
  { id: 'amber-earth', label: '琥珀大地', icon: '🌄' },
  { id: 'lavender-calm', label: '薰衣草静', icon: '🌿' },
  { id: 'violet-glass', label: '紫晶玻璃', icon: '💎' },
]

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(true)
  const currentTheme = ref('amber-earth')

  const naiveTheme = computed(() => isDark.value ? darkTheme : null)

  function init() {
    const savedTheme = localStorage.getItem('mindvault-theme')
    if (savedTheme && THEMES.some(t => t.id === savedTheme)) {
      currentTheme.value = savedTheme
    } else {
      currentTheme.value = 'amber-earth'
    }
    const savedDark = localStorage.getItem('mindvault-dark')
    if (savedDark !== null) {
      isDark.value = savedDark === 'true'
    } else {
      isDark.value = true
    }
    applyTheme()
  }

  function setTheme(id) {
    if (!THEMES.some(t => t.id === id)) return
    currentTheme.value = id
    localStorage.setItem('mindvault-theme', id)
    applyTheme()
  }

  function toggleDark() {
    isDark.value = !isDark.value
    localStorage.setItem('mindvault-dark', isDark.value)
    applyTheme()
  }

  function applyTheme() {
    const html = document.documentElement
    html.setAttribute('data-theme', currentTheme.value)
    html.classList.toggle('light', !isDark.value)
  }

  return { isDark, currentTheme, naiveTheme, init, setTheme, toggleDark, applyTheme }
})